package com.fooddelivery.service;

import com.fooddelivery.model.Order;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction for order operations.
 */
public interface OrderOperations {
    Order placeOrder(String username, String customerName, String itemName, int quantity);

    Optional<Order> findOrderById(String username, String orderId);

    List<Order> getOrderHistory(String username);

    boolean markOrderAsDelivered(String username, String orderId);
}
