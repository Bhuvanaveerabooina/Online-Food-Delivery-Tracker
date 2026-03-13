package com.fooddelivery.app;

import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.Role;
import com.fooddelivery.model.User;
import com.fooddelivery.repo.MenuItemRepository;
import com.fooddelivery.repo.OrderRepository;
import com.fooddelivery.repo.RestaurantRepository;
import com.fooddelivery.repo.UserRepository;
import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.OrderService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class OnlineFoodDeliveryTrackerApplication {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
    private static final Path STORE = Path.of("data", "app-store.txt");

    private final RestaurantRepository restaurantRepository = new RestaurantRepository();
    private final MenuItemRepository menuItemRepository = new MenuItemRepository();
    private final UserRepository userRepository = new UserRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    private final AuthService authService = new AuthService(userRepository);
    private final OrderService orderService = new OrderService(orderRepository);

    private final Map<String, User> sessions = new HashMap<>();

    public static void main(String[] args) throws IOException {
        new OnlineFoodDeliveryTrackerApplication().start();
    }

    private void start() throws IOException {
        seedCoreData();
        loadPersistentData();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", this::handleHome);
        server.createContext("/login", this::handleLogin);
        server.createContext("/register", this::handleRegister);
        server.createContext("/app", this::handleApp);
        server.createContext("/place-order", this::handlePlaceOrder);
        server.createContext("/owner/update", this::handleOwnerStatusUpdate);
        server.createContext("/delivery/deliver", this::handleMarkDelivered);
        server.createContext("/logout", this::handleLogout);
        server.start();

        System.out.println("Online Food Delivery Tracker running at http://localhost:8080");
        keepServerAlive();
    }

    private void keepServerAlive() {
        try {
            Thread.currentThread().join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleHome(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }

        User user = getAuthenticatedUser(exchange);
        if (user != null) {
            redirect(exchange, "/app");
            return;
        }
        sendHtml(exchange, buildLoginPage(null), 200);
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }

        Map<String, String> params = parseForm(readBody(exchange));
        String username = params.getOrDefault("username", "").trim();
        String password = params.getOrDefault("password", "").trim();
        Role role = parseRole(params.getOrDefault("role", "").trim());
        var authenticated = role == null ? java.util.Optional.<User>empty() : authService.authenticate(username, password, role);

        if (authenticated.isEmpty()) {
            sendHtml(exchange, buildLoginPage("Invalid username, password, or role."), 401);
            return;
        }

        User user = authenticated.get();
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, user);
        exchange.getResponseHeaders().add("Set-Cookie", "SESSION_ID=" + sessionId + "; Path=/; HttpOnly");
        redirect(exchange, "/app");
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }

        Map<String, String> params = parseForm(readBody(exchange));
        String username = params.getOrDefault("username", "").trim();
        String password = params.getOrDefault("password", "").trim();

        if (username.isBlank() || password.isBlank()) {
            sendHtml(exchange, buildLoginPage("Username and password are required for registration."), 400);
            return;
        }
        if (userRepository.findByUsername(username).isPresent()) {
            sendHtml(exchange, buildLoginPage("Username already exists."), 409);
            return;
        }

        userRepository.save(username, password, Role.CUSTOMER, null);
        persistData();
        sendHtml(exchange, buildLoginPage("Registration successful. Log in with role CUSTOMER."), 200);
    }

    private void handleApp(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }

        User user = getAuthenticatedUser(exchange);
        if (user == null) {
            redirect(exchange, "/");
            return;
        }
        Map<String, String> query = parseForm(exchange.getRequestURI().getRawQuery() == null ? "" : exchange.getRequestURI().getRawQuery());
        sendHtml(exchange, buildDashboardPage(user, query.get("msg"), query.get("status")), 200);
    }

    private void handlePlaceOrder(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        User user = getAuthenticatedUser(exchange);
        if (user == null || user.getRole() != Role.CUSTOMER) {
            redirect(exchange, "/");
            return;
        }

        Map<String, String> params = parseForm(readBody(exchange));
        String menuItemIdText = params.getOrDefault("menuItemId", "");
        String quantityText = params.getOrDefault("quantity", "1");
        String address = params.getOrDefault("address", "");

        try {
            long menuItemId = Long.parseLong(menuItemIdText);
            int quantity = Integer.parseInt(quantityText);
            MenuItem item = menuItemRepository.findAll().stream().filter(m -> m.getId().equals(menuItemId)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid menu item"));
            orderService.placeOrder(user, item.getRestaurant(), item, quantity, address);
            persistData();
            redirect(exchange, "/app?msg=" + encode("Order placed successfully"));
        } catch (RuntimeException ex) {
            sendHtml(exchange, buildDashboardPage(user, "Failed to place order: " + ex.getMessage(), null), 400);
        }
    }

    private void handleOwnerStatusUpdate(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        User user = getAuthenticatedUser(exchange);
        if (user == null || user.getRole() != Role.RESTAURANT_OWNER || user.getRestaurant() == null) {
            redirect(exchange, "/");
            return;
        }

        Map<String, String> params = parseForm(readBody(exchange));
        try {
            Order order = orderService.byOrderId(params.getOrDefault("orderId", ""));
            if (!order.getRestaurant().getId().equals(user.getRestaurant().getId())) {
                throw new IllegalArgumentException("Order does not belong to your restaurant");
            }
            OrderStatus status = OrderStatus.valueOf(params.getOrDefault("status", ""));
            orderService.updateStatus(order, status);
            persistData();
            redirect(exchange, "/app?msg=" + encode("Order status updated"));
        } catch (RuntimeException ex) {
            sendHtml(exchange, buildDashboardPage(user, "Status update failed: " + ex.getMessage(), null), 400);
        }
    }

    private void handleMarkDelivered(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        User user = getAuthenticatedUser(exchange);
        if (user == null || user.getRole() != Role.DELIVERY_PERSON) {
            redirect(exchange, "/");
            return;
        }

        Map<String, String> params = parseForm(readBody(exchange));
        try {
            Order order = orderService.byOrderId(params.getOrDefault("orderId", ""));
            orderService.updateStatus(order, OrderStatus.DELIVERED);
            persistData();
            redirect(exchange, "/app?msg=" + encode("Order marked as delivered"));
        } catch (RuntimeException ex) {
            sendHtml(exchange, buildDashboardPage(user, "Failed to mark delivered: " + ex.getMessage(), null), 400);
        }
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }

        String sessionId = extractSessionId(exchange);
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
        exchange.getResponseHeaders().add("Set-Cookie", "SESSION_ID=deleted; Path=/; Max-Age=0");
        redirect(exchange, "/");
    }

    private User getAuthenticatedUser(HttpExchange exchange) {
        String sessionId = extractSessionId(exchange);
        return sessionId == null ? null : sessions.get(sessionId);
    }

    private String extractSessionId(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }

        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String[] kv = cookie.trim().split("=", 2);
            if (kv.length == 2 && "SESSION_ID".equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private Role parseRole(String role) {
        try {
            return Role.valueOf(role);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Map<String, String> parseForm(String formData) {
        Map<String, String> result = new HashMap<>();
        if (formData == null || formData.isBlank()) {
            return result;
        }

        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            String key = decode(keyValue[0]);
            String value = keyValue.length > 1 ? decode(keyValue[1]) : "";
            result.put(key, value);
        }
        return result;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private String decode(String input) {
        return URLDecoder.decode(input, StandardCharsets.UTF_8);
    }

    private String encode(String input) {
        return URLEncoder.encode(input, StandardCharsets.UTF_8);
    }

    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(405, -1);
        exchange.close();
    }

    private void sendHtml(HttpExchange exchange, String html, int statusCode) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String buildLoginPage(String message) {
        String statusBlock = "";
        if (message != null) {
            statusBlock = "<p class='status'>" + escapeHtml(message) + "</p>";
        }

        String userOptions = "<option value=''>Type manually or choose demo user</option>" + userRepository.findAll().stream()
                .map(user -> "<option value='" + escapeHtml(user.getUsername()) + "'>" + escapeHtml(user.getUsername()) + " (" + user.getRole() + ")</option>")
                .reduce("", String::concat);

        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset='UTF-8'>
                  <title>Online Food Delivery Tracker</title>
                  <style>
                    body { margin: 0; font-family: Arial, sans-serif; background:#f3f4f6; }
                    .card { width:min(560px, 92vw); margin: 40px auto; background:#fff; border:1px solid #d1d5db; padding:24px; }
                    h1,h2 { margin:0 0 10px; }
                    label { display:block; margin-top:10px; font-weight:700; }
                    input, select { width:100%; padding:10px; margin-top:6px; border:1px solid #9ca3af; }
                    .status { margin: 0 0 12px; padding: 10px; font-weight: 700; background:#eef2ff; }
                    .btn { margin-top:14px; width:100%; padding:11px; background:#111827; color:#fff; border:0; cursor:pointer; }
                    .hint { margin-top: 12px; color:#374151; font-size:13px; }
                    .split { display:grid; grid-template-columns: 1fr 1fr; gap:18px; }
                  </style>
                </head>
                <body>
                  <div class='card'>
                    <h1>Food Delivery Tracker (localhost web app)</h1>
                    __STATUS__
                    <div class='split'>
                      <form method='post' action='/login'>
                        <h2>Login</h2>
                        <label>Role</label>
                        <select name='role' required>
                          <option value='CUSTOMER'>CUSTOMER</option>
                          <option value='RESTAURANT_OWNER'>RESTAURANT_OWNER</option>
                          <option value='DELIVERY_PERSON'>DELIVERY_PERSON</option>
                        </select>
                        <label>Username</label>
                        <input list='existingUsers' name='username' required />
                        <datalist id='existingUsers'>__USER_OPTIONS__</datalist>
                        <label>Password</label>
                        <input type='password' name='password' required />
                        <button class='btn' type='submit'>Login</button>
                      </form>

                      <form method='post' action='/register'>
                        <h2>Register New CUSTOMER</h2>
                        <label>Username</label>
                        <input name='username' required />
                        <label>Password</label>
                        <input type='password' name='password' required />
                        <button class='btn' type='submit'>Register</button>
                      </form>
                    </div>
                    <div class='hint'>Demo users: customer1/pass, owner_spice/pass, owner_pizza/pass, delivery1/pass</div>
                  </div>
                </body>
                </html>
                """
                .replace("__STATUS__", statusBlock)
                .replace("__USER_OPTIONS__", userOptions);
    }

    private String buildDashboardPage(User user, String fallbackMessage, String statusFilter) {
        String message = fallbackMessage;

        String top = """
                <div class='top'>
                  <h1>Welcome, __USERNAME__</h1>
                  <div><a class='btn' href='/logout'>Logout</a></div>
                </div>
                """.replace("__USERNAME__", escapeHtml(user.getUsername()));

        String body;
        if (user.getRole() == Role.CUSTOMER) {
            body = buildCustomerDashboard(user);
        } else if (user.getRole() == Role.RESTAURANT_OWNER) {
            body = buildOwnerDashboard(user, statusFilter);
        } else {
            body = buildDeliveryDashboard();
        }

        String statusBlock = "";
        if (message != null) {
            statusBlock = "<div class='status'>" + escapeHtml(message) + "</div>";
        }

        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset='UTF-8'>
                  <title>Dashboard - Food Delivery Tracker</title>
                  <style>
                    body { margin:0; font-family:Arial, sans-serif; background:#f3f4f6; color:#111827; }
                    .top { display:flex; justify-content:space-between; align-items:center; padding:20px 26px; background:#fff; border-bottom:1px solid #d1d5db; }
                    .btn { padding:8px 14px; border:1px solid #111827; color:#111827; text-decoration:none; background:#fff; }
                    .wrap { padding:24px; }
                    .panel { background:#fff; border:1px solid #d1d5db; padding:18px; margin-bottom:16px; }
                    table { width:100%; border-collapse:collapse; }
                    th, td { border:1px solid #d1d5db; padding:10px; text-align:left; font-size:14px; }
                    input, select { padding:8px; border:1px solid #9ca3af; }
                    .status { background:#dbeafe; padding:10px; font-weight:700; margin-bottom:12px; }
                    .inline { display:flex; gap:8px; align-items:center; }
                  </style>
                </head>
                <body>
                  __TOP__
                  <div class='wrap'>
                    __STATUS__
                    <div class='panel'><strong>Role:</strong> __ROLE__</div>
                    __BODY__
                  </div>
                </body>
                </html>
                """
                .replace("__TOP__", top)
                .replace("__STATUS__", statusBlock)
                .replace("__ROLE__", user.getRole().name())
                .replace("__BODY__", body);
    }

    private String buildCustomerDashboard(User customer) {
        String menuOptions = menuItemRepository.findAll().stream()
                .map(item -> "<option value='" + item.getId() + "'>" + escapeHtml(item.getRestaurant().getName()) + " - " + escapeHtml(item.getName()) + " (₹" + item.getPrice() + ")</option>")
                .collect(Collectors.joining());

        String rows = orderRepository.findByCustomerOrderByCreatedAtDesc(customer).stream()
                .map(this::orderRow)
                .collect(Collectors.joining());
        if (rows.isEmpty()) {
            rows = "<tr><td colspan='7'>No orders yet.</td></tr>";
        }

        return """
                <div class='panel'>
                  <h2>Place Order</h2>
                  <form method='post' action='/place-order' class='inline'>
                    <select name='menuItemId' required>__ITEMS__</select>
                    <input type='number' min='1' value='1' name='quantity' required />
                    <input name='address' placeholder='Delivery address' required />
                    <button type='submit'>Place Order</button>
                  </form>
                </div>
                <div class='panel'>
                  <h2>Order History</h2>
                  <table>
                    <tr><th>ID</th><th>Restaurant</th><th>Item</th><th>Qty</th><th>Status</th><th>Address</th><th>Time</th></tr>
                    __ROWS__
                  </table>
                </div>
                """
                .replace("__ITEMS__", menuOptions)
                .replace("__ROWS__", rows);
    }

    private String buildOwnerDashboard(User owner, String statusFilter) {
        List<Order> orders = new ArrayList<>(orderRepository.findByRestaurantOrderByCreatedAtDesc(owner.getRestaurant()));
        List<OrderStatus> filterStatuses = List.of(OrderStatus.PLACED, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED);

        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                OrderStatus selected = OrderStatus.valueOf(statusFilter);
                orders = orders.stream().filter(order -> order.getStatus() == selected).collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {
                statusFilter = "";
            }
        }

        final String currentFilter = statusFilter == null ? "" : statusFilter;
        String filterOptions = "<option value=''>ALL</option>" + filterStatuses.stream()
                .map(status -> "<option value='" + status + "'" + (status.name().equals(currentFilter) ? " selected" : "") + ">" + status + "</option>")
                .collect(Collectors.joining());

        String rows = orders.stream().map(order -> {
            String statusOptions = filterStatuses.stream()
                    .map(status -> "<option value='" + status + "'" + (status == order.getStatus() ? " selected" : "") + ">" + status + "</option>")
                    .collect(Collectors.joining());
            return "<tr><td>" + order.getOrderId() + "</td><td>" + escapeHtml(order.getCustomer().getUsername()) + "</td><td>" + escapeHtml(order.getMenuItem().getName())
                    + "</td><td>" + order.getQuantity() + "</td><td>" + order.getStatus() + "</td><td>"
                    + "<form method='post' action='/owner/update' class='inline'><input type='hidden' name='orderId' value='" + order.getOrderId() + "'/>"
                    + "<select name='status'>" + statusOptions + "</select><button type='submit'>Update</button></form></td></tr>";
        }).collect(Collectors.joining());

        if (rows.isEmpty()) {
            rows = "<tr><td colspan='6'>No orders for your restaurant.</td></tr>";
        }

        return """
                <div class='panel'>
                  <h2>Owner Dashboard - __RESTAURANT__</h2>
                  <form method='get' action='/app' class='inline'>
                    <label for='status'>Filter status:</label>
                    <select id='status' name='status'>__FILTER_OPTIONS__</select>
                    <button type='submit'>Apply Filter</button>
                  </form>
                  <table>
                    <tr><th>ID</th><th>Customer</th><th>Item</th><th>Qty</th><th>Current Status</th><th>Change Status</th></tr>
                    __ROWS__
                  </table>
                </div>
                """
                .replace("__RESTAURANT__", escapeHtml(owner.getRestaurant().getName()))
                .replace("__ROWS__", rows)
                .replace("__FILTER_OPTIONS__", filterOptions);
    }

    private String buildDeliveryDashboard() {
        List<Order> orders = orderRepository.findByStatusInOrderByCreatedAtDesc(List.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY));
        String rows = orders.stream().map(order -> "<tr><td>" + order.getOrderId() + "</td><td>" + escapeHtml(order.getCustomer().getUsername()) + "</td><td>"
                + escapeHtml(order.getRestaurant().getName()) + "</td><td>" + order.getStatus() + "</td><td>"
                + "<form method='post' action='/delivery/deliver'><input type='hidden' name='orderId' value='" + order.getOrderId() + "'/><button type='submit'>Mark Delivered</button></form></td></tr>")
                .collect(Collectors.joining());
        if (rows.isEmpty()) {
            rows = "<tr><td colspan='5'>No active delivery orders.</td></tr>";
        }

        return """
                <div class='panel'>
                  <h2>Delivery Dashboard</h2>
                  <table>
                    <tr><th>ID</th><th>Customer</th><th>Restaurant</th><th>Status</th><th>Action</th></tr>
                    __ROWS__
                  </table>
                </div>
                """.replace("__ROWS__", rows);
    }

    private String orderRow(Order order) {
        return "<tr><td>" + order.getOrderId() + "</td><td>" + escapeHtml(order.getRestaurant().getName()) + "</td><td>" + escapeHtml(order.getMenuItem().getName())
                + "</td><td>" + order.getQuantity() + "</td><td>" + order.getStatus() + "</td><td>" + escapeHtml(order.getDeliveryAddress()) + "</td><td>" + TIME_FORMAT.format(order.getCreatedAt()) + "</td></tr>";
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void seedCoreData() {
        Restaurant spiceHub = restaurantRepository.save("Spice Hub");
        Restaurant pizzaPoint = restaurantRepository.save("Pizza Point");

        MenuItem paneerBowl = menuItemRepository.save("Paneer Bowl", 180.0, spiceHub);
        MenuItem vegBiryani = menuItemRepository.save("Veg Biryani", 220.0, spiceHub);
        MenuItem margheritaPizza = menuItemRepository.save("Margherita Pizza", 250.0, pizzaPoint);

        if (userRepository.count() == 0) {
            User customer = userRepository.save("customer1", "pass", Role.CUSTOMER, null);
            userRepository.save("owner_spice", "pass", Role.RESTAURANT_OWNER, spiceHub);
            userRepository.save("owner_pizza", "pass", Role.RESTAURANT_OWNER, pizzaPoint);
            userRepository.save("delivery1", "pass", Role.DELIVERY_PERSON, null);

            Order first = orderService.placeOrder(customer, spiceHub, paneerBowl, 2, "MG Road");
            Order second = orderService.placeOrder(customer, spiceHub, vegBiryani, 1, "MG Road");
            Order third = orderService.placeOrder(customer, pizzaPoint, margheritaPizza, 3, "Whitefield");
            orderService.updateStatus(first, OrderStatus.DELIVERED);
            orderService.updateStatus(second, OrderStatus.OUT_FOR_DELIVERY);
            orderService.updateStatus(third, OrderStatus.PREPARING);
        }
    }

    private void loadPersistentData() throws IOException {
        if (!Files.exists(STORE)) {
            persistData();
            return;
        }
        List<String> lines = Files.readAllLines(STORE, StandardCharsets.UTF_8);
        boolean loadedUsers = false;
        boolean loadedOrders = false;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\t");
            if (parts[0].equals("USER") && parts.length >= 6) {
                String username = decode(parts[2]);
                if (userRepository.findByUsername(username).isPresent()) {
                    continue;
                }
                Restaurant restaurant = "-".equals(parts[5]) ? null : restaurantRepository.findByName(decode(parts[5])).orElse(null);
                userRepository.saveExisting(Long.parseLong(parts[1]), username, decode(parts[3]), Role.valueOf(parts[4]), restaurant);
                loadedUsers = true;
            }
            if (parts[0].equals("ORDER") && parts.length >= 11) {
                User customer = userRepository.findByUsername(decode(parts[2])).orElse(null);
                Restaurant restaurant = restaurantRepository.findByName(decode(parts[3])).orElse(null);
                if (customer == null || restaurant == null) {
                    continue;
                }
                MenuItem item = menuItemRepository.findByNameAndRestaurant(decode(parts[4]), restaurant).orElse(null);
                if (item == null) {
                    continue;
                }
                Order order = new Order(
                        Long.parseLong(parts[1]),
                        decode(parts[5]),
                        customer,
                        restaurant,
                        item,
                        Integer.parseInt(parts[6]),
                        Double.parseDouble(parts[7]),
                        decode(parts[8]),
                        OrderStatus.valueOf(parts[9]),
                        LocalDateTime.parse(parts[10])
                );
                orderRepository.saveExisting(order);
                loadedOrders = true;
            }
        }

        if (loadedUsers || loadedOrders) {
            System.out.println("Loaded persisted users/orders from " + STORE);
        }
    }

    private void persistData() throws IOException {
        Files.createDirectories(STORE.getParent());
        List<String> lines = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            lines.add(String.join("\t",
                    "USER",
                    String.valueOf(user.getId()),
                    encode(user.getUsername()),
                    encode(user.getPassword()),
                    user.getRole().name(),
                    user.getRestaurant() == null ? "-" : encode(user.getRestaurant().getName())
            ));
        }

        for (Order order : orderRepository.findAll()) {
            lines.add(String.join("\t",
                    "ORDER",
                    String.valueOf(order.getId()),
                    encode(order.getCustomer().getUsername()),
                    encode(order.getRestaurant().getName()),
                    encode(order.getMenuItem().getName()),
                    encode(order.getOrderId()),
                    String.valueOf(order.getQuantity()),
                    String.valueOf(order.getItemPrice()),
                    encode(order.getDeliveryAddress()),
                    order.getStatus().name(),
                    order.getCreatedAt().toString()
            ));
        }

        Files.write(STORE, lines, StandardCharsets.UTF_8);
    }
}
