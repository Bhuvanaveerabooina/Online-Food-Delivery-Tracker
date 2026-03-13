package com.fooddelivery.repo;

import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByRestaurantOrderByName(Restaurant restaurant);
}
