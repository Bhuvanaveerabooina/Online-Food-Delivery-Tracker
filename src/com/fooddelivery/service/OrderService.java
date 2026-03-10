package com.fooddelivery.service;

import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.util.OrderIdGenerator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Handles all order-related business logic.
 */
public class OrderService implements OrderOperations {
    private final List<Order> orders = new ArrayList<>();
    private final OrderFileStore orderFileStore = new OrderFileStore();
    private final ScheduledExecutorService statusScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "order-status-updater");
        thread.setDaemon(true);
        return thread;
    });

    public OrderService() {
        List<Order> savedOrders = orderFileStore.loadOrders();
        orders.addAll(savedOrders);
        savedOrders.forEach(savedOrder -> OrderIdGenerator.syncCounterFromExistingId(savedOrder.getOrderId()));

        // Align statuses after loading existing orders from disk.
        refreshStatusesFromElapsedTime();
        statusScheduler.scheduleAtFixedRate(this::refreshStatusesFromElapsedTime, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public synchronized Order placeOrder(String username, String customerName, String itemName, int quantity) {
        validateOrderInput(username, customerName, itemName, quantity);

        Order order = new Order(OrderIdGenerator.generateOrderId(), username.trim(), customerName.trim(), itemName.trim(), quantity);
        orders.add(order);
        persistOrders();
        return order;
    }

    @Override
    public synchronized Optional<Order> findOrderById(String username, String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return Optional.empty();
        }

        return orders.stream()
                .filter(order -> order.getPlacedByUsername() != null)
                .filter(order -> order.getPlacedByUsername().equalsIgnoreCase(username))
                .filter(order -> order.getOrderId().equalsIgnoreCase(orderId.trim()))
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

    @Override
    public synchronized boolean markOrderAsDelivered(String username, String orderId) {
        Optional<Order> orderOpt = findOrderById(username, orderId);
        if (orderOpt.isEmpty()) {
            return false;
        }

        Order order = orderOpt.get();
        if (order.getStatus() == OrderStatus.DELIVERED) {
            return false;
        }

        order.setStatus(OrderStatus.DELIVERED);
        persistOrders();
        return true;
    }

    public synchronized void refreshStatusesFromElapsedTime() {
        boolean changed = false;
        for (Order order : orders) {
            OrderStatus expectedStatus = calculateStatus(order.getOrderTime());
            if (order.getStatus() != expectedStatus) {
                order.setStatus(expectedStatus);
                changed = true;
            }
        }

        if (changed) {
            persistOrders();
        }
    }

    private OrderStatus calculateStatus(LocalDateTime orderTime) {
        long secondsElapsed = Duration.between(orderTime, LocalDateTime.now()).getSeconds();
        if (secondsElapsed >= 18) {
            return OrderStatus.DELIVERED;
        }
        if (secondsElapsed >= 12) {
            return OrderStatus.OUT_FOR_DELIVERY;
        }
        if (secondsElapsed >= 6) {
            return OrderStatus.PREPARING;
        }
        return OrderStatus.PLACED;
    }

    private void validateOrderInput(String username, String customerName, String itemName, int quantity) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid user session.");
        }
        if (customerName == null || customerName.trim().length() < 2) {
            throw new IllegalArgumentException("Customer name must be at least 2 characters.");
        }
        if (itemName == null || itemName.trim().length() < 2) {
            throw new IllegalArgumentException("Item name must be at least 2 characters.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }
    }

    private synchronized void persistOrders() {
        orderFileStore.saveOrders(orders);
    }
}
