package com.fooddelivery.service;

import com.fooddelivery.model.*;
import com.fooddelivery.repo.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository foodOrderRepository;

    public OrderService(OrderRepository foodOrderRepository) {
        this.foodOrderRepository = foodOrderRepository;
    }

    public Order placeOrder(User customer, Restaurant restaurant, MenuItem menuItem, int quantity, String address) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        Order order = new Order(
                "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                customer,
                restaurant,
                menuItem,
                quantity,
                menuItem.getPrice(),
                address,
                OrderStatus.PLACED
        );
        return foodOrderRepository.save(order);
    }

    public List<Order> customerOrders(User customer) {
        return foodOrderRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    public List<Order> restaurantOrders(Restaurant restaurant) {
        return foodOrderRepository.findByRestaurantOrderByCreatedAtDesc(restaurant);
    }

    public List<Order> deliveryOrders() {
        return foodOrderRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY)
        );
    }

    public void updateStatus(Order order, OrderStatus status) {
        order.setStatus(status);
        foodOrderRepository.save(order);
    }

    public Order byOrderId(String orderId) {
        return foodOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }
}
