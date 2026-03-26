package com.fooddelivery.service;

import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.util.OrderIdGenerator;

import java.time.LocalDateTime;
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
    private final List<Restaurant> restaurants = new ArrayList<>();
    private final List<MenuItem> menuItems = new ArrayList<>();
    private final OrderFileStore orderFileStore = new OrderFileStore();

    public OrderService() {
        seedRestaurantAndMenuData();
        List<Order> savedOrders = orderFileStore.loadOrders();
        orders.addAll(savedOrders);

        for (Order savedOrder : savedOrders) {
            OrderIdGenerator.syncCounterFromExistingId(savedOrder.getOrderId());
        }

        seedOrdersIfNeeded();
        resumePendingOrderProgression();
    }

    @Override
    public synchronized Order placeOrder(String username, String customerName, int restaurantId, int itemId, int quantity, String deliveryAddress) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required.");
        }
        if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery address is required.");
        }

        Restaurant restaurant = getRestaurantById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant selected."));

        MenuItem item = getMenuItemById(itemId)
                .filter(menuItem -> menuItem.getRestaurantId() == restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid menu item selected."));

        double totalPrice = item.getPrice() * quantity;

        Order order = new Order(
                OrderIdGenerator.generateOrderId(),
                customerName.trim(),
                username,
                restaurantId,
                restaurant.getRestaurantName(),
                itemId,
                item.getItemName(),
                item.getPrice(),
                quantity,
                totalPrice,
                deliveryAddress.trim(),
                OrderStatus.PLACED,
                null,
                LocalDateTime.now()
        );

        orders.add(order);
        persistOrders();
        return order;
    }

    @Override
    public synchronized Optional<Order> findCustomerOrderById(String username, String orderId) {
        return orders.stream()
                .filter(order -> order.getCustomerUserId().equalsIgnoreCase(username))
                .filter(order -> order.getOrderId().equalsIgnoreCase(orderId))
                .findFirst();
    }

    @Override
    public synchronized List<Order> getCustomerOrderHistory(String username) {
        return orders.stream()
                .filter(order -> order.getCustomerUserId().equalsIgnoreCase(username))
                .sorted(Comparator.comparing(Order::getOrderTime).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public synchronized List<Order> getOrdersForRestaurant(int restaurantId, String customerNameFilter, OrderStatus statusFilter) {
        String search = customerNameFilter == null ? "" : customerNameFilter.trim().toLowerCase();
        return orders.stream()
                .filter(order -> order.getRestaurantId() == restaurantId)
                .filter(order -> search.isEmpty() || order.getCustomerName().toLowerCase().contains(search))
                .filter(order -> statusFilter == null || order.getStatus() == statusFilter)
                .sorted(Comparator.comparing(Order::getOrderTime).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public synchronized boolean acceptOrderForRestaurant(int restaurantId, String orderId) {
        Optional<Order> orderOpt = orders.stream()
                .filter(order -> order.getRestaurantId() == restaurantId)
                .filter(order -> order.getOrderId().equalsIgnoreCase(orderId))
                .filter(order -> order.getStatus() == OrderStatus.PLACED)
                .findFirst();

        if (orderOpt.isEmpty()) {
            return false;
        }

        Order order = orderOpt.get();
        order.setStatus(OrderStatus.PREPARING);
        persistOrders();
        startOrderProgression(order);
        return true;
    }

    @Override
    public synchronized List<Order> getOrdersForDeliveryPerson(String username) {
        return orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.OUT_FOR_DELIVERY)
                .filter(order -> order.getAssignedDeliveryPersonId() == null
                        || order.getAssignedDeliveryPersonId().equalsIgnoreCase(username)
                        || order.getAssignedDeliveryPersonId().equalsIgnoreCase("delivery"))
                .sorted(Comparator.comparing(Order::getOrderTime).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public synchronized boolean markOrderAsDelivered(String username, String orderId) {
        Optional<Order> orderOpt = orders.stream()
                .filter(order -> order.getOrderId().equalsIgnoreCase(orderId))
                .filter(order -> order.getStatus() == OrderStatus.OUT_FOR_DELIVERY)
                .filter(order -> order.getAssignedDeliveryPersonId() == null
                        || order.getAssignedDeliveryPersonId().equalsIgnoreCase(username)
                        || order.getAssignedDeliveryPersonId().equalsIgnoreCase("delivery"))
                .findFirst();

        if (orderOpt.isEmpty()) {
            return false;
        }

        Order order = orderOpt.get();
        order.setAssignedDeliveryPersonId(username);
        order.setStatus(OrderStatus.DELIVERED);
        persistOrders();
        return true;
    }

    @Override
    public synchronized List<Restaurant> getRestaurants() {
        return new ArrayList<>(restaurants);
    }

    @Override
    public synchronized List<MenuItem> getMenuItemsByRestaurant(int restaurantId) {
        return menuItems.stream()
                .filter(item -> item.getRestaurantId() == restaurantId)
                .collect(Collectors.toList());
    }

    private Optional<Restaurant> getRestaurantById(int restaurantId) {
        return restaurants.stream().filter(restaurant -> restaurant.getId() == restaurantId).findFirst();
    }

    private Optional<MenuItem> getMenuItemById(int itemId) {
        return menuItems.stream().filter(item -> item.getId() == itemId).findFirst();
    }

    private void seedRestaurantAndMenuData() {
        restaurants.add(new Restaurant(1, "Spice Garden"));
        restaurants.add(new Restaurant(2, "Urban Pizza"));

        menuItems.add(new MenuItem(101, 1, "Paneer Biryani", 180));
        menuItems.add(new MenuItem(102, 1, "Veg Thali", 140));
        menuItems.add(new MenuItem(201, 2, "Margherita Pizza", 220));
        menuItems.add(new MenuItem(202, 2, "Pasta Alfredo", 190));
    }

    private void seedOrdersIfNeeded() {
        if (!orders.isEmpty()) {
            return;
        }

        orders.add(new Order(OrderIdGenerator.generateOrderId(), "Rahul", "customer", 1, "Spice Garden", 101,
                "Paneer Biryani", 180, 2, 360, "MG Road, Bengaluru", OrderStatus.PLACED, null,
                LocalDateTime.now().minusHours(3)));
        orders.add(new Order(OrderIdGenerator.generateOrderId(), "Rahul", "customer", 1, "Spice Garden", 102,
                "Veg Thali", 140, 1, 140, "MG Road, Bengaluru", OrderStatus.PREPARING, null,
                LocalDateTime.now().minusHours(2)));
        orders.add(new Order(OrderIdGenerator.generateOrderId(), "Rahul", "customer", 2, "Urban Pizza", 201,
                "Margherita Pizza", 220, 1, 220, "MG Road, Bengaluru", OrderStatus.OUT_FOR_DELIVERY, "delivery",
                LocalDateTime.now().minusHours(1)));
        persistOrders();
    }

    private synchronized void persistOrders() {
        orderFileStore.saveOrders(orders);
    }

    private void resumePendingOrderProgression() {
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.PREPARING) {
                startOrderProgression(order);
            }
        }
    }

    private void startOrderProgression(Order order) {
        Thread orderProgressThread = new Thread(() -> {
            try {
                if (order.getStatus() == OrderStatus.PREPARING) {
                    updateStatusAfterDelay(order, OrderStatus.OUT_FOR_DELIVERY, 2000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        orderProgressThread.setDaemon(true);
        orderProgressThread.start();
    }

    private void updateStatusAfterDelay(Order order, OrderStatus status, long delayMs) throws InterruptedException {
        Thread.sleep(delayMs);
        synchronized (this) {
            if (order.getStatus() == OrderStatus.DELIVERED) {
                return;
            }
            order.setStatus(status);
            if (status == OrderStatus.OUT_FOR_DELIVERY && order.getAssignedDeliveryPersonId() == null) {
                order.setAssignedDeliveryPersonId("delivery");
            }
            persistOrders();
        }
    }
}
