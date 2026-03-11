package com.fooddelivery.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Data class representing one food order.
 */
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String orderId;
    private final String placedByUsername;
    private final String customerName;
    private final String customerAddress;
    private final String restaurantName;
    private final String itemName;
    private final double price;
    private final int quantity;
    private final LocalDateTime orderTime;
    private boolean deliveredByDeliveryPerson;
    private OrderStatus status;

    public Order(String orderId, String placedByUsername, String customerName, String customerAddress,
                 String restaurantName, String itemName, double price, int quantity) {
        this.orderId = orderId;
        this.placedByUsername = placedByUsername;
        this.customerName = customerName;
        this.customerAddress = customerAddress;
        this.restaurantName = restaurantName;
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
        this.orderTime = LocalDateTime.now();
        this.status = OrderStatus.PLACED;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getPlacedByUsername() {
        return placedByUsername;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public String getItemName() {
        return itemName;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public boolean isDeliveredByDeliveryPerson() {
        return deliveredByDeliveryPerson;
    }

    public void setDeliveredByDeliveryPerson(boolean deliveredByDeliveryPerson) {
        this.deliveredByDeliveryPerson = deliveredByDeliveryPerson;
    }
}
