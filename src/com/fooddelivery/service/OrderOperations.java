package com.fooddelivery.service;

import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.model.Restaurant;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction for order operations.
 */
public interface OrderOperations {
    Order placeOrder(String username, String customerName, int restaurantId, int itemId, int quantity, String deliveryAddress);

    Optional<Order> findCustomerOrderById(String username, String orderId);

    List<Order> getCustomerOrderHistory(String username);

    List<Order> getOrdersForRestaurant(int restaurantId, String customerNameFilter, OrderStatus statusFilter);

    boolean acceptOrderForRestaurant(int restaurantId, String orderId);

    List<Order> getOrdersForDeliveryPerson(String username);

    boolean markOrderAsDelivered(String username, String orderId);

    List<Restaurant> getRestaurants();

    List<MenuItem> getMenuItemsByRestaurant(int restaurantId);
}
