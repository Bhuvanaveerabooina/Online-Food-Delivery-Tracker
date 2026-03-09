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
    private final OrderFileStore orderFileStore = new OrderFileStore();

    public OrderService() {
        List<Order> savedOrders = orderFileStore.loadOrders();
        orders.addAll(savedOrders);

        for (Order savedOrder : savedOrders) {
            OrderIdGenerator.syncCounterFromExistingId(savedOrder.getOrderId());
        }
    }

    @Override
    public synchronized Order placeOrder(String username, String customerName, String itemName, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }

        Order order = new Order(OrderIdGenerator.generateOrderId(), username, customerName, itemName, quantity);
        orders.add(order);
        persistOrders();
        startDeliverySimulation(order);
        return order;
    }

    @Override
    public synchronized Optional<Order> findOrderById(String username, String orderId) {
        return orders.stream()
                .filter(order -> order.getPlacedByUsername() != null)
                .filter(order -> order.getPlacedByUsername().equalsIgnoreCase(username))
                .filter(order -> order.getOrderId().equalsIgnoreCase(orderId))
                .findFirst();
    }

    @Override
    public synchronized List<Order> getOrderHistory(String username) {
        return orders.stream()
                .filter(order -> order.getPlacedByUsername() != null)
                .filter(order -> order.getPlacedByUsername().equalsIgnoreCase(username))
                .sorted(Comparator.comparing(Order::getOrderTime).reversed())
                .collect(Collectors.toList());
    }

    private void startDeliverySimulation(Order order) {
        Thread deliveryThread = new Thread(() -> {
            try {
                updateStatusAfterDelay(order, OrderStatus.PREPARING, 2000);
                updateStatusAfterDelay(order, OrderStatus.OUT_FOR_DELIVERY, 2000);
                updateStatusAfterDelay(order, OrderStatus.DELIVERED, 2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        deliveryThread.setDaemon(true);
        deliveryThread.start();
    }

    private void updateStatusAfterDelay(Order order, OrderStatus status, long delayMs) throws InterruptedException {
        Thread.sleep(delayMs);

        synchronized (this) {
            order.setStatus(status);
            persistOrders();
        }
    }

    private synchronized void persistOrders() {
        orderFileStore.saveOrders(orders);
    }
}
