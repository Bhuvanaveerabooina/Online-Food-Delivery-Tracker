package com.fooddelivery.service;

import com.fooddelivery.model.Order;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles saving/loading order history to a local file.
 *
 * Uses Java serialization to keep implementation simple for beginners.
 */
public class OrderFileStore {
    // File created in project/app working directory.
    private static final String ORDERS_FILE = "orders-data.ser";

    /**
     * Saves complete order list to disk.
     */
    public void saveOrders(List<Order> orders) {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(ORDERS_FILE))) {
            outputStream.writeObject(orders);
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
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(ORDERS_FILE))) {
            Object data = inputStream.readObject();

            if (data instanceof List<?>) {
                return (List<Order>) data;
            }
        } catch (IOException | ClassNotFoundException e) {
            // First run or unreadable file: silently start with empty list.
        }

        return new ArrayList<>();
    }
}
