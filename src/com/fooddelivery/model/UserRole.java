package com.fooddelivery.model;

public enum UserRole {
    CUSTOMER,
    RESTAURANT_OWNER,
    DELIVERY_PERSON;

    public static UserRole fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role is required.");
        }

        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "CUSTOMER" -> CUSTOMER;
            case "RESTAURANT_OWNER", "RESTAURANT OWNER" -> RESTAURANT_OWNER;
            case "DELIVERY_PERSON", "DELIVERY PERSON" -> DELIVERY_PERSON;
            default -> throw new IllegalArgumentException("Unsupported role: " + value);
        };
    }
}
