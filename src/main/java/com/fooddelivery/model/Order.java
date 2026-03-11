package com.fooddelivery.model;

import java.time.LocalDateTime;

public class Order {
    private final Long id;
    private final String orderId;
    private final User customer;
    private final Restaurant restaurant;
    private final MenuItem menuItem;
    private final int quantity;
    private final double itemPrice;
    private final String deliveryAddress;
    private OrderStatus status;
    private final LocalDateTime createdAt;

    public Order(Long id, String orderId, User customer, Restaurant restaurant, MenuItem menuItem,
                 int quantity, double itemPrice, String deliveryAddress, OrderStatus status) {
        this.id = id;
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

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return orderId + " (" + customer.getUsername() + " - " + status + ")";
    }
}
