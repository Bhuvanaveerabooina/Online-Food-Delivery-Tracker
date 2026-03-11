package com.fooddelivery.service;

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
        return userRepository.findByUsername(username)
                .filter(user -> user.getPassword().equals(password) && user.getRole() == role);
    }
}
