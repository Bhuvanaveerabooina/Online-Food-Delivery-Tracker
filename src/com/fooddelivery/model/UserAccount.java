package com.fooddelivery.model;

import java.io.Serializable;

public class UserAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String passwordHash;
    private final Role role;
    private final String restaurantName;

    public UserAccount(String username, String passwordHash, Role role, String restaurantName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.restaurantName = restaurantName;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getRestaurantName() {
        return restaurantName;
    }
}
