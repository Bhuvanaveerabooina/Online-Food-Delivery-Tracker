package com.fooddelivery.model;

import jakarta.persistence.*;

@Entity
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double price;

    @ManyToOne(optional = false)
    private Restaurant restaurant;

    public MenuItem() {}

    public MenuItem(String name, double price, Restaurant restaurant) {
        this.name = name;
        this.price = price;
        this.restaurant = restaurant;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public Restaurant getRestaurant() { return restaurant; }

    @Override
    public String toString() {
        return name;
    }
}
