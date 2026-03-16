package com.fooddelivery.service;

import com.fooddelivery.model.UserAccount;
import com.fooddelivery.model.UserRole;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuthService {
    private final UserFileStore userFileStore = new UserFileStore();
    private final List<UserAccount> users = new ArrayList<>();

    public AuthService() {
        users.addAll(userFileStore.loadUsers());
        seedUsersIfNeeded();
    }

    public synchronized void register(String username, String password, UserRole role, Integer restaurantId) {
        validate(username, password, role, restaurantId);
        boolean exists = users.stream().anyMatch(user -> user.getUsername().equalsIgnoreCase(username));
        if (exists) {
            throw new IllegalArgumentException("Username already exists.");
        }
        users.add(new UserAccount(username.trim(), hash(password), role, restaurantId));
        userFileStore.saveUsers(users);
    }

    public synchronized boolean authenticate(String username, String password, UserRole role) {
        Optional<UserAccount> user = users.stream()
                .filter(account -> account.getUsername().equalsIgnoreCase(username))
                .filter(account -> account.getRole() == role)
                .findFirst();

        return user.map(value -> value.getPasswordHash().equals(hash(password))).orElse(false);
    }

    public synchronized Optional<UserAccount> findByUsername(String username) {
        return users.stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    private void seedUsersIfNeeded() {
        boolean changed = false;
        if (users.stream().noneMatch(user -> user.getUsername().equalsIgnoreCase("customer"))) {
            users.add(new UserAccount("customer", hash("customer123"), UserRole.CUSTOMER, null));
            changed = true;
        }
        if (users.stream().noneMatch(user -> user.getUsername().equalsIgnoreCase("owner"))) {
            users.add(new UserAccount("owner", hash("owner123"), UserRole.RESTAURANT_OWNER, 1));
            changed = true;
        }
        if (users.stream().noneMatch(user -> user.getUsername().equalsIgnoreCase("delivery"))) {
            users.add(new UserAccount("delivery", hash("delivery123"), UserRole.DELIVERY_PERSON, null));
            changed = true;
        }
        if (changed) {
            userFileStore.saveUsers(users);
        }
    }

    private void validate(String username, String password, UserRole role, Integer restaurantId) {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters.");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }
        if (role == UserRole.RESTAURANT_OWNER && restaurantId == null) {
            throw new IllegalArgumentException("Restaurant owner must be mapped to a restaurant.");
        }
    }

    private String hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte hashByte : hashBytes) {
                builder.append(String.format("%02x", hashByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
