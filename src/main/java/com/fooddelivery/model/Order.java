package com.fooddelivery.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderId;

    @ManyToOne(optional = false)
    private User customer;

    @ManyToOne(optional = false)
    private Restaurant restaurant;

    @ManyToOne(optional = false)
    private MenuItem menuItem;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private double itemPrice;

    @Column(nullable = false)
    private String deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Order() {}

    public Order(String orderId, User customer, Restaurant restaurant, MenuItem menuItem,
                     int quantity, double itemPrice, String deliveryAddress, OrderStatus status) {
        this.orderId = orderId;
        this.customer = customer;
        this.restaurant = restaurant;
        this.menuItem = menuItem;
        this.quantity = quantity;
        this.itemPrice = itemPrice;
        this.deliveryAddress = deliveryAddress;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public User getCustomer() { return customer; }
    public Restaurant getRestaurant() { return restaurant; }
    public MenuItem getMenuItem() { return menuItem; }
    public int getQuantity() { return quantity; }
    public double getItemPrice() { return itemPrice; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(OrderStatus status) { this.status = status; }
}
