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
        server.createContext("/api/owner/update-status", new JsonPostHandler(this::handleOwnerUpdateStatus));

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
        serveHtml(exchange, appPage(user.getUsername(), user.getRole().name()));
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

    private void handleOwnerUpdateStatus(HttpExchange exchange, Map<String, String> payload) throws IOException {
        UserAccount user = requireRole(exchange, UserRole.RESTAURANT_OWNER);
        if (user == null) return;

        String orderId = payload.getOrDefault("orderId", "").trim();
        String statusText = payload.getOrDefault("status", "").trim().toUpperCase();

        if (orderId.isEmpty() || statusText.isEmpty()) {
            writeJson(exchange, 400, jsonMessage("orderId and status are required."));
            return;
        }

        try {
            OrderStatus status = OrderStatus.valueOf(statusText);
            boolean updated = orderService.updateOrderStatusForRestaurant(user.getRestaurantId(), orderId, status);
            if (!updated) {
                writeJson(exchange, 400, jsonMessage("Order not found or invalid status update."));
                return;
            }
            writeJson(exchange, 200, jsonMessage("Order status updated."));
        } catch (IllegalArgumentException exception) {
            writeJson(exchange, 400, jsonMessage("Invalid status."));
        }
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
                        body { font-family: Arial, sans-serif; margin: 40px; background: #f4f7ff; }
                        .card { max-width: 460px; background: white; padding: 24px; margin: auto; border-radius: 8px; }
                        input, button, select { width: 100%; padding: 10px; margin-top: 8px; }
                        .message { color: #204685; margin-top: 10px; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h2>Food Delivery Tracker Login</h2>
                        <input id="username" placeholder="Username" />
                        <input id="password" type="password" placeholder="Password" />
                        <select id="role" onchange="toggleRestaurantForOwner()">
                            <option value="CUSTOMER">Customer</option>
                            <option value="RESTAURANT_OWNER">Restaurant Owner</option>
                            <option value="DELIVERY_PERSON">Delivery Person</option>
                        </select>
                        <select id="ownerRestaurant" style="display:none"></select>
                        <button onclick="login()">Login</button>
                        <button onclick="register()">Register</button>
                        <div id="msg" class="message"></div>
                        <p>Sample users: customer/customer123, owner/owner123, delivery/delivery123</p>
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
                            document.getElementById('ownerRestaurant').style.display = isOwner ? 'block' : 'none';
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
                    </script>
                </body>
                </html>
                """;
    }

    private String appPage(String username, String role) {
        return """
                <!doctype html>
                <html>
                <head>
                    <title>Food Delivery Tracker</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 30px; background:#f7f9ff; }
                        .top { display:flex; justify-content:space-between; align-items:center; }
                        .panel { background:white; padding:16px; border-radius:8px; margin-top:14px; }
                        input, button, select { padding:8px; margin:4px; }
                        table { width:100%; border-collapse: collapse; margin-top:8px; }
                        th, td { border:1px solid #ddd; padding:8px; text-align:left; }
                    </style>
                </head>
                <body>
                    <div class="top">
                        <h2>Welcome, __USERNAME__ (__ROLE__)</h2>
                        <form method="post" action="/api/logout"><button>Logout</button></form>
                    </div>

                    <div id="customerDashboard" style="display:none">
                        <div class="panel">
                            <h3>Place Order</h3>
                            <input id="customerName" placeholder="Customer name" />
                            <select id="restaurantSelect" onchange="loadMenuItems()"></select>
                            <input id="deliveryAddress" placeholder="Delivery address" />
                            <select id="itemSelect" onchange="updatePrice()"></select>
                            <input id="price" readonly placeholder="Price" />
                            <input id="quantity" type="number" min="1" value="1" placeholder="Quantity" />
                            <button onclick="placeOrder()">Place Order</button>
                            <div id="placeMsg"></div>
                        </div>
                        <div class="panel">
                            <h3>Check Current Order Status</h3>
                            <input id="trackId" placeholder="Order ID" />
                            <button onclick="checkStatus()">Check Status</button>
                            <div id="statusMsg"></div>
                        </div>
                        <div class="panel">
                            <h3>My Order History</h3>
                            <table>
                                <thead><tr><th>ID</th><th>Restaurant</th><th>Item</th><th>Qty</th><th>Total</th><th>Address</th><th>Status</th><th>Time</th></tr></thead>
                                <tbody id="customerOrdersBody"></tbody>
                            </table>
                        </div>
                    </div>

                    <div id="ownerDashboard" style="display:none">
                        <div class="panel">
                            <h3>Restaurant Orders</h3>
                            <input id="ownerCustomerFilter" placeholder="Filter by customer" />
                            <select id="ownerStatusFilter">
                                <option value="">All Statuses</option>
                                <option value="PLACED">PLACED</option>
                                <option value="PREPARING">PREPARING</option>
                                <option value="OUT_FOR_DELIVERY">OUT_FOR_DELIVERY</option>
                                <option value="DELIVERED">DELIVERED</option>
                            </select>
                            <button onclick="loadOwnerOrders()">Apply Filters</button>
                            <table>
                                <thead><tr><th>ID</th><th>Customer</th><th>Item</th><th>Qty</th><th>Address</th><th>Time</th><th>Status</th><th>Update</th></tr></thead>
                                <tbody id="ownerOrdersBody"></tbody>
                            </table>
                        </div>
                    </div>

                    <div id="deliveryDashboard" style="display:none">
                        <div class="panel">
                            <h3>Orders To Deliver</h3>
                            <table>
                                <thead><tr><th>ID</th><th>Customer</th><th>Address</th><th>Restaurant</th><th>Item</th><th>Status</th><th>Action</th></tr></thead>
                                <tbody id="deliveryOrdersBody"></tbody>
                            </table>
                        </div>
                    </div>

                    <script>
                        const role = '__ROLE__';
                        let menuItems = [];
                        function showDashboard() {
                            if (role === 'CUSTOMER') document.getElementById('customerDashboard').style.display = 'block';
                            if (role === 'RESTAURANT_OWNER') document.getElementById('ownerDashboard').style.display = 'block';
                            if (role === 'DELIVERY_PERSON') document.getElementById('deliveryDashboard').style.display = 'block';
                        }

                        async function initCustomer() {
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
                            document.getElementById('price').value = item ? item.price : '';
                        }

                        async function placeOrder() {
                            const payload = {
                                customerName: document.getElementById('customerName').value,
                                restaurantId: document.getElementById('restaurantSelect').value,
                                itemId: document.getElementById('itemSelect').value,
                                quantity: document.getElementById('quantity').value,
                                deliveryAddress: document.getElementById('deliveryAddress').value
                            };
                            const res = await fetch('/api/customer/place-order', {
                                method:'POST',
                                headers:{'Content-Type':'application/json'},
                                body: JSON.stringify(payload)
                            });
                            const data = await res.json();
                            document.getElementById('placeMsg').innerText = data.message + (data.orderId ? (' ID: ' + data.orderId) : '');
                            loadCustomerOrders();
                        }

                        async function checkStatus() {
                            const id = document.getElementById('trackId').value;
                            const res = await fetch('/api/customer/status?id=' + encodeURIComponent(id));
                            const data = await res.json();
                            document.getElementById('statusMsg').innerText = data.status ? (data.orderId + ': ' + data.status) : data.message;
                        }

                        async function loadCustomerOrders() {
                            const res = await fetch('/api/customer/orders');
                            const data = await res.json();
                            const body = document.getElementById('customerOrdersBody');
                            body.innerHTML = '';
                            (data.orders || []).forEach(order => {
                                body.innerHTML += `<tr><td>${order.orderId}</td><td>${order.restaurantName}</td><td>${order.itemName}</td><td>${order.quantity}</td><td>${order.totalPrice}</td><td>${order.deliveryAddress}</td><td>${order.status}</td><td>${order.orderTime}</td></tr>`;
                            });
                        }

                        async function loadOwnerOrders() {
                            const customer = encodeURIComponent(document.getElementById('ownerCustomerFilter').value || '');
                            const status = encodeURIComponent(document.getElementById('ownerStatusFilter').value || '');
                            const res = await fetch('/api/owner/orders?customer=' + customer + '&status=' + status);
                            const data = await res.json();
                            const body = document.getElementById('ownerOrdersBody');
                            body.innerHTML = '';
                            (data.orders || []).forEach(order => {
                                const options = ['PLACED', 'PREPARING', 'OUT_FOR_DELIVERY']
                                    .map(status => `<option value="${status}" ${status === order.status ? 'selected' : ''}>${status}</option>`)
                                    .join('');
                                body.innerHTML += `<tr><td>${order.orderId}</td><td>${order.customerName}</td><td>${order.itemName}</td><td>${order.quantity}</td><td>${order.deliveryAddress}</td><td>${order.orderTime}</td><td>${order.status}</td><td><select id="status-${order.orderId}">${options}</select><button onclick="updateOwnerStatus('${order.orderId}')">Save</button></td></tr>`;
                            });
                        }

                        async function updateOwnerStatus(orderId) {
                            const status = document.getElementById('status-' + orderId).value;
                            await fetch('/api/owner/update-status', {
                                method:'POST',
                                headers:{'Content-Type':'application/json'},
                                body: JSON.stringify({ orderId, status })
                            });
                            loadOwnerOrders();
                        }

                        async function loadDeliveryOrders() {
                            const res = await fetch('/api/delivery/orders');
                            const data = await res.json();
                            const body = document.getElementById('deliveryOrdersBody');
                            body.innerHTML = '';
                            (data.orders || []).forEach(order => {
                                body.innerHTML += `<tr><td>${order.orderId}</td><td>${order.customerName}</td><td>${order.deliveryAddress}</td><td>${order.restaurantName}</td><td>${order.itemName}</td><td>${order.status}</td><td><button onclick="markDelivered('${order.orderId}')">Mark Delivered</button></td></tr>`;
                            });
                        }

                        async function markDelivered(orderId) {
                            await fetch('/api/delivery/mark-delivered', {
                                method:'POST',
                                headers:{'Content-Type':'application/json'},
                                body: JSON.stringify({ orderId })
                            });
                            loadDeliveryOrders();
                        }

                        showDashboard();
                        if (role === 'CUSTOMER') initCustomer();
                        if (role === 'RESTAURANT_OWNER') { loadOwnerOrders(); setInterval(loadOwnerOrders, 3000); }
                        if (role === 'DELIVERY_PERSON') { loadDeliveryOrders(); setInterval(loadDeliveryOrders, 3000); }
                    </script>
                </body>
                </html>
                """.replace("__USERNAME__", escape(username)).replace("__ROLE__", role);
    }
}
