package com.fooddelivery.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    private Restaurant restaurant;

    public User() {}

    public User(String username, String password, Role role, Restaurant restaurant) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.restaurant = restaurant;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public Restaurant getRestaurant() { return restaurant; }
}
