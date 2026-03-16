package com.fooddelivery.model;

public enum UserRole {
    CUSTOMER,
    RESTAURANT_OWNER,
    DELIVERY_PERSON;

    public static UserRole fromText(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role is required.");
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "CUSTOMER" -> CUSTOMER;
            case "RESTAURANT_OWNER", "OWNER" -> RESTAURANT_OWNER;
            case "DELIVERY_PERSON", "DELIVERY" -> DELIVERY_PERSON;
            default -> throw new IllegalArgumentException("Invalid role selected.");
        };
    }
}
