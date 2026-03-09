package com.fooddelivery.service;

import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.util.OrderIdGenerator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handles all order-related business logic.
 */
public class OrderService implements OrderOperations {
    private final List<Order> orders = new ArrayList<>();

    @Override
    public synchronized Order placeOrder(String customerName, String itemName, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }

        Order order = new Order(OrderIdGenerator.generateOrderId(), customerName, itemName, quantity);
        orders.add(order);
        startDeliverySimulation(order);
        return order;
    }

    @Override
    public synchronized Optional<Order> findOrderById(String orderId) {
        return orders.stream()
                .filter(order -> order.getOrderId().equalsIgnoreCase(orderId))
                .findFirst();
    }

    @Override
    public synchronized List<Order> getOrderHistory() {
        return orders.stream()
                .sorted(Comparator.comparing(Order::getOrderTime).reversed())
                .collect(Collectors.toList());
    }

    private void startDeliverySimulation(Order order) {
        Thread deliveryThread = new Thread(() -> {
            try {
                Thread.sleep(2000);
                order.setStatus(OrderStatus.PREPARING);

                Thread.sleep(2000);
                order.setStatus(OrderStatus.OUT_FOR_DELIVERY);

                Thread.sleep(2000);
                order.setStatus(OrderStatus.DELIVERED);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Delivery status simulation interrupted for order " + order.getOrderId());
            }
        });

        deliveryThread.setDaemon(true);
        deliveryThread.start();
    }
}
