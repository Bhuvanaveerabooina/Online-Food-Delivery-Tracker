package com.fooddelivery.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Order implements Serializable {
    private static final long serialVersionUID = 2L;

    private final String orderId;
    private final String customerUsername;
    private final String customerName;
    private final String restaurantName;
    private final String itemName;
    private final double itemPrice;
    private final int quantity;
    private final String address;
    private final LocalDateTime orderTime;
    private OrderStatus status;
    private String assignedDeliveryUsername;

    public Order(String orderId, String customerUsername, String customerName, String restaurantName,
                 String itemName, double itemPrice, int quantity, String address) {
        this.orderId = orderId;
        this.customerUsername = customerUsername;
        this.customerName = customerName;
        this.restaurantName = restaurantName;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.quantity = quantity;
        this.address = address;
        this.orderTime = LocalDateTime.now();
        this.status = OrderStatus.PLACED;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerUsername() { return customerUsername; }
    public String getCustomerName() { return customerName; }
    public String getRestaurantName() { return restaurantName; }
    public String getItemName() { return itemName; }
    public double getItemPrice() { return itemPrice; }
    public int getQuantity() { return quantity; }
    public String getAddress() { return address; }
    public LocalDateTime getOrderTime() { return orderTime; }
    public OrderStatus getStatus() { return status; }
    public String getAssignedDeliveryUsername() { return assignedDeliveryUsername; }

    public double getTotalPrice() {
        return itemPrice * quantity;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public void setAssignedDeliveryUsername(String assignedDeliveryUsername) {
        this.assignedDeliveryUsername = assignedDeliveryUsername;
    }
}
