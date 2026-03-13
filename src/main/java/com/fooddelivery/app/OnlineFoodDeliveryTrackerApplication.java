package com.fooddelivery.app;

import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.Role;
import com.fooddelivery.model.User;
import com.fooddelivery.repo.MenuItemRepository;
import com.fooddelivery.repo.OrderRepository;
import com.fooddelivery.repo.RestaurantRepository;
import com.fooddelivery.repo.UserRepository;
import com.fooddelivery.service.OrderService;
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
                               UserRepository userRepository,
                               OrderRepository orderRepository,
                               OrderService orderService) {
        return args -> {
            if (restaurantRepository.count() > 0) {
                return;
            }

            Restaurant spiceHub = restaurantRepository.save(new Restaurant("Spice Hub"));
            Restaurant pizzaPoint = restaurantRepository.save(new Restaurant("Pizza Point"));

            MenuItem paneerBowl = menuItemRepository.save(new MenuItem("Paneer Bowl", 180.0, spiceHub));
            MenuItem vegBiryani = menuItemRepository.save(new MenuItem("Veg Biryani", 220.0, spiceHub));
            MenuItem margheritaPizza = menuItemRepository.save(new MenuItem("Margherita Pizza", 250.0, pizzaPoint));

            User customer = userRepository.save(new User("customer1", "pass", Role.CUSTOMER, null));
            userRepository.save(new User("owner_spice", "pass", Role.RESTAURANT_OWNER, spiceHub));
            userRepository.save(new User("owner_pizza", "pass", Role.RESTAURANT_OWNER, pizzaPoint));
            userRepository.save(new User("delivery1", "pass", Role.DELIVERY_PERSON, null));

            Order first = orderService.placeOrder(customer, spiceHub, paneerBowl, 2, "MG Road");
            Order second = orderService.placeOrder(customer, spiceHub, vegBiryani, 1, "MG Road");
            Order third = orderService.placeOrder(customer, pizzaPoint, margheritaPizza, 3, "Whitefield");

            first.setStatus(OrderStatus.DELIVERED);
            second.setStatus(OrderStatus.OUT_FOR_DELIVERY);
            third.setStatus(OrderStatus.PREPARING);
            orderRepository.save(first);
            orderRepository.save(second);
            orderRepository.save(third);
        };
    }
}
