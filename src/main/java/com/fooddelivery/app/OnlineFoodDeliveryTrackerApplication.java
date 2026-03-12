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
            String color = success ? "#14532d" : "#7f1d1d";
            String bg = success ? "#dcfce7" : "#fee2e2";
            statusBlock = "<p style='padding:10px;border-radius:8px;color:" + color + ";background:" + bg + ";'>" + message + "</p>";
        }

        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset='UTF-8'>
                  <title>Online Food Delivery Tracker</title>
                  <style>
                    body { font-family: Arial, sans-serif; background: #f5f7fb; margin: 0; }
                    .container { max-width: 900px; margin: 40px auto; background: white; border-radius: 12px; padding: 24px; box-shadow: 0 6px 20px rgba(0,0,0,0.08); }
                    h1 { margin-top: 0; }
                    .roles { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
                    .card { border: 1px solid #dbe3f4; border-radius: 10px; padding: 12px; background: #fbfdff; }
                    label { display: block; margin-top: 10px; font-weight: bold; }
                    input, select { width: 100%; box-sizing: border-box; padding: 8px; margin-top: 4px; }
                    button { margin-top: 14px; padding: 10px 16px; background: #1d4ed8; color: white; border: 0; border-radius: 8px; cursor: pointer; }
                    small { color: #475569; }
                  </style>
                </head>
                <body>
                  <div class='container'>
                    <h1>Online Food Delivery Tracker Login</h1>
                    <p>Use one localhost page for all logins. Choose role, then enter matching credentials.</p>
                    __STATUS__
                    <div class='roles'>
                      <div class='card'>
                        <h3>Customer</h3>
                        <small>Can place orders and check order history.</small><br>
                        <strong>Demo:</strong> customer1 / pass
                      </div>
                      <div class='card'>
                        <h3>Restaurant Owner</h3>
                        <small>Can manage own restaurant orders.</small><br>
                        <strong>Demo:</strong> owner_spice / pass or owner_pizza / pass
                      </div>
                      <div class='card'>
                        <h3>Delivery Person</h3>
                        <small>Can update delivery status.</small><br>
                        <strong>Demo:</strong> delivery1 / pass
                      </div>
                    </div>

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

                      <button type='submit'>Login</button>
                    </form>
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
