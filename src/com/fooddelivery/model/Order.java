package com.fooddelivery.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Data class representing one food order.
 */
public class Order implements Serializable {
    private static final long serialVersionUID = 2L;

    private final String orderId;
    private final String customerName;
    private final String customerUserId;
    private final int restaurantId;
    private final String restaurantName;
    private final int itemId;
    private final String itemName;
    private final double price;
    private final int quantity;
    private final double totalPrice;
    private final String deliveryAddress;
    private final LocalDateTime orderTime;
    private OrderStatus status;
    private String assignedDeliveryPersonId;

    public Order(
            String orderId,
            String customerName,
            String customerUserId,
            int restaurantId,
            String restaurantName,
            int itemId,
            String itemName,
            double price,
            int quantity,
            double totalPrice,
            String deliveryAddress,
            OrderStatus status,
            String assignedDeliveryPersonId,
            LocalDateTime orderTime
    ) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.customerUserId = customerUserId;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.itemId = itemId;
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.deliveryAddress = deliveryAddress;
        this.status = status;
        this.assignedDeliveryPersonId = assignedDeliveryPersonId;
        this.orderTime = orderTime;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerUserId() { return customerUserId; }
    public int getRestaurantId() { return restaurantId; }
    public String getRestaurantName() { return restaurantName; }
    public int getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public double getTotalPrice() { return totalPrice; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public LocalDateTime getOrderTime() { return orderTime; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getAssignedDeliveryPersonId() { return assignedDeliveryPersonId; }
    public void setAssignedDeliveryPersonId(String assignedDeliveryPersonId) { this.assignedDeliveryPersonId = assignedDeliveryPersonId; }
}
