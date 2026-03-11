package com.fooddelivery.service;

import com.fooddelivery.model.Role;
import com.fooddelivery.model.UserAccount;

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
        ensureDefaultUsers();
    }

    public synchronized Optional<UserAccount> authenticate(String username, String password, Role role) {
        if (username == null || password == null || role == null) {
            return Optional.empty();
        }

        return users.stream()
                .filter(account -> account.getUsername().equalsIgnoreCase(username.trim()))
                .filter(account -> account.getRole() == role)
                .filter(account -> account.getPasswordHash().equals(hash(password)))
                .findFirst();
    }

    private void ensureDefaultUsers() {
        if (!users.isEmpty()) {
            return;
        }

        users.add(new UserAccount("customer1", hash("cust123"), Role.CUSTOMER, null));
        users.add(new UserAccount("owner_spice", hash("owner123"), Role.RESTAURANT_OWNER, "Spice Hub"));
        users.add(new UserAccount("owner_burger", hash("owner123"), Role.RESTAURANT_OWNER, "Burger Barn"));
        users.add(new UserAccount("owner_pizza", hash("owner123"), Role.RESTAURANT_OWNER, "Pizza Corner"));
        users.add(new UserAccount("delivery1", hash("del123"), Role.DELIVERY_PERSON, null));

        userFileStore.saveUsers(users);
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
