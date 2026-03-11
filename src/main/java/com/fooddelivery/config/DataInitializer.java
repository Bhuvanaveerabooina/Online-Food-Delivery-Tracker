package com.fooddelivery.config;

import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.Role;
import com.fooddelivery.model.User;
import com.fooddelivery.repository.MenuItemRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               RestaurantRepository restaurantRepository,
                               MenuItemRepository menuItemRepository) {
        return args -> {
            if (restaurantRepository.count() > 0) {
                return;
            }

            Restaurant spiceHub = restaurantRepository.save(new Restaurant("Spice Hub"));
            Restaurant pizzaPalace = restaurantRepository.save(new Restaurant("Pizza Palace"));

            menuItemRepository.save(new MenuItem("Paneer Biryani", new BigDecimal("180.00"), spiceHub));
            menuItemRepository.save(new MenuItem("Veg Thali", new BigDecimal("140.00"), spiceHub));
            menuItemRepository.save(new MenuItem("Margherita Pizza", new BigDecimal("220.00"), pizzaPalace));
            menuItemRepository.save(new MenuItem("Farmhouse Pizza", new BigDecimal("300.00"), pizzaPalace));

            userRepository.save(new User("customer1", "cust123", Role.CUSTOMER, null));
            userRepository.save(new User("owner_spice", "owner123", Role.RESTAURANT_OWNER, spiceHub));
            userRepository.save(new User("delivery1", "del123", Role.DELIVERY_PERSON, null));
        };
    }
}
