package com.fooddelivery.repo;

import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.Role;
import com.fooddelivery.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class UserRepository {
    private final List<User> users = new ArrayList<>();
    private final AtomicLong idGen = new AtomicLong(1);

    public User save(String username, String password, Role role, Restaurant restaurant) {
        User user = new User(idGen.getAndIncrement(), username, password, role, restaurant);
        users.add(user);
        return user;
    }

    public Optional<User> findByUsername(String username) {
        return users.stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst();
    }
}
