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
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OnlineFoodDeliveryTrackerApplication {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

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
        seedData();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", this::handleHome);
        server.createContext("/login", this::handleLogin);
        server.createContext("/app", this::handleApp);
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

        String form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseForm(form);

        String username = params.getOrDefault("username", "").trim();
        String password = params.getOrDefault("password", "").trim();
        String roleText = params.getOrDefault("role", "").trim();

        Role role = parseRole(roleText);
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

        sendHtml(exchange, buildDashboardPage(user), 200);
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

    private String decode(String input) {
        return URLDecoder.decode(input, StandardCharsets.UTF_8);
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

    private String buildLoginPage(String errorMessage) {
        String statusBlock = "";
        if (errorMessage != null) {
            statusBlock = "<p class='status error'>" + errorMessage + "</p>";
        }

        String roleOptions = """
                <option value='CUSTOMER'>CUSTOMER</option>
                <option value='RESTAURANT_OWNER'>RESTAURANT_OWNER</option>
                <option value='DELIVERY_PERSON'>DELIVERY_PERSON</option>
                """;

        String userOptions = new StringBuilder()
                .append("<option value=''>Select existing user</option>")
                .append(userRepository.findAll().stream()
                        .map(user -> "<option value='" + user.getUsername() + "'>" + user.getUsername() + " (" + user.getRole() + ")</option>")
                        .reduce("", String::concat))
                .toString();

        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset='UTF-8'>
                  <title>Online Food Delivery Tracker</title>
                  <style>
                    body { margin: 0; font-family: Arial, sans-serif; background:#f3f4f6; }
                    .card { width:min(520px, 92vw); margin: 60px auto; background:#fff; border:1px solid #d1d5db; padding:24px; }
                    h1 { margin:0 0 18px; }
                    label { display:block; margin-top:12px; font-weight:700; }
                    input, select { width:100%; padding:10px; margin-top:6px; border:1px solid #9ca3af; }
                    .status { margin: 0 0 12px; padding: 10px; font-weight: 700; }
                    .error { background:#fee2e2; color:#991b1b; }
                    .btn { margin-top:16px; width:100%; padding:11px; background:#111827; color:#fff; border:0; cursor:pointer; }
                    .hint { margin-top: 12px; color:#374151; font-size:13px; }
                  </style>
                </head>
                <body>
                  <div class='card'>
                    <h1>Food Delivery Tracker Login</h1>
                    __STATUS__
                    <form method='post' action='/login'>
                      <label>Role</label>
                      <select name='role' required>
                        __ROLE_OPTIONS__
                      </select>

                      <label>User ID</label>
                      <select name='username' required>
                        __USER_OPTIONS__
                      </select>

                      <label>Password</label>
                      <input type='password' name='password' required />

                      <button class='btn' type='submit'>Login</button>
                    </form>
                    <div class='hint'>Login uses Java repository users and redirects to profile after successful authentication.</div>
                  </div>
                </body>
                </html>
                """
                .replace("__STATUS__", statusBlock)
                .replace("__ROLE_OPTIONS__", roleOptions)
                .replace("__USER_OPTIONS__", userOptions);
    }

    private String buildDashboardPage(User user) {
        List<Order> orders = new ArrayList<>(orderRepository.findByCustomerOrderByCreatedAtDesc(user));
        if (orders.isEmpty()) {
            orders = orderRepository.findByStatusInOrderByCreatedAtDesc(
                    List.of(OrderStatus.PLACED, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED)
            );
        }

        String rows = orders.stream()
                .map(order -> "<tr>"
                        + "<td>" + order.getOrderId() + "</td>"
                        + "<td>" + order.getCustomer().getUsername() + "</td>"
                        + "<td>" + order.getMenuItem().getName() + "</td>"
                        + "<td>" + order.getQuantity() + "</td>"
                        + "<td>" + order.getStatus() + "</td>"
                        + "<td>" + TIME_FORMAT.format(order.getCreatedAt()) + "</td>"
                        + "</tr>")
                .reduce("", String::concat);

        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset='UTF-8'>
                  <title>Profile - Food Delivery Tracker</title>
                  <style>
                    body { margin:0; font-family:Arial, sans-serif; background:#f3f4f6; color:#111827; }
                    .top { display:flex; justify-content:space-between; align-items:center; padding:20px 26px; background:#fff; border-bottom:1px solid #d1d5db; }
                    .btn { padding:8px 14px; border:1px solid #111827; color:#111827; text-decoration:none; }
                    .wrap { padding:24px; }
                    .panel { background:#fff; border:1px solid #d1d5db; padding:18px; margin-bottom:16px; }
                    table { width:100%; border-collapse:collapse; }
                    th, td { border:1px solid #d1d5db; padding:10px; text-align:left; }
                  </style>
                </head>
                <body>
                  <div class='top'>
                    <h1>Welcome, __USERNAME__</h1>
                    <a class='btn' href='/logout'>Logout</a>
                  </div>
                  <div class='wrap'>
                    <div class='panel'><strong>Role:</strong> __ROLE__</div>
                    <div class='panel'>
                      <h2>Order History</h2>
                      <table>
                        <tr><th>ID</th><th>Customer</th><th>Item</th><th>Qty</th><th>Status</th><th>Time</th></tr>
                        __ROWS__
                      </table>
                    </div>
                  </div>
                </body>
                </html>
                """
                .replace("__USERNAME__", user.getUsername())
                .replace("__ROLE__", user.getRole().name())
                .replace("__ROWS__", rows);
    }

    private void seedData() {
        if (restaurantRepository.count() > 0) {
            return;
        }

        Restaurant spiceHub = restaurantRepository.save("Spice Hub");
        Restaurant pizzaPoint = restaurantRepository.save("Pizza Point");

        MenuItem paneerBowl = menuItemRepository.save("Paneer Bowl", 180.0, spiceHub);
        MenuItem vegBiryani = menuItemRepository.save("Veg Biryani", 220.0, spiceHub);
        MenuItem margheritaPizza = menuItemRepository.save("Margherita Pizza", 250.0, pizzaPoint);

        User customer = userRepository.save("bhuvana", "pass", Role.CUSTOMER, null);
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
