package com.fooddelivery.model;

import jakarta.persistence.*;

@Entity
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    public Restaurant() {}

    public Restaurant(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}
