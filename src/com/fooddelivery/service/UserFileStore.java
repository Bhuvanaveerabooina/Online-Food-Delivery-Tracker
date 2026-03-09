package com.fooddelivery.service;

import com.fooddelivery.model.UserAccount;

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

public class UserFileStore {
    private static final Path STORAGE_DIRECTORY = Paths.get(System.getProperty("user.home"), ".online-food-delivery-tracker");
    private static final Path USERS_FILE = STORAGE_DIRECTORY.resolve("users-data.ser");

    public void saveUsers(List<UserAccount> users) {
        try {
            Files.createDirectories(STORAGE_DIRECTORY);
            try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(USERS_FILE.toFile()))) {
                outputStream.writeObject(users);
            }
        } catch (IOException e) {
            System.out.println("Could not save users: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<UserAccount> loadUsers() {
        if (!Files.exists(USERS_FILE)) {
            return new ArrayList<>();
        }

        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(USERS_FILE.toFile()))) {
            Object data = inputStream.readObject();
            if (data instanceof List<?>) {
                return (List<UserAccount>) data;
            }
        } catch (IOException | ClassNotFoundException ignored) {
        }
        return new ArrayList<>();
    }
}
