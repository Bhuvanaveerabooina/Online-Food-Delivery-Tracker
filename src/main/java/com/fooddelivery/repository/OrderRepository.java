package com.fooddelivery.repository;

import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerOrderByCreatedAtDesc(User customer);

    List<Order> findByRestaurantOrderByCreatedAtDesc(Restaurant restaurant);

    List<Order> findByStatusInOrderByCreatedAtDesc(Collection<OrderStatus> statuses);
}
