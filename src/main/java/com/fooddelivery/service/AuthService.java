package com.fooddelivery.service;

import com.fooddelivery.model.Role;
import com.fooddelivery.model.User;
import com.fooddelivery.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User login(String username, String password, Role role) {
        return userRepository.findByUsername(username)
                .filter(user -> user.getPassword().equals(password) && user.getRole() == role)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username, password, or role."));
    }
}
