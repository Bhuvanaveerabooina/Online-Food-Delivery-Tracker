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
    public synchronized Order placeOrder(String customerUsername, String customerName, String restaurantName,
                                         String itemName, double itemPrice, int quantity, String address) {
        validateOrderInput(customerUsername, customerName, restaurantName, itemName, itemPrice, quantity, address);

        Order order = new Order(OrderIdGenerator.generateOrderId(), customerUsername.trim(), customerName.trim(),
                restaurantName.trim(), itemName.trim(), itemPrice, quantity, address.trim());
        orders.add(order);
        persistOrders();
        return order;
    }

    @Override
    public synchronized List<Order> getOrdersForCustomer(String customerUsername) {
        return orders.stream()
                .filter(order -> order.getCustomerUsername().equalsIgnoreCase(customerUsername))
                .sorted(Comparator.comparing(Order::getOrderTime).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Order> getOrdersForRestaurant(String restaurantName) {
        return orders.stream()
                .filter(order -> order.getRestaurantName().equalsIgnoreCase(restaurantName))
                .sorted(Comparator.comparing(Order::getOrderTime).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Order> searchOrdersForRestaurant(String restaurantName, String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ENGLISH);
        return getOrdersForRestaurant(restaurantName).stream()
                .filter(order -> normalized.isEmpty()
                        || order.getOrderId().toLowerCase(Locale.ENGLISH).contains(normalized)
                        || order.getCustomerName().toLowerCase(Locale.ENGLISH).contains(normalized))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Order> getOrdersForDeliveryPerson(String deliveryUsername) {
        return orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.READY_FOR_PICKUP
                        || order.getStatus() == OrderStatus.OUT_FOR_DELIVERY)
                .filter(order -> order.getAssignedDeliveryUsername() == null
                        || order.getAssignedDeliveryUsername().equalsIgnoreCase(deliveryUsername))
                .sorted(Comparator.comparing(Order::getOrderTime).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public synchronized boolean updateRestaurantOrderStatus(String restaurantName, String orderId, OrderStatus newStatus) {
        Optional<Order> orderOpt = findOrderForRestaurant(restaurantName, orderId);
        if (!orderOpt.isPresent()) {
            return false;
        }

        Order order = orderOpt.get();
        if (!isValidRestaurantTransition(order.getStatus(), newStatus)) {
            return false;
        }

        order.setStatus(newStatus);
        persistOrders();
        return true;
    }

    @Override
    public synchronized boolean markOutForDelivery(String deliveryUsername, String orderId) {
        Optional<Order> orderOpt = findOrder(orderId);
        if (!orderOpt.isPresent()) {
            return false;
        }
        Order order = orderOpt.get();
        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            return false;
        }

        order.setAssignedDeliveryUsername(deliveryUsername);
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        persistOrders();
        return true;
    }

    @Override
    public synchronized boolean markDelivered(String deliveryUsername, String orderId) {
        Optional<Order> orderOpt = findOrder(orderId);
        if (!orderOpt.isPresent()) {
            return false;
        }

        Order order = orderOpt.get();
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            return false;
        }

        if (order.getAssignedDeliveryUsername() != null
                && !order.getAssignedDeliveryUsername().equalsIgnoreCase(deliveryUsername)) {
            return false;
        }

        order.setAssignedDeliveryUsername(deliveryUsername);
        order.setStatus(OrderStatus.DELIVERED);
        persistOrders();
        return true;
    }

    private Optional<Order> findOrderForRestaurant(String restaurantName, String orderId) {
        return orders.stream()
                .filter(order -> order.getRestaurantName().equalsIgnoreCase(restaurantName))
                .filter(order -> order.getOrderId().equalsIgnoreCase(orderId))
                .findFirst();
    }

    private Optional<Order> findOrder(String orderId) {
        return orders.stream()
                .filter(order -> order.getOrderId().equalsIgnoreCase(orderId))
                .findFirst();
    }

    private boolean isValidRestaurantTransition(OrderStatus current, OrderStatus next) {
        return (current == OrderStatus.PLACED && next == OrderStatus.PREPARING)
                || (current == OrderStatus.PREPARING && next == OrderStatus.READY_FOR_PICKUP);
    }

    private void validateOrderInput(String customerUsername, String customerName, String restaurantName,
                                    String itemName, double itemPrice, int quantity, String address) {
        if (customerUsername == null || customerUsername.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid user session.");
        }
        if (customerName == null || customerName.trim().length() < 2) {
            throw new IllegalArgumentException("Customer name must be at least 2 characters.");
        }
        if (restaurantName == null || restaurantName.trim().isEmpty()) {
            throw new IllegalArgumentException("Please select a restaurant.");
        }
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new IllegalArgumentException("Please select an item.");
        }
        if (itemPrice <= 0) {
            throw new IllegalArgumentException("Invalid item price.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }
        if (address == null || address.trim().length() < 5) {
            throw new IllegalArgumentException("Delivery address must be at least 5 characters.");
        }
    }

    private synchronized void persistOrders() {
        orderFileStore.saveOrders(orders);
    }
}
