package com.fooddelivery.app;

import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.Role;
import com.fooddelivery.repo.MenuItemRepository;
import com.fooddelivery.repo.RestaurantRepository;
import com.fooddelivery.repo.UserRepository;
import com.fooddelivery.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class OnlineFoodDeliveryTrackerApplication {
    private final RestaurantRepository restaurantRepository = new RestaurantRepository();
    private final MenuItemRepository menuItemRepository = new MenuItemRepository();
    private final UserRepository userRepository = new UserRepository();
    private final AuthService authService = new AuthService(userRepository);

    public static void main(String[] args) throws IOException {
        new OnlineFoodDeliveryTrackerApplication().start();
    }

    private void start() throws IOException {
        seedData();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", this::handleHome);
        server.createContext("/login", this::handleLogin);
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

        String html = buildPage(null, false);
        sendHtml(exchange, html, 200);
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
        boolean success = role != null && authService.authenticate(username, password, role).isPresent();

        String message;
        if (success) {
            message = "Login successful for " + role + " user: " + username;
        } else {
            message = "Invalid login details. Check username, password, and role requirements.";
        }

        sendHtml(exchange, buildPage(message, success), success ? 200 : 401);
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

    private String buildPage(String message, boolean success) {
        String statusBlock = "";
        if (message != null) {
            String statusClass = success ? "status success" : "status error";
            statusBlock = "<p class='" + statusClass + "'>" + message + "</p>";
        }

        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset='UTF-8'>
                  <title>Online Food Delivery Tracker</title>
                  <style>
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      font-family: 'Segoe UI', Arial, sans-serif;
                      min-height: 100vh;
                      display: flex;
                      align-items: center;
                      justify-content: center;
                      background: radial-gradient(circle at top, #0f172a 0%, #1e293b 40%, #334155 100%);
                      color: #0f172a;
                    }
                    .container {
                      width: min(880px, 94vw);
                      border-radius: 20px;
                      padding: 26px;
                      background: rgba(255, 255, 255, 0.95);
                      box-shadow: 0 20px 50px rgba(2, 6, 23, 0.35);
                    }
                    .header { margin-bottom: 18px; }
                    .badge {
                      display: inline-block;
                      background: #dbeafe;
                      color: #1d4ed8;
                      border-radius: 999px;
                      padding: 6px 10px;
                      font-size: 12px;
                      font-weight: 700;
                      margin-bottom: 8px;
                    }
                    h1 { margin: 0; font-size: 34px; }
                    .sub { margin: 8px 0 0; color: #334155; }
                    .layout { display: grid; grid-template-columns: 1.1fr 1fr; gap: 20px; }
                    .info, .login-card {
                      border: 1px solid #dbe3f4;
                      border-radius: 14px;
                      padding: 18px;
                      background: #ffffff;
                    }
                    .role { padding: 10px 0; border-bottom: 1px solid #e2e8f0; }
                    .role:last-child { border-bottom: 0; }
                    .role h3 { margin: 0 0 4px; font-size: 16px; }
                    .role p { margin: 0 0 4px; color: #475569; font-size: 14px; }
                    .demo { font-size: 13px; color: #0f172a; }
                    label { display: block; margin-top: 12px; font-weight: 700; font-size: 14px; }
                    input, select {
                      width: 100%;
                      padding: 11px 12px;
                      margin-top: 6px;
                      border: 1px solid #cbd5e1;
                      border-radius: 10px;
                      font-size: 14px;
                    }
                    .actions { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 16px; }
                    button {
                      padding: 12px;
                      border-radius: 10px;
                      border: 0;
                      font-size: 15px;
                      cursor: pointer;
                    }
                    .login-btn { background: #2563eb; color: white; font-weight: 700; }
                    .register-btn { background: #e2e8f0; color: #0f172a; font-weight: 600; }
                    .status { margin: 0 0 14px; padding: 12px; border-radius: 10px; font-weight: 600; }
                    .success { background: #dcfce7; color: #14532d; }
                    .error { background: #fee2e2; color: #7f1d1d; }
                    @media (max-width: 760px) {
                      .layout { grid-template-columns: 1fr; }
                      h1 { font-size: 28px; }
                    }
                  </style>
                </head>
                <body>
                  <div class='container'>
                    <div class='header'>
                      <span class='badge'>UPDATED WEB LOGIN</span>
                      <h1>Food Delivery Tracker</h1>
                      <p class='sub'>New split layout with clear role guidance and modern login controls.</p>
                    </div>
                    <div class='layout'>
                      <div class='info'>
                        <div class='role'>
                          <h3>Customer</h3>
                          <p>Place food orders and check your order history.</p>
                          <div class='demo'>Demo: customer1 / pass</div>
                        </div>
                        <div class='role'>
                          <h3>Restaurant Owner</h3>
                          <p>Manage incoming orders for your restaurant.</p>
                          <div class='demo'>Demo: owner_spice / pass or owner_pizza / pass</div>
                        </div>
                        <div class='role'>
                          <h3>Delivery Person</h3>
                          <p>Update delivery progress after pickup and drop.</p>
                          <div class='demo'>Demo: delivery1 / pass</div>
                        </div>
                      </div>

                      <div class='login-card'>
                        __STATUS__
                        <form method='post' action='/login'>
                          <label>Role</label>
                          <select name='role' required>
                            <option value='CUSTOMER'>CUSTOMER</option>
                            <option value='RESTAURANT_OWNER'>RESTAURANT_OWNER</option>
                            <option value='DELIVERY_PERSON'>DELIVERY_PERSON</option>
                          </select>

                          <label>Username</label>
                          <input type='text' name='username' required />

                          <label>Password</label>
                          <input type='password' name='password' required />

                          <div class='actions'>
                            <button class='login-btn' type='submit'>Login</button>
                            <button class='register-btn' type='button'>Register</button>
                          </div>
                        </form>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.replace("__STATUS__", statusBlock);
    }

    private void seedData() {
        if (restaurantRepository.count() > 0) {
            return;
        }

        Restaurant spiceHub = restaurantRepository.save("Spice Hub");
        Restaurant pizzaPoint = restaurantRepository.save("Pizza Point");

        menuItemRepository.save("Paneer Bowl", 180.0, spiceHub);
        menuItemRepository.save("Veg Biryani", 220.0, spiceHub);
        menuItemRepository.save("Margherita Pizza", 250.0, pizzaPoint);
        menuItemRepository.save("Farmhouse Pizza", 320.0, pizzaPoint);

        userRepository.save("customer1", "pass", Role.CUSTOMER, null);
        userRepository.save("owner_spice", "pass", Role.RESTAURANT_OWNER, spiceHub);
        userRepository.save("owner_pizza", "pass", Role.RESTAURANT_OWNER, pizzaPoint);
        userRepository.save("delivery1", "pass", Role.DELIVERY_PERSON, null);
    }
}
