package com.fooddelivery.service;

import com.fooddelivery.model.Restaurant;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RestaurantFileStore {
    private static final Path STORAGE_DIRECTORY = Paths.get(System.getProperty("user.home"), ".online-food-delivery-tracker");
    private static final Path RESTAURANTS_FILE = STORAGE_DIRECTORY.resolve("restaurants-data.ser");

    public void saveRestaurants(List<Restaurant> restaurants) {
        try {
            Files.createDirectories(STORAGE_DIRECTORY);
            try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(RESTAURANTS_FILE.toFile()))) {
                outputStream.writeObject(restaurants);
            }
        } catch (IOException e) {
            System.out.println("Could not save restaurants: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Restaurant> loadRestaurants() {
        if (!Files.exists(RESTAURANTS_FILE)) {
            return new ArrayList<>();
        }

        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(RESTAURANTS_FILE.toFile()))) {
            Object data = inputStream.readObject();
            if (data instanceof List<?>) {
                return (List<Restaurant>) data;
            }
        } catch (IOException | ClassNotFoundException ignored) {
        }
        return new ArrayList<>();
    }
}
