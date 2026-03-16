package com.fooddelivery.model;

public class Restaurant {
    private final int id;
    private final String restaurantName;

    public Restaurant(int id, String restaurantName) {
        this.id = id;
        this.restaurantName = restaurantName;
    }

    public int getId() {
        return id;
    }

    public String getRestaurantName() {
        return restaurantName;
    }
}
