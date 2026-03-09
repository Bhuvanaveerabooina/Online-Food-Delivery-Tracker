package com.fooddelivery.service;

import com.fooddelivery.model.Order;

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

/**
 * Handles saving/loading order history to a local file.
 *
 * Uses Java serialization to keep implementation simple for beginners.
 */
public class OrderFileStore {
    // Save under user home so data persists regardless of app run location.
    private static final Path STORAGE_DIRECTORY = Paths.get(System.getProperty("user.home"), ".online-food-delivery-tracker");
    private static final Path ORDERS_FILE = STORAGE_DIRECTORY.resolve("orders-data.ser");

    /**
     * Saves complete order list to disk.
     */
    public void saveOrders(List<Order> orders) {
        try {
            Files.createDirectories(STORAGE_DIRECTORY);
            try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(ORDERS_FILE.toFile()))) {
                outputStream.writeObject(orders);
            }
        } catch (IOException e) {
            // Keep app usable even if file save fails.
            System.out.println("Could not save orders: " + e.getMessage());
        }
    }

    /**
     * Loads order list from disk. Returns empty list if file is missing/invalid.
     */
    @SuppressWarnings("unchecked")
    public List<Order> loadOrders() {
        if (!Files.exists(ORDERS_FILE)) {
            return new ArrayList<>();
        }

        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(ORDERS_FILE.toFile()))) {
            Object data = inputStream.readObject();

            if (data instanceof List<?>) {
                return (List<Order>) data;
            }
        } catch (IOException | ClassNotFoundException e) {
            // Unreadable file: silently start with empty list.
        }

        return new ArrayList<>();
    }
}
