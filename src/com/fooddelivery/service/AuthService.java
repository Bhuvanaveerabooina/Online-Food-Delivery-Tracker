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
        migrateLegacyUsersWithoutRole();
    }

    private void migrateLegacyUsersWithoutRole() {
        boolean changed = false;
        for (int i = 0; i < users.size(); i++) {
            UserAccount account = users.get(i);
            if (account.getRole() == null) {
                users.set(i, new UserAccount(account.getUsername(), account.getPasswordHash(), UserRole.CUSTOMER));
                changed = true;
            }
        }
        if (changed) {
            userFileStore.saveUsers(users);
        }
    }

    public synchronized void register(String username, String password, String roleValue) {
        validate(username, password);
        UserRole role = UserRole.fromValue(roleValue);
        boolean exists = users.stream().anyMatch(user -> user.getUsername().equalsIgnoreCase(username));
        if (exists) {
            throw new IllegalArgumentException("Username already exists.");
        }
        users.add(new UserAccount(username.trim(), hash(password), role));
        userFileStore.saveUsers(users);
    }

    public synchronized Optional<UserAccount> authenticate(String username, String password) {
        Optional<UserAccount> user = users.stream()
                .filter(account -> account.getUsername().equalsIgnoreCase(username))
                .findFirst();

        return user.filter(value -> value.getPasswordHash().equals(hash(password)));
    }

    private void validate(String username, String password) {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters.");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters.");
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
