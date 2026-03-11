package com.fooddelivery.service;

import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Restaurant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RestaurantService {
    private final RestaurantFileStore restaurantFileStore = new RestaurantFileStore();
    private final List<Restaurant> restaurants = new ArrayList<>();

    public RestaurantService() {
        restaurants.addAll(restaurantFileStore.loadRestaurants());
        if (restaurants.isEmpty()) {
            seedDefaultRestaurants();
            restaurantFileStore.saveRestaurants(restaurants);
        }
    }

    public List<Restaurant> getAllRestaurants() {
        return new ArrayList<>(restaurants);
    }

    public Optional<Restaurant> findByName(String restaurantName) {
        return restaurants.stream()
                .filter(restaurant -> restaurant.getName().equalsIgnoreCase(restaurantName))
                .findFirst();
    }

    private void seedDefaultRestaurants() {
        restaurants.add(new Restaurant("Spice Hub", Arrays.asList(
                new MenuItem("Veg Biryani", 120),
                new MenuItem("Paneer Butter Masala", 180),
                new MenuItem("Naan", 30)
        )));

        restaurants.add(new Restaurant("Burger Barn", Arrays.asList(
                new MenuItem("Classic Burger", 90),
                new MenuItem("Cheese Burger", 110),
                new MenuItem("French Fries", 60)
        )));

        restaurants.add(new Restaurant("Pizza Corner", Arrays.asList(
                new MenuItem("Margherita Pizza", 200),
                new MenuItem("Farmhouse Pizza", 250),
                new MenuItem("Garlic Bread", 80)
        )));
    }
}
