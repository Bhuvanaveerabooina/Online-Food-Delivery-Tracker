package com.fooddelivery.app;

import com.fooddelivery.model.*;
import com.fooddelivery.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "com.fooddelivery")
public class OnlineFoodDeliveryTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(OnlineFoodDeliveryTrackerApplication.class, args);
    }

    @Bean
    CommandLineRunner seedData(RestaurantRepository restaurantRepository,
                               MenuItemRepository menuItemRepository,
                               UserRepository userRepository) {
        return args -> {
            if (restaurantRepository.count() > 0) {
                return;
            }

            Restaurant spiceHub = restaurantRepository.save(new Restaurant("Spice Hub"));
            Restaurant pizzaPoint = restaurantRepository.save(new Restaurant("Pizza Point"));

            menuItemRepository.save(new MenuItem("Paneer Bowl", 180.0, spiceHub));
            menuItemRepository.save(new MenuItem("Veg Biryani", 220.0, spiceHub));
            menuItemRepository.save(new MenuItem("Margherita Pizza", 250.0, pizzaPoint));
            menuItemRepository.save(new MenuItem("Farmhouse Pizza", 320.0, pizzaPoint));

            userRepository.save(new User("customer1", "pass", Role.CUSTOMER, null));
            userRepository.save(new User("owner_spice", "pass", Role.RESTAURANT_OWNER, spiceHub));
            userRepository.save(new User("owner_pizza", "pass", Role.RESTAURANT_OWNER, pizzaPoint));
            userRepository.save(new User("delivery1", "pass", Role.DELIVERY_PERSON, null));
        };
    }
}
