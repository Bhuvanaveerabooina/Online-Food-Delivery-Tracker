package com.fooddelivery.model;

import java.io.Serializable;

public class UserAccount implements Serializable {
    private static final long serialVersionUID = 2L;

    private final String username;
    private final String passwordHash;
    private final UserRole role;
    private final Integer restaurantId;

    public UserAccount(String username, String passwordHash, UserRole role, Integer restaurantId) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.restaurantId = restaurantId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public Integer getRestaurantId() {
        return restaurantId;
    }
}
