package com.fooddelivery.model;

import java.io.Serializable;

public class MenuItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final double price;

    public MenuItem(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }
}
