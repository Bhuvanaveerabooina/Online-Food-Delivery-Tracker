package com.fooddelivery.repo;

import com.fooddelivery.model.Restaurant;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class RestaurantRepository {
    private final List<Restaurant> restaurants = new ArrayList<>();
    private final AtomicLong idGen = new AtomicLong(1);

    public Restaurant save(String name) {
        Restaurant restaurant = new Restaurant(idGen.getAndIncrement(), name);
        restaurants.add(restaurant);
        return restaurant;
    }

    public List<Restaurant> findAll() {
        return List.copyOf(restaurants);
    }

    public long count() {
        return restaurants.size();
    }

    public Optional<Restaurant> findByName(String name) {
        return restaurants.stream().filter(r -> r.getName().equals(name)).findFirst();
    }
}
