package com.fooddelivery.service;

import com.fooddelivery.model.*;
import com.fooddelivery.repository.MenuItemRepository;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public OrderService(OrderRepository orderRepository,
                        RestaurantRepository restaurantRepository,
                        MenuItemRepository menuItemRepository) {
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
    }

    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    public List<MenuItem> getMenuItemsByRestaurant(Restaurant restaurant) {
        return menuItemRepository.findByRestaurant(restaurant);
    }

    @Transactional
    public Order placeOrder(User customer, Restaurant restaurant, MenuItem menuItem, int quantity, String address) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery address is required.");
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setMenuItem(menuItem);
        order.setQuantity(quantity);
        order.setDeliveryAddress(address.trim());
        order.setStatus(OrderStatus.PLACED);
        order.setCreatedAt(LocalDateTime.now());
        BigDecimal total = menuItem.getPrice().multiply(BigDecimal.valueOf(quantity));
        order.setTotalPrice(total);

        return orderRepository.save(order);
    }

    public List<Order> getOrdersForCustomer(User customer) {
        return orderRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    public List<Order> getOrdersForRestaurantOwner(User owner) {
        return orderRepository.findByRestaurantOrderByCreatedAtDesc(owner.getRestaurant());
    }

    public List<Order> getDeliveryOrders() {
        return orderRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY));
    }

    @Transactional
    public void updateStatus(Order order, OrderStatus newStatus) {
        validateTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        boolean valid = (current == OrderStatus.PLACED && next == OrderStatus.PREPARING)
                || (current == OrderStatus.PREPARING && next == OrderStatus.READY_FOR_PICKUP)
                || (current == OrderStatus.READY_FOR_PICKUP && next == OrderStatus.OUT_FOR_DELIVERY)
                || (current == OrderStatus.OUT_FOR_DELIVERY && next == OrderStatus.DELIVERED);
        if (!valid) {
            throw new IllegalArgumentException("Invalid status update.");
        }
    }
}
