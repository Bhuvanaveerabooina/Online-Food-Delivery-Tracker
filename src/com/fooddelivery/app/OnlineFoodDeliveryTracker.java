package com.fooddelivery.app;

import com.fooddelivery.model.Order;
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
 * Starts a web application for real-time order tracking with login.
 */
public class OnlineFoodDeliveryTracker {
    private static final int PORT = 8080;
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
        server.createContext("/login", exchange -> serveHtml(exchange, loginPage()));
        server.createContext("/app", this::handleApp);
        server.createContext("/api/register", new JsonPostHandler(this::handleRegister));
        server.createContext("/api/login", new JsonPostHandler(this::handleLogin));
        server.createContext("/api/logout", this::handleLogout);
        server.createContext("/api/place-order", new JsonPostHandler(this::handlePlaceOrder));
        server.createContext("/api/status", this::handleStatus);
        server.createContext("/api/orders", this::handleOrders);
        server.createContext("/api/confirm-delivery", new JsonPostHandler(this::handleConfirmDelivery));

        server.setExecutor(null);
        server.start();
        System.out.println("Server started on http://localhost:" + PORT);
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        String username = getLoggedInUser(exchange);
        redirect(exchange, username == null ? "/login" : "/app");
    }

    private void handleApp(HttpExchange exchange) throws IOException {
        String username = getLoggedInUser(exchange);
        if (username == null) {
            redirect(exchange, "/login");
            return;
        }
        serveHtml(exchange, appPage(username));
    }

    private void handleRegister(HttpExchange exchange, Map<String, String> payload) throws IOException {
        try {
            authService.register(payload.get("username"), payload.get("password"));
            writeJson(exchange, 200, "{\"message\":\"Account created successfully. Please login.\"}");
        } catch (IllegalArgumentException exception) {
            writeJson(exchange, 400, jsonMessage(exception.getMessage()));
        }
    }

    private void handleLogin(HttpExchange exchange, Map<String, String> payload) throws IOException {
        String username = payload.getOrDefault("username", "").trim();
        boolean authenticated = authService.authenticate(username, payload.get("password"));
        if (!authenticated) {
            writeJson(exchange, 401, jsonMessage("Invalid username or password."));
            return;
        }

        String token = UUID.randomUUID().toString();
        sessions.put(token, username);
        exchange.getResponseHeaders().add("Set-Cookie", "SESSION=" + token + "; Path=/; HttpOnly");
        writeJson(exchange, 200, "{\"message\":\"Login successful.\"}");
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = readCookie(exchange, "SESSION");
        if (token != null) {
            sessions.remove(token);
        }
        exchange.getResponseHeaders().add("Set-Cookie", "SESSION=deleted; Path=/; Max-Age=0");
        redirect(exchange, "/login");
    }

    private void handlePlaceOrder(HttpExchange exchange, Map<String, String> payload) throws IOException {
        String username = getLoggedInUser(exchange);
        if (username == null) {
            writeJson(exchange, 401, jsonMessage("Please login first."));
            return;
        }

        try {
            int quantity = Integer.parseInt(payload.getOrDefault("quantity", "0"));
            Order order = orderService.placeOrder(
                    username,
                    payload.getOrDefault("customerName", "").trim(),
                    payload.getOrDefault("itemName", "").trim(),
                    quantity
            );
            writeJson(exchange, 200, "{\"message\":\"Order placed.\",\"orderId\":\"" + order.getOrderId() + "\"}");
        } catch (Exception exception) {
            writeJson(exchange, 400, jsonMessage("Could not place order: " + exception.getMessage()));
        }
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        String username = getLoggedInUser(exchange);
        if (username == null) {
            writeJson(exchange, 401, jsonMessage("Please login first."));
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String orderId = "";
        if (query != null && query.startsWith("id=")) {
            orderId = query.substring(3);
        }

        Optional<Order> order = orderService.findOrderById(username, orderId);
        if (order.isEmpty()) {
            writeJson(exchange, 404, jsonMessage("Order not found."));
            return;
        }

        writeJson(exchange, 200, "{\"orderId\":\"" + order.get().getOrderId() + "\",\"status\":\"" + order.get().getStatus() + "\"}");
    }


    private void handleConfirmDelivery(HttpExchange exchange, Map<String, String> payload) throws IOException {
        String username = getLoggedInUser(exchange);
        if (username == null) {
            writeJson(exchange, 401, jsonMessage("Please login first."));
            return;
        }

        String orderId = payload.getOrDefault("orderId", "").trim();
        if (orderId.isEmpty()) {
            writeJson(exchange, 400, jsonMessage("orderId is required."));
            return;
        }

        Optional<Order> order = orderService.findOrderById(username, orderId);
        if (order.isEmpty()) {
            writeJson(exchange, 404, jsonMessage("Order not found."));
            return;
        }

        if (order.get().getStatus() != com.fooddelivery.model.OrderStatus.AWAITING_CUSTOMER_VERIFICATION) {
            writeJson(exchange, 400, jsonMessage("Order is not ready for customer verification."));
            return;
        }

        boolean updated = orderService.markOrderAsDelivered(username, orderId);
        if (!updated) {
            writeJson(exchange, 400, jsonMessage("Order could not be marked as delivered."));
            return;
        }

        writeJson(exchange, 200, jsonMessage("Order marked as delivered."));
    }

    private void handleOrders(HttpExchange exchange) throws IOException {
        String username = getLoggedInUser(exchange);
        if (username == null) {
            writeJson(exchange, 401, jsonMessage("Please login first."));
            return;
        }

        List<Order> orders = orderService.getOrderHistory(username);
        StringBuilder json = new StringBuilder();
        json.append("{\"orders\":[");
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            json.append("{\"orderId\":\"").append(order.getOrderId())
                    .append("\",\"customerName\":\"").append(escape(order.getCustomerName()))
                    .append("\",\"itemName\":\"").append(escape(order.getItemName()))
                    .append("\",\"quantity\":").append(order.getQuantity())
                    .append(",\"status\":\"").append(order.getStatus())
                    .append("\",\"orderTime\":\"").append(order.getOrderTime().format(TIME_FORMATTER))
                    .append("\"}");
            if (i < orders.size() - 1) {
                json.append(",");
            }
        }
        json.append("]}");
        writeJson(exchange, 200, json.toString());
    }

    private String getLoggedInUser(HttpExchange exchange) {
        String token = readCookie(exchange, "SESSION");
        if (token == null) {
            return null;
        }
        return sessions.get(token);
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
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String jsonMessage(String message) {
        return "{\"message\":\"" + escape(message) + "\"}";
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
                        input, button { width: 100%; padding: 10px; margin-top: 8px; }
                        .message { color: #204685; margin-top: 10px; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h2>Food Delivery Tracker Login</h2>
                        <input id="username" placeholder="Username" />
                        <input id="password" type="password" placeholder="Password" />
                        <button onclick="login()">Login</button>
                        <button onclick="register()">Register</button>
                        <div id="msg" class="message"></div>
                    </div>
                    <script>
                        async function post(url) {
                            const res = await fetch(url, {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({
                                    username: document.getElementById('username').value,
                                    password: document.getElementById('password').value
                                })
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
                    </script>
                </body>
                </html>
                """;
    }

    private String appPage(String username) {
        return """
                <!doctype html>
                <html>
                <head>
                    <title>Food Delivery Tracker</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 30px; background:#f7f9ff; }
                        .top { display:flex; justify-content:space-between; align-items:center; }
                        .panel { background:white; padding:16px; border-radius:8px; margin-top:14px; }
                        input, button { padding:8px; margin:4px; }
                        table { width:100%; border-collapse: collapse; margin-top:8px; }
                        th, td { border:1px solid #ddd; padding:8px; text-align:left; }
                    </style>
                </head>
                <body>
                    <div class="top">
                        <h2>Welcome, __USERNAME__</h2>
                        <form method="post" action="/api/logout"><button>Logout</button></form>
                    </div>

                    <div class="panel">
                        <h3>Place Order</h3>
                        <input id="customer" placeholder="Customer name" />
                        <input id="item" placeholder="Food item" />
                        <input id="qty" type="number" placeholder="Quantity" />
                        <button onclick="placeOrder()">Place</button>
                        <div id="placeMsg"></div>
                    </div>

                    <div class="panel">
                        <h3>Check Status</h3>
                        <input id="trackId" placeholder="Order ID" />
                        <button onclick="checkStatus()">Check</button>
                        <button onclick="confirmDelivery()">Mark Delivered (Customer Verification)</button>
                        <div id="statusMsg"></div>
                    </div>

                    <div class="panel">
                        <h3>Order History (Real-time refresh)</h3>
                        <table>
                            <thead><tr><th>ID</th><th>Customer</th><th>Item</th><th>Qty</th><th>Status</th><th>Time</th></tr></thead>
                            <tbody id="ordersBody"></tbody>
                        </table>
                    </div>

                    <script>
                        async function placeOrder() {
                            const res = await fetch('/api/place-order', {
                                method:'POST',
                                headers:{'Content-Type':'application/json'},
                                body: JSON.stringify({
                                    customerName: document.getElementById('customer').value,
                                    itemName: document.getElementById('item').value,
                                    quantity: document.getElementById('qty').value
                                })
                            });
                            const data = await res.json();
                            document.getElementById('placeMsg').innerText = data.message + (data.orderId ? (' ID: ' + data.orderId) : '');
                            loadOrders();
                        }
                        async function checkStatus() {
                            const id = document.getElementById('trackId').value;
                            const res = await fetch('/api/status?id=' + encodeURIComponent(id));
                            const data = await res.json();
                            document.getElementById('statusMsg').innerText = data.status ? (data.orderId + ': ' + data.status) : data.message;
                        }
                        async function confirmDelivery() {
                            const id = document.getElementById('trackId').value;
                            const res = await fetch('/api/confirm-delivery', {
                                method:'POST',
                                headers:{'Content-Type':'application/json'},
                                body: JSON.stringify({ orderId: id })
                            });
                            const data = await res.json();
                            document.getElementById('statusMsg').innerText = data.message || 'Updated';
                            loadOrders();
                        }
                        async function loadOrders() {
                            const res = await fetch('/api/orders');
                            const data = await res.json();
                            const body = document.getElementById('ordersBody');
                            body.innerHTML = '';
                            (data.orders || []).forEach(order => {
                                const row = `<tr><td>${order.orderId}</td><td>${order.customerName}</td><td>${order.itemName}</td><td>${order.quantity}</td><td>${order.status}</td><td>${order.orderTime}</td></tr>`;
                                body.innerHTML += row;
                            });
                        }
                        setInterval(loadOrders, 2000);
                        loadOrders();
                    </script>
                </body>
                </html>
                """.replace("__USERNAME__", escape(username));
    }
}
