package com.fooddelivery.model;

public class User {
    private final Long id;
    private final String username;
    private final String password;
    private final Role role;
    private final Restaurant restaurant;

    public User(Long id, String username, String password, Role role, Restaurant restaurant) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.restaurant = restaurant;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }
}
