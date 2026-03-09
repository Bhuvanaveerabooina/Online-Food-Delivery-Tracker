package com.fooddelivery.model;

import java.time.LocalDateTime;

/**
 * Data class representing one food order.
 */
public class Order {
    private final String orderId;
    private final String customerName;
    private final String itemName;
    private final int quantity;
    private final LocalDateTime orderTime;
    private OrderStatus status;

    public Order(String orderId, String customerName, String itemName, int quantity) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.itemName = itemName;
        this.quantity = quantity;
        this.orderTime = LocalDateTime.now();
        this.status = OrderStatus.PLACED;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getItemName() {
        return itemName;
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

    @Override
    public String toString() {
        return "Order ID: " + orderId
                + ", Customer: " + customerName
                + ", Item: " + itemName
                + ", Qty: " + quantity
                + ", Status: " + status
                + ", Time: " + orderTime;
    }
}
