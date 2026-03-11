package com.fooddelivery.repo;

import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerOrderByCreatedAtDesc(User customer);
    List<Order> findByRestaurantOrderByCreatedAtDesc(Restaurant restaurant);
    List<Order> findByStatusInOrderByCreatedAtDesc(List<OrderStatus> statuses);
    Optional<Order> findByOrderId(String orderId);
}
