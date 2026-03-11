package com.fooddelivery.service;

import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.util.OrderIdGenerator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
        return placeOrder(username, customerName, "", "", itemName, 0, quantity);
    }

    public synchronized Order placeOrder(String username, String customerName, String customerAddress,
                                         String restaurantName, String itemName, double price, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }
        if (restaurantName == null || restaurantName.trim().isEmpty()) {
            throw new IllegalArgumentException("Restaurant name is required.");
        }
        if (customerAddress == null || customerAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer address is required.");
        }

        Order order = new Order(
                OrderIdGenerator.generateOrderId(),
                username,
                customerName,
                customerAddress,
                restaurantName,
                itemName,
                price,
                quantity
        );
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

    public synchronized Optional<Order> findAnyOrderById(String orderId) {
        return orders.stream().filter(order -> order.getOrderId().equalsIgnoreCase(orderId)).findFirst();
    }

    @Override
    public synchronized List<Order> getOrderHistory(String username) {
        return orders.stream()
                .filter(order -> order.getPlacedByUsername() != null)
                .filter(order -> order.getPlacedByUsername().equalsIgnoreCase(username))
                .sorted(Comparator.comparing(Order::getOrderTime).reversed())
                .collect(Collectors.toList());
    }

    public synchronized List<Order> getAllOrders() {
        return orders.stream()
                .sorted(Comparator.comparing(Order::getOrderTime).reversed())
                .collect(Collectors.toList());
    }

    public synchronized List<Order> getOrdersByCustomerFilter(String filter) {
        String normalized = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        return orders.stream()
                .filter(order -> normalized.isEmpty() || order.getCustomerName().toLowerCase(Locale.ROOT).contains(normalized))
                .sorted(Comparator.comparing(Order::getOrderTime).reversed())
                .collect(Collectors.toList());
    }

    public synchronized boolean markOrderAsDeliveredByDeliveryPerson(String orderId) {
        Optional<Order> orderOpt = findAnyOrderById(orderId);
        if (orderOpt.isEmpty()) {
            return false;
        }

        Order order = orderOpt.get();
        if (order.getStatus() != OrderStatus.AWAITING_CUSTOMER_VERIFICATION) {
            return false;
        }

        order.setDeliveredByDeliveryPerson(true);
        order.setStatus(OrderStatus.DELIVERED);
        persistOrders();
        return true;
    }

    public synchronized boolean markOrderAsDelivered(String username, String orderId) {
        Optional<Order> orderOpt = findOrderById(username, orderId);
        if (orderOpt.isEmpty()) {
            return false;
        }

        Order order = orderOpt.get();
        if (order.getStatus() != OrderStatus.AWAITING_CUSTOMER_VERIFICATION) {
            return false;
        }

        order.setStatus(OrderStatus.DELIVERED);
        persistOrders();
        return true;
    }

    private void startDeliverySimulation(Order order) {
        Thread deliveryThread = new Thread(() -> {
            try {
                updateStatusAfterDelay(order, OrderStatus.PREPARING, 2000);
                updateStatusAfterDelay(order, OrderStatus.OUT_FOR_DELIVERY, 2000);
                updateStatusAfterDelay(order, OrderStatus.AWAITING_CUSTOMER_VERIFICATION, 2000);
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
            if (order.getStatus() != OrderStatus.DELIVERED) {
                order.setStatus(status);
                persistOrders();
            }
        }
    }

    private synchronized void persistOrders() {
        orderFileStore.saveOrders(orders);
    }
}
