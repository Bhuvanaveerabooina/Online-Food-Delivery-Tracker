package com.fooddelivery.model;

public enum Role {
    CUSTOMER,
    RESTAURANT_OWNER,
    DELIVERY_PERSON;

    public String getDisplayName() {
        return name().replace('_', ' ');
    }
}
