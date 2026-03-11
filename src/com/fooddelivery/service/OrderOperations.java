package com.fooddelivery.service;

import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;

import java.util.List;

public interface OrderOperations {
    Order placeOrder(String customerUsername, String customerName, String restaurantName, String itemName,
                     double itemPrice, int quantity, String address);

    List<Order> getOrdersForCustomer(String customerUsername);

    List<Order> getOrdersForRestaurant(String restaurantName);

    List<Order> searchOrdersForRestaurant(String restaurantName, String query);

    List<Order> getOrdersForDeliveryPerson(String deliveryUsername);

    boolean updateRestaurantOrderStatus(String restaurantName, String orderId, OrderStatus newStatus);

    boolean markOutForDelivery(String deliveryUsername, String orderId);

    boolean markDelivered(String deliveryUsername, String orderId);
}
