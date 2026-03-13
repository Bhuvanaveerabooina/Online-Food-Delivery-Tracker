package com.fooddelivery.service;

import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.User;
import com.fooddelivery.repo.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order placeOrder(User customer, Restaurant restaurant, MenuItem menuItem, int quantity, String address) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Delivery address is required");
        }

        return orderRepository.save(new Order(
                "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                customer,
                restaurant,
                menuItem,
                quantity,
                menuItem.getPrice(),
                address.trim(),
                OrderStatus.PLACED
        ));
    }

    public List<Order> customerOrders(User customer) {
        return orderRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    public List<Order> restaurantOrders(Restaurant restaurant) {
        return orderRepository.findByRestaurantOrderByCreatedAtDesc(restaurant);
    }

    public List<Order> deliveryOrders() {
        return orderRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY)
        );
    }

    public void updateStatus(Order order, OrderStatus status) {
        order.setStatus(status);
        orderRepository.save(order);
    }

    public Order byOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }
}
