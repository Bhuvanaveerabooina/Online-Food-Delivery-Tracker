package com.fooddelivery.model;

public class MenuItem {
    private final int id;
    private final int restaurantId;
    private final String itemName;
    private final double price;

    public MenuItem(int id, int restaurantId, String itemName, double price) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.itemName = itemName;
        this.price = price;
    }

    public int getId() {
        return id;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public String getItemName() {
        return itemName;
    }

    public double getPrice() {
        return price;
    }
}
