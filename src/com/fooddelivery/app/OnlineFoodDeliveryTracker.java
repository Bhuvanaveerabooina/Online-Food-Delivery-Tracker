package com.fooddelivery.app;

import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.UserAccount;
import com.fooddelivery.model.UserRole;
import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.OrderService;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Starts a web application for role-based food delivery tracking.
 */
public class OnlineFoodDeliveryTracker {
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

    private final OrderService orderService = new OrderService();
    private final AuthService authService = new AuthService();
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        new OnlineFoodDeliveryTracker().start();
    }

    private void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", this::handleRoot);
        server.createContext("/health", this::handleHealth);
        server.createContext("/login", exchange -> serveHtml(exchange, loginPage()));
        server.createContext("/app", this::handleApp);

        server.createContext("/api/register", new JsonPostHandler(this::handleRegister));
        server.createContext("/api/login", new JsonPostHandler(this::handleLogin));
        server.createContext("/api/logout", this::handleLogout);
        server.createContext("/api/me", this::handleMe);

        server.createContext("/api/restaurants", this::handleRestaurants);
        server.createContext("/api/menu", this::handleMenuItems);

        server.createContext("/api/customer/place-order", new JsonPostHandler(this::handlePlaceOrder));
        server.createContext("/api/customer/orders", this::handleCustomerOrders);
        server.createContext("/api/customer/status", this::handleCustomerStatus);

        server.createContext("/api/owner/orders", this::handleOwnerOrders);
        server.createContext("/api/owner/accept-order", new JsonPostHandler(this::handleOwnerAcceptOrder));

        server.createContext("/api/delivery/orders", this::handleDeliveryOrders);
        server.createContext("/api/delivery/mark-delivered", new JsonPostHandler(this::handleMarkDelivered));

        server.setExecutor(null);
        server.start();
        System.out.println("Server started on http://localhost:" + PORT);
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        UserAccount user = getLoggedInUser(exchange);
        redirect(exchange, user == null ? "/login" : "/app");
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        writeJson(exchange, 200, "{\"status\":\"ok\"}");
    }

    private void handleApp(HttpExchange exchange) throws IOException {
        UserAccount user = getLoggedInUser(exchange);
        if (user == null) {
            redirect(exchange, "/login");
            return;
        }
        if (user.getRole() == UserRole.CUSTOMER) {
            serveHtml(exchange, customerDashboardPage(user));
            return;
        }
        if (user.getRole() == UserRole.RESTAURANT_OWNER) {
            serveHtml(exchange, ownerDashboardPage(user));
            return;
        }
        serveHtml(exchange, deliveryDashboardPage(user));
    }

    private void handleRegister(HttpExchange exchange, Map<String, String> payload) throws IOException {
        try {
            UserRole role = UserRole.fromText(payload.get("role"));
            Integer restaurantId = parseInteger(payload.get("restaurantId"));
            authService.register(payload.get("username"), payload.get("password"), role, restaurantId);
            writeJson(exchange, 200, jsonMessage("Account created successfully. Please login."));
        } catch (IllegalArgumentException exception) {
            writeJson(exchange, 400, jsonMessage(exception.getMessage()));
        }
    }

    private void handleLogin(HttpExchange exchange, Map<String, String> payload) throws IOException {
        String username = payload.getOrDefault("username", "").trim();
        UserRole role;
        try {
            role = UserRole.fromText(payload.get("role"));
        } catch (IllegalArgumentException e) {
            writeJson(exchange, 400, jsonMessage(e.getMessage()));
            return;
        }

        boolean authenticated = authService.authenticate(username, payload.get("password"), role);
        if (!authenticated) {
            writeJson(exchange, 401, jsonMessage("Invalid username, password, or role."));
            return;
        }

        String token = UUID.randomUUID().toString();
        sessions.put(token, username);
        exchange.getResponseHeaders().add("Set-Cookie", "SESSION=" + token + "; Path=/; HttpOnly");
        writeJson(exchange, 200, jsonMessage("Login successful."));
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = readCookie(exchange, "SESSION");
        if (token != null) {
            sessions.remove(token);
        }
        exchange.getResponseHeaders().add("Set-Cookie", "SESSION=deleted; Path=/; Max-Age=0");
        redirect(exchange, "/login");
    }

    private void handleMe(HttpExchange exchange) throws IOException {
        UserAccount user = getLoggedInUser(exchange);
        if (user == null) {
            writeJson(exchange, 401, jsonMessage("Please login first."));
            return;
        }
        writeJson(exchange, 200,
                "{\"username\":\"" + escape(user.getUsername()) + "\",\"role\":\"" + user.getRole() + "\"}");
    }

    private void handleRestaurants(HttpExchange exchange) throws IOException {
        List<Restaurant> restaurants = orderService.getRestaurants();
        StringBuilder json = new StringBuilder("{\"restaurants\":[");
        for (int i = 0; i < restaurants.size(); i++) {
            Restaurant restaurant = restaurants.get(i);
            json.append("{\"id\":").append(restaurant.getId())
                    .append(",\"restaurantName\":\"").append(escape(restaurant.getRestaurantName())).append("\"}");
            if (i < restaurants.size() - 1) {
                json.append(",");
            }
        }
        json.append("]}");
        writeJson(exchange, 200, json.toString());
    }

    private void handleMenuItems(HttpExchange exchange) throws IOException {
        Integer restaurantId = parseInteger(getQueryParam(exchange, "restaurantId"));
        if (restaurantId == null) {
            writeJson(exchange, 400, jsonMessage("restaurantId is required."));
            return;
        }

        List<MenuItem> menuItems = orderService.getMenuItemsByRestaurant(restaurantId);
        StringBuilder json = new StringBuilder("{\"items\":[");
        for (int i = 0; i < menuItems.size(); i++) {
            MenuItem item = menuItems.get(i);
            json.append("{\"id\":").append(item.getId())
                    .append(",\"itemName\":\"").append(escape(item.getItemName()))
                    .append("\",\"price\":").append(item.getPrice()).append("}");
            if (i < menuItems.size() - 1) {
                json.append(",");
            }
        }
        json.append("]}");
        writeJson(exchange, 200, json.toString());
    }

    private void handlePlaceOrder(HttpExchange exchange, Map<String, String> payload) throws IOException {
        UserAccount user = requireRole(exchange, UserRole.CUSTOMER);
        if (user == null) return;

        try {
            Order order = orderService.placeOrder(
                    user.getUsername(),
                    payload.getOrDefault("customerName", "").trim(),
                    Integer.parseInt(payload.getOrDefault("restaurantId", "0")),
                    Integer.parseInt(payload.getOrDefault("itemId", "0")),
                    Integer.parseInt(payload.getOrDefault("quantity", "0")),
                    payload.getOrDefault("deliveryAddress", "").trim()
            );
            writeJson(exchange, 200, "{\"message\":\"Order placed successfully.\",\"orderId\":\"" + order.getOrderId() + "\"}");
        } catch (Exception exception) {
            writeJson(exchange, 400, jsonMessage("Could not place order: " + exception.getMessage()));
        }
    }

    private void handleCustomerOrders(HttpExchange exchange) throws IOException {
        UserAccount user = requireRole(exchange, UserRole.CUSTOMER);
        if (user == null) return;
        writeJson(exchange, 200, ordersJson(orderService.getCustomerOrderHistory(user.getUsername())));
    }

    private void handleCustomerStatus(HttpExchange exchange) throws IOException {
        UserAccount user = requireRole(exchange, UserRole.CUSTOMER);
        if (user == null) return;

        String orderId = getQueryParam(exchange, "id");
        if (orderId == null || orderId.isBlank()) {
            writeJson(exchange, 400, jsonMessage("Order id is required."));
            return;
        }

        Optional<Order> order = orderService.findCustomerOrderById(user.getUsername(), orderId);
        if (order.isEmpty()) {
            writeJson(exchange, 404, jsonMessage("Order not found."));
            return;
        }

        writeJson(exchange, 200, "{\"orderId\":\"" + order.get().getOrderId() + "\",\"status\":\"" + order.get().getStatus() + "\"}");
    }

    private void handleOwnerOrders(HttpExchange exchange) throws IOException {
        UserAccount user = requireRole(exchange, UserRole.RESTAURANT_OWNER);
        if (user == null) return;

        String customer = getQueryParam(exchange, "customer");
        String statusRaw = getQueryParam(exchange, "status");
        OrderStatus status = null;
        if (statusRaw != null && !statusRaw.isBlank()) {
            try {
                status = OrderStatus.valueOf(statusRaw.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        writeJson(exchange, 200, ordersJson(orderService.getOrdersForRestaurant(user.getRestaurantId(), customer, status)));
    }

    private void handleOwnerAcceptOrder(HttpExchange exchange, Map<String, String> payload) throws IOException {
        UserAccount user = requireRole(exchange, UserRole.RESTAURANT_OWNER);
        if (user == null) return;

        String orderId = payload.getOrDefault("orderId", "").trim();
        if (orderId.isEmpty()) {
            writeJson(exchange, 400, jsonMessage("orderId is required."));
            return;
        }

        boolean updated = orderService.acceptOrderForRestaurant(user.getRestaurantId(), orderId);
        if (!updated) {
            writeJson(exchange, 400, jsonMessage("Only placed orders can be accepted."));
            return;
        }

        writeJson(exchange, 200, jsonMessage("Order accepted and moved to preparing."));
    }

    private void handleDeliveryOrders(HttpExchange exchange) throws IOException {
        UserAccount user = requireRole(exchange, UserRole.DELIVERY_PERSON);
        if (user == null) return;
        writeJson(exchange, 200, ordersJson(orderService.getOrdersForDeliveryPerson(user.getUsername())));
    }

    private void handleMarkDelivered(HttpExchange exchange, Map<String, String> payload) throws IOException {
        UserAccount user = requireRole(exchange, UserRole.DELIVERY_PERSON);
        if (user == null) return;

        String orderId = payload.getOrDefault("orderId", "").trim();
        if (orderId.isEmpty()) {
            writeJson(exchange, 400, jsonMessage("orderId is required."));
            return;
        }

        boolean updated = orderService.markOrderAsDelivered(user.getUsername(), orderId);
        if (!updated) {
            writeJson(exchange, 400, jsonMessage("Order could not be marked as delivered."));
            return;
        }

        writeJson(exchange, 200, jsonMessage("Order marked as delivered."));
    }

    private UserAccount requireRole(HttpExchange exchange, UserRole role) throws IOException {
        UserAccount user = getLoggedInUser(exchange);
        if (user == null) {
            writeJson(exchange, 401, jsonMessage("Please login first."));
            return null;
        }
        if (user.getRole() != role) {
            writeJson(exchange, 403, jsonMessage("Access denied for this role."));
            return null;
        }
        return user;
    }

    private UserAccount getLoggedInUser(HttpExchange exchange) {
        String token = readCookie(exchange, "SESSION");
        if (token == null) {
            return null;
        }
        String username = sessions.get(token);
        if (username == null) {
            return null;
        }
        return authService.findByUsername(username).orElse(null);
    }

    private String readCookie(HttpExchange exchange, String name) {
        Headers headers = exchange.getRequestHeaders();
        if (!headers.containsKey("Cookie")) {
            return null;
        }

        for (String cookie : headers.get("Cookie")) {
            String[] parts = cookie.split(";");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.startsWith(name + "=")) {
                    return trimmed.substring(name.length() + 1);
                }
            }
        }
        return null;
    }

    private String getQueryParam(HttpExchange exchange, String key) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isBlank()) {
            return null;
        }
        String[] parts = query.split("&");
        for (String part : parts) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length == 2 && keyValue[0].equals(key)) {
                return keyValue[1].replace("%20", " ");
            }
        }
        return null;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void serveHtml(HttpExchange exchange, String html) throws IOException {
        byte[] data = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private void writeJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private void redirect(HttpExchange exchange, String path) throws IOException {
        exchange.getResponseHeaders().add("Location", path);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String jsonMessage(String message) {
        return "{\"message\":\"" + escape(message) + "\"}";
    }

    private String ordersJson(List<Order> orders) {
        StringBuilder json = new StringBuilder();
        json.append("{\"orders\":[");
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            json.append("{\"orderId\":\"").append(order.getOrderId())
                    .append("\",\"customerName\":\"").append(escape(order.getCustomerName()))
                    .append("\",\"restaurantName\":\"").append(escape(order.getRestaurantName()))
                    .append("\",\"itemName\":\"").append(escape(order.getItemName()))
                    .append("\",\"price\":").append(order.getPrice())
                    .append(",\"quantity\":").append(order.getQuantity())
                    .append(",\"totalPrice\":").append(order.getTotalPrice())
                    .append(",\"deliveryAddress\":\"").append(escape(order.getDeliveryAddress()))
                    .append("\",\"status\":\"").append(order.getStatus())
                    .append("\",\"orderTime\":\"").append(order.getOrderTime().format(TIME_FORMATTER))
                    .append("\"}");
            if (i < orders.size() - 1) {
                json.append(",");
            }
        }
        json.append("]}");
        return json.toString();
    }

    private interface JsonHandler {
        void handle(HttpExchange exchange, Map<String, String> payload) throws IOException;
    }

    private static class JsonPostHandler implements HttpHandler {
        private final JsonHandler handler;

        private JsonPostHandler(JsonHandler handler) {
            this.handler = handler;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }
            String body = readBody(exchange.getRequestBody());
            Map<String, String> payload = parseSimpleJson(body);
            handler.handle(exchange, payload);
        }

        private static String readBody(InputStream inputStream) throws IOException {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        private static Map<String, String> parseSimpleJson(String json) {
            Map<String, String> data = new HashMap<>();
            String trimmed = json.trim();
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
                return data;
            }
            String content = trimmed.substring(1, trimmed.length() - 1).trim();
            if (content.isEmpty()) {
                return data;
            }
            String[] pairs = content.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length != 2) {
                    continue;
                }
                String key = keyValue[0].trim().replaceAll("^\"|\"$", "");
                String value = keyValue[1].trim().replaceAll("^\"|\"$", "");
                data.put(key, value);
            }
            return data;
        }
    }

    private String loginPage() {
        return """
                <!doctype html>
                <html>
                <head>
                    <title>Login - Food Delivery Tracker</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 0; background: #f4f7fb; color: #1f2937; }
                        .layout { max-width: 980px; margin: 40px auto; display: grid; grid-template-columns: 1.1fr .9fr; gap: 24px; padding: 0 20px; }
                        .card { background: #fff; padding: 24px; border-radius: 12px; box-shadow: 0 10px 25px rgba(15,23,42,.08); }
                        label { display: block; margin-top: 12px; font-weight: bold; }
                        input, select, button { width: 100%; padding: 10px; margin-top: 6px; border: 1px solid #cbd5e1; border-radius: 8px; box-sizing: border-box; }
                        button { background: #4361d8; color: #fff; border: none; cursor: pointer; font-weight: bold; }
                        .secondary { background: #497d78; }
                        .hidden { display: none; }
                        .message { min-height: 24px; margin-top: 12px; color: #1d4ed8; }
                        .sample, .help { background: #f8fafc; border-radius: 10px; padding: 12px; margin-top: 12px; }
                        h1 { font-size: 42px; line-height: 1.05; margin-bottom: 28px; color: #182b4d; }
                        h2 { font-size: 26px; margin-bottom: 20px; color: #182b4d; }
                        p { font-size: 18px; }
                        @media (max-width: 800px) { .layout { grid-template-columns: 1fr; } }
                    </style>
                </head>
                <body>
                    <div class="layout">
                        <div class="card">
                            <h1>Role-Based Food Delivery Tracker</h1>
                            <p>Select a role, then login or register.</p>
                            <label for="role">Role</label>
                            <select id="role" onchange="toggleRestaurantForOwner()">
                                <option value="CUSTOMER">Customer</option>
                                <option value="RESTAURANT_OWNER">Restaurant Owner</option>
                                <option value="DELIVERY_PERSON">Delivery Person</option>
                            </select>
                            <label for="username">Username</label>
                            <input id="username" placeholder="Enter username" />
                            <label for="password">Password</label>
                            <input id="password" type="password" placeholder="Enter password" />
                            <div id="restaurantWrap" class="hidden">
                                <label for="ownerRestaurant">Restaurant</label>
                                <select id="ownerRestaurant"></select>
                            </div>
                            <button type="button" onclick="login()">Login</button>
                            <button type="button" class="secondary" onclick="register()">Register</button>
                            <div id="msg" class="message"></div>
                        </div>
                        <div class="card">
                            <h2>Sample Users</h2>
                            <div class="sample"><strong>Customer:</strong> customer / customer123</div>
                            <div class="sample"><strong>Owner:</strong> owner / owner123</div>
                            <div class="sample"><strong>Delivery:</strong> delivery / delivery123</div>
                            <div class="help">
                                <p><strong>Customer:</strong> place orders, track status, and view own history.</p>
                                <p><strong>Owner:</strong> see restaurant orders and filter by customer or status.</p>
                                <p><strong>Delivery:</strong> view delivery orders and mark delivered.</p>
                            </div>
                        </div>
                    </div>
                    <script>
                        let restaurants = [];
                        async function loadRestaurants() {
                            const res = await fetch('/api/restaurants');
                            const data = await res.json();
                            restaurants = data.restaurants || [];
                            const select = document.getElementById('ownerRestaurant');
                            select.innerHTML = restaurants.map(r => `<option value="${r.id}">${r.restaurantName}</option>`).join('');
                        }
                        function toggleRestaurantForOwner() {
                            const isOwner = document.getElementById('role').value === 'RESTAURANT_OWNER';
                            document.getElementById('restaurantWrap').className = isOwner ? '' : 'hidden';
                        }
                        async function post(url) {
                            const role = document.getElementById('role').value;
                            const payload = {
                                username: document.getElementById('username').value,
                                password: document.getElementById('password').value,
                                role: role
                            };
                            if (role === 'RESTAURANT_OWNER') {
                                payload.restaurantId = document.getElementById('ownerRestaurant').value;
                            }
                            const res = await fetch(url, {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify(payload)
                            });
                            return {status: res.status, body: await res.json()};
                        }
                        async function login() {
                            const result = await post('/api/login');
                            document.getElementById('msg').innerText = result.body.message;
                            if (result.status === 200) window.location = '/app';
                        }
                        async function register() {
                            const result = await post('/api/register');
                            document.getElementById('msg').innerText = result.body.message;
                        }
                        loadRestaurants();
                        toggleRestaurantForOwner();
                    </script>
                </body>
                </html>
                """;
    }

    private String customerDashboardPage(UserAccount user) {
        return """
                <!doctype html>
                <html>
                <head>
                    <title>Customer Dashboard</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 0; background: #f4f7fb; color: #1f2937; }
                        .page { max-width: 1150px; margin: 24px auto; padding: 0 20px 30px; }
                        .topbar { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 20px; }
                        .badge { background: #dbeafe; color: #1d4ed8; padding: 6px 12px; border-radius: 999px; font-size: 14px; display: inline-block; }
                        .grid { display: grid; grid-template-columns: 1.1fr .9fr; gap: 18px; }
                        .panel { background: #fff; padding: 20px; border-radius: 12px; box-shadow: 0 10px 25px rgba(15,23,42,.08); }
                        .full { margin-top: 18px; }
                        label { display: block; margin-top: 10px; font-weight: bold; }
                        input, select, textarea, button { width: 100%; padding: 10px; margin-top: 6px; border: 1px solid #cbd5e1; border-radius: 8px; box-sizing: border-box; }
                        textarea { min-height: 90px; resize: vertical; }
                        button { background: #4361d8; color: #fff; border: none; cursor: pointer; font-weight: bold; }
                        .price-box { background: #f8fafc; padding: 10px; border-radius: 8px; margin-top: 6px; border: 1px solid #e2e8f0; }
                        .message { min-height: 24px; margin-top: 10px; color: #1d4ed8; }
                        table { width: 100%; border-collapse: collapse; margin-top: 10px; }
                        th, td { border: 1px solid #e2e8f0; padding: 10px; text-align: left; }
                        th { background: #eff6ff; }
                        @media (max-width: 900px) { .grid { grid-template-columns: 1fr; } .topbar { flex-direction: column; align-items: flex-start; } }
                    </style>
                </head>
                <body>
                    <div class="page">
                        <div class="topbar">
                            <div>
                                <h1>Customer Dashboard</h1>
                                <div class="badge">Signed in as __USERNAME__</div>
                            </div>
                            <form method="post" action="/api/logout"><button type="submit">Logout</button></form>
                        </div>

                        <div class="grid">
                            <div class="panel">
                                <h2>Place Order</h2>
                                <label>Customer Name</label>
                                <input id="customerName" value="__USERNAME__">
                                <label>Restaurant Name</label>
                                <select id="restaurantSelect" onchange="loadMenuItems()"></select>
                                <label>Delivery Address</label>
                                <textarea id="deliveryAddress" placeholder="Enter delivery address"></textarea>
                                <label>Item Name</label>
                                <select id="itemSelect" onchange="updatePrice()"></select>
                                <label>Selected Item Price</label>
                                <div id="priceDisplay" class="price-box">Rs. 0.00</div>
                                <label>Quantity</label>
                                <input id="quantity" type="number" min="1" value="1">
                                <button type="button" onclick="placeOrder()">Place Order</button>
                                <div id="placeMsg" class="message"></div>
                            </div>

                            <div class="panel">
                                <h2>Order Status</h2>
                                <label>Order ID</label>
                                <input id="trackId" placeholder="Enter order ID">
                                <button type="button" onclick="checkStatus()">Check Status</button>
                                <div id="statusMsg" class="message"></div>
                            </div>
                        </div>

                        <div class="panel full">
                            <h2>My Order History</h2>
                            <table>
                                <thead><tr><th>Order ID</th><th>Restaurant</th><th>Item</th><th>Qty</th><th>Total</th><th>Address</th><th>Status</th><th>Order Time</th></tr></thead>
                                <tbody id="ordersBody"></tbody>
                            </table>
                        </div>
                    </div>
                    <script>
                        let menuItems = [];
                        async function init() {
                            const res = await fetch('/api/restaurants');
                            const data = await res.json();
                            const restaurantSelect = document.getElementById('restaurantSelect');
                            restaurantSelect.innerHTML = (data.restaurants || []).map(r => `<option value="${r.id}">${r.restaurantName}</option>`).join('');
                            if ((data.restaurants || []).length > 0) {
                                await loadMenuItems();
                            }
                            loadCustomerOrders();
                            setInterval(loadCustomerOrders, 3000);
                        }

                        async function loadMenuItems() {
                            const restaurantId = document.getElementById('restaurantSelect').value;
                            const res = await fetch('/api/menu?restaurantId=' + restaurantId);
                            const data = await res.json();
                            menuItems = data.items || [];
                            const itemSelect = document.getElementById('itemSelect');
                            itemSelect.innerHTML = menuItems.map(item => `<option value="${item.id}">${item.itemName}</option>`).join('');
                            updatePrice();
                        }

                        function updatePrice() {
                            const itemId = Number(document.getElementById('itemSelect').value);
                            const item = menuItems.find(i => i.id === itemId);
                            document.getElementById('priceDisplay').innerText = item ? ('Rs. ' + Number(item.price).toFixed(2)) : 'Rs. 0.00';
                        }

                        async function placeOrder() {
                            const payload = {
                                customerName: document.getElementById('customerName').value.trim(),
                                restaurantId: document.getElementById('restaurantSelect').value,
                                itemId: document.getElementById('itemSelect').value,
                                quantity: document.getElementById('quantity').value,
                                deliveryAddress: document.getElementById('deliveryAddress').value.trim()
                            };
                            const res = await fetch('/api/customer/place-order', {
                                method:'POST',
                                headers:{'Content-Type':'application/json'},
                                body: JSON.stringify(payload)
                            });
                            const data = await res.json();
                            document.getElementById('placeMsg').innerText = data.message + (data.orderId ? (' Order ID: ' + data.orderId) : '');
                            if (res.status === 200 && data.orderId) {
                                document.getElementById('trackId').value = data.orderId;
                            }
                            loadCustomerOrders();
                        }

                        async function checkStatus() {
                            const id = document.getElementById('trackId').value.trim();
                            const res = await fetch('/api/customer/status?id=' + encodeURIComponent(id));
                            const data = await res.json();
                            document.getElementById('statusMsg').innerText = data.status ? (data.orderId + ': ' + data.status) : data.message;
                        }

                        async function loadCustomerOrders() {
                            const res = await fetch('/api/customer/orders');
                            const data = await res.json();
                            const body = document.getElementById('ordersBody');
                            body.innerHTML = '';
                            (data.orders || []).forEach(order => {
                                body.innerHTML += `<tr><td>${order.orderId}</td><td>${order.restaurantName}</td><td>${order.itemName}</td><td>${order.quantity}</td><td>Rs. ${Number(order.totalPrice).toFixed(2)}</td><td>${order.deliveryAddress}</td><td>${order.status}</td><td>${order.orderTime}</td></tr>`;
                            });
                        }
                        init();
                    </script>
                </body>
                </html>
                """.replace("__USERNAME__", escapeHtml(user.getUsername()));
    }

    private String ownerDashboardPage(UserAccount user) {
        return """
                <!doctype html>
                <html>
                <head>
                    <title>Owner Dashboard</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 0; background: #f4f7fb; color: #1f2937; }
                        .page { max-width: 1180px; margin: 24px auto; padding: 0 20px 30px; }
                        .topbar { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 20px; }
                        .panel { background: #fff; padding: 20px; border-radius: 12px; box-shadow: 0 10px 25px rgba(15,23,42,.08); }
                        .filters { display: grid; grid-template-columns: 1fr 220px 160px; gap: 12px; align-items: end; }
                        .badge { background: #dcfce7; color: #166534; padding: 6px 12px; border-radius: 999px; font-size: 14px; display: inline-block; }
                        input, select, button { width: 100%; padding: 10px; margin-top: 6px; border: 1px solid #cbd5e1; border-radius: 8px; box-sizing: border-box; }
                        button { background: #4361d8; color: #fff; border: none; cursor: pointer; font-weight: bold; }
                        table { width: 100%; border-collapse: collapse; margin-top: 16px; }
                        th, td { border: 1px solid #e2e8f0; padding: 10px; text-align: left; }
                        th { background: #eff6ff; }
                        @media (max-width: 900px) { .filters { grid-template-columns: 1fr; } .topbar { flex-direction: column; align-items: flex-start; } }
                    </style>
                </head>
                <body>
                    <div class="page">
                        <div class="topbar">
                            <div>
                                <h1>Restaurant Owner Dashboard</h1>
                                <div class="badge">Signed in as __USERNAME__</div>
                            </div>
                            <form method="post" action="/api/logout"><button type="submit">Logout</button></form>
                        </div>
                        <div class="panel">
                            <h2>Restaurant Orders</h2>
                            <div class="filters">
                                <div>
                                    <label>Filter by Customer Name</label>
                                    <input id="ownerCustomerFilter" placeholder="Search customer name">
                                </div>
                                <div>
                                    <label>Filter by Status</label>
                                    <select id="ownerStatusFilter">
                                        <option value="">All Statuses</option>
                                        <option value="PLACED">PLACED</option>
                                        <option value="PREPARING">PREPARING</option>
                                        <option value="OUT_FOR_DELIVERY">OUT_FOR_DELIVERY</option>
                                        <option value="DELIVERED">DELIVERED</option>
                                    </select>
                                </div>
                                <div><button type="button" onclick="loadOwnerOrders()">Apply Filters</button></div>
                            </div>
                            <table>
                                <thead><tr><th>Order ID</th><th>Customer</th><th>Restaurant</th><th>Item</th><th>Qty</th><th>Address</th><th>Status</th><th>Order Time</th><th>Action</th></tr></thead>
                                <tbody id="ownerOrdersBody"></tbody>
                            </table>
                        </div>
                    </div>
                    <script>
                        async function loadOwnerOrders() {
                            const customer = encodeURIComponent(document.getElementById('ownerCustomerFilter').value.trim());
                            const status = encodeURIComponent(document.getElementById('ownerStatusFilter').value);
                            const res = await fetch('/api/owner/orders?customer=' + customer + '&status=' + status);
                            const data = await res.json();
                            const body = document.getElementById('ownerOrdersBody');
                            body.innerHTML = '';
                            (data.orders || []).forEach(order => {
                                const action = order.status === 'PLACED'
                                    ? `<button type="button" onclick="acceptOrder('${order.orderId}')">Accept Order</button>`
                                    : `<span>${order.status}</span>`;
                                body.innerHTML += `<tr><td>${order.orderId}</td><td>${order.customerName}</td><td>${order.restaurantName}</td><td>${order.itemName}</td><td>${order.quantity}</td><td>${order.deliveryAddress}</td><td>${order.status}</td><td>${order.orderTime}</td><td>${action}</td></tr>`;
                            });
                        }

                        async function acceptOrder(orderId) {
                            await fetch('/api/owner/accept-order', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({ orderId })
                            });
                            loadOwnerOrders();
                        }

                        document.getElementById('ownerCustomerFilter').addEventListener('input', loadOwnerOrders);
                        document.getElementById('ownerStatusFilter').addEventListener('change', loadOwnerOrders);
                        loadOwnerOrders();
                        setInterval(loadOwnerOrders, 4000);
                    </script>
                </body>
                </html>
                """.replace("__USERNAME__", escapeHtml(user.getUsername()));
    }

    private String deliveryDashboardPage(UserAccount user) {
        return """
                <!doctype html>
                <html>
                <head>
                    <title>Delivery Dashboard</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 0; background: #f4f7fb; color: #1f2937; }
                        .page { max-width: 1180px; margin: 24px auto; padding: 0 20px 30px; }
                        .topbar { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 20px; }
                        .panel { background: #fff; padding: 26px; border-radius: 12px; box-shadow: 0 10px 25px rgba(15,23,42,.08); }
                        .badge { background: #fee2e2; color: #c2410c; padding: 6px 14px; border-radius: 999px; font-size: 14px; display: inline-block; }
                        button { padding: 10px 14px; border-radius: 8px; border: none; cursor: pointer; font-weight: bold; background: #4361d8; color: #fff; }
                        table { width: 100%; border-collapse: collapse; margin-top: 14px; }
                        th, td { border: 1px solid #e2e8f0; padding: 10px 12px; text-align: left; vertical-align: middle; }
                        th { background: #eff6ff; }
                        #message { min-height: 24px; color: #1d4ed8; margin-top: 10px; }
                        @media (max-width: 900px) { .topbar { flex-direction: column; align-items: flex-start; } }
                    </style>
                </head>
                <body>
                    <div class="page">
                        <div class="topbar">
                            <div>
                                <h1>Delivery Person Dashboard</h1>
                                <div class="badge">Signed in as __USERNAME__</div>
                                <p>View delivery orders and mark them delivered.</p>
                            </div>
                            <form method="post" action="/api/logout"><button type="submit">Logout</button></form>
                        </div>
                        <div class="panel">
                            <h2>Delivery Orders</h2>
                            <div id="message"></div>
                            <table>
                                <thead><tr><th>Order ID</th><th>Customer</th><th>Restaurant</th><th>Item</th><th>Qty</th><th>Address</th><th>Status</th><th>Order Time</th><th>Action</th></tr></thead>
                                <tbody id="deliveryOrdersBody"></tbody>
                            </table>
                        </div>
                    </div>
                    <script>
                        async function markDelivered(orderId) {
                            const res = await fetch('/api/delivery/mark-delivered', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({ orderId })
                            });
                            const data = await res.json();
                            document.getElementById('message').innerText = data.message || '';
                            loadDeliveryOrders();
                        }

                        async function loadDeliveryOrders() {
                            const res = await fetch('/api/delivery/orders');
                            const data = await res.json();
                            const body = document.getElementById('deliveryOrdersBody');
                            body.innerHTML = '';
                            (data.orders || []).forEach(order => {
                                const action = order.status === 'OUT_FOR_DELIVERY'
                                    ? `<button type="button" onclick="markDelivered('${order.orderId}')">Mark Delivered</button>`
                                    : `<span>${order.status}</span>`;
                                body.innerHTML += `<tr><td>${order.orderId}</td><td>${order.customerName}</td><td>${order.restaurantName}</td><td>${order.itemName}</td><td>${order.quantity}</td><td>${order.deliveryAddress}</td><td>${order.status}</td><td>${order.orderTime}</td><td>${action}</td></tr>`;
                            });
                        }

                        loadDeliveryOrders();
                        setInterval(loadDeliveryOrders, 4000);
                    </script>
                </body>
                </html>
                """.replace("__USERNAME__", escapeHtml(user.getUsername()));
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
