package com.fooddelivery.repo;

import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class OrderRepository {
    private final List<Order> orders = new ArrayList<>();
    private final AtomicLong idGen = new AtomicLong(1);

    public Order save(Order order) {
        if (order.getId() == null) {
            Order fresh = new Order(
                    idGen.getAndIncrement(),
                    order.getOrderId(),
                    order.getCustomer(),
                    order.getRestaurant(),
                    order.getMenuItem(),
                    order.getQuantity(),
                    order.getItemPrice(),
                    order.getDeliveryAddress(),
                    order.getStatus()
            );
            orders.add(fresh);
            return fresh;
        }
        if (orders.stream().noneMatch(existing -> existing.getId().equals(order.getId()))) {
            orders.add(order);
            idGen.updateAndGet(current -> Math.max(current, order.getId() + 1));
        }
        return order;
    }

    public Order saveExisting(Order order) {
        orders.add(order);
        idGen.updateAndGet(current -> Math.max(current, order.getId() + 1));
        return order;
    }

    public List<Order> findByCustomerOrderByCreatedAtDesc(User customer) {
        return orders.stream()
                .filter(o -> o.getCustomer().getId().equals(customer.getId()))
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .toList();
    }

    public List<Order> findByRestaurantOrderByCreatedAtDesc(Restaurant restaurant) {
        return orders.stream()
                .filter(o -> o.getRestaurant().getId().equals(restaurant.getId()))
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .toList();
    }

    public List<Order> findByStatusInOrderByCreatedAtDesc(List<OrderStatus> statuses) {
        return orders.stream()
                .filter(o -> statuses.contains(o.getStatus()))
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .toList();
    }

    public Optional<Order> findByOrderId(String orderId) {
        return orders.stream().filter(o -> o.getOrderId().equalsIgnoreCase(orderId)).findFirst();
    }

    public List<Order> findAll() {
        return List.copyOf(orders);
    }
}
