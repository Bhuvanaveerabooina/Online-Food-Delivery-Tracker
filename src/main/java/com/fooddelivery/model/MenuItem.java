package com.fooddelivery.model;

public class MenuItem {
    private final Long id;
    private final String name;
    private final double price;
    private final Restaurant restaurant;

    public MenuItem(Long id, String name, double price, Restaurant restaurant) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.restaurant = restaurant;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    @Override
    public String toString() {
        return name;
    }
}
