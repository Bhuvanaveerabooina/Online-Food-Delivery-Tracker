package com.fooddelivery.service;

import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.Role;
import com.fooddelivery.model.User;
import com.fooddelivery.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> authenticate(String username, String password, Role role) {
        return userRepository.findByUsernameIgnoreCase(username)
                .filter(user -> user.getPassword().equals(password) && user.getRole() == role);
    }

    public User register(String username, String password, Role role, Restaurant restaurant) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (userRepository.existsByUsernameIgnoreCase(username.trim())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (role == Role.RESTAURANT_OWNER && restaurant == null) {
            throw new IllegalArgumentException("Restaurant owner must have a restaurant");
        }

        return userRepository.save(new User(username.trim(), password.trim(), role, restaurant));
    }
}
