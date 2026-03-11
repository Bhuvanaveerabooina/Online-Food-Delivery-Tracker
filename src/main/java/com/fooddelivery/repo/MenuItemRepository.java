package com.fooddelivery.repo;

import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Restaurant;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MenuItemRepository {
    private final List<MenuItem> items = new ArrayList<>();
    private final AtomicLong idGen = new AtomicLong(1);

    public MenuItem save(String name, double price, Restaurant restaurant) {
        MenuItem menuItem = new MenuItem(idGen.getAndIncrement(), name, price, restaurant);
        items.add(menuItem);
        return menuItem;
    }

    public List<MenuItem> findByRestaurant(Restaurant restaurant) {
        return items.stream().filter(i -> i.getRestaurant().getId().equals(restaurant.getId())).toList();
    }
}
