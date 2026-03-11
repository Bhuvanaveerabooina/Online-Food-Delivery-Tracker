package com.fooddelivery.app;

import com.fooddelivery.model.*;
import com.fooddelivery.repo.MenuItemRepository;
import com.fooddelivery.repo.OrderRepository;
import com.fooddelivery.repo.RestaurantRepository;
import com.fooddelivery.repo.UserRepository;
import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.OrderService;

import java.util.List;
import java.util.Scanner;

public class OnlineFoodDeliveryTrackerApplication {
    private final RestaurantRepository restaurantRepository = new RestaurantRepository();
    private final MenuItemRepository menuItemRepository = new MenuItemRepository();
    private final UserRepository userRepository = new UserRepository();
    private final OrderService orderService = new OrderService(new OrderRepository());
    private final AuthService authService = new AuthService(userRepository);
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new OnlineFoodDeliveryTrackerApplication().start();
    }

    private void start() {
        seedData();
        System.out.println("=== Online Food Delivery Tracker (Java Console) ===");

        while (true) {
            User user = login();
            if (user == null) {
                System.out.println("Exiting app.");
                return;
            }
            switch (user.getRole()) {
                case CUSTOMER -> customerMenu(user);
                case RESTAURANT_OWNER -> ownerMenu(user);
                case DELIVERY_PERSON -> deliveryMenu();
            }
        }
    }

    private User login() {
        while (true) {
            System.out.println("\n1) Login\n0) Exit");
            int option = readInt("Choose option: ");
            if (option == 0) {
                return null;
            }
            if (option != 1) {
                System.out.println("Invalid option.");
                continue;
            }

            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();
            Role role = chooseRole();

            if (role == null) {
                System.out.println("Invalid role.");
                continue;
            }

            User user = authService.authenticate(username, password, role).orElse(null);
            if (user != null) {
                System.out.println("Login successful. Welcome, " + user.getUsername());
                return user;
            }
            System.out.println("Invalid credentials or role.");
        }
    }

    private void customerMenu(User customer) {
        while (true) {
            System.out.println("\nCustomer Menu\n1) Place order\n2) View my orders\n0) Logout");
            int choice = readInt("Choose option: ");
            switch (choice) {
                case 1 -> placeOrder(customer);
                case 2 -> printOrders(orderService.customerOrders(customer));
                case 0 -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void placeOrder(User customer) {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        if (restaurants.isEmpty()) {
            System.out.println("No restaurants available.");
            return;
        }

        Restaurant restaurant = chooseFromList("Select restaurant", restaurants);
        if (restaurant == null) {
            return;
        }

        List<MenuItem> menuItems = menuItemRepository.findByRestaurant(restaurant);
        MenuItem item = chooseFromList("Select menu item", menuItems);
        if (item == null) {
            return;
        }

        int quantity = readInt("Quantity: ");
        System.out.print("Delivery address: ");
        String address = scanner.nextLine();

        try {
            Order order = orderService.placeOrder(customer, restaurant, item, quantity, address);
            System.out.println("Order placed: " + order.getOrderId());
        } catch (Exception ex) {
            System.out.println("Failed to place order: " + ex.getMessage());
        }
    }

    private void ownerMenu(User owner) {
        while (true) {
            System.out.println("\nRestaurant Owner Menu\n1) View restaurant orders\n2) Search orders\n3) Update order status\n0) Logout");
            int choice = readInt("Choose option: ");
            switch (choice) {
                case 1 -> printOrders(orderService.restaurantOrders(owner.getRestaurant()));
                case 2 -> searchOwnerOrders(owner);
                case 3 -> updateOwnerOrderStatus(owner);
                case 0 -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void searchOwnerOrders(User owner) {
        System.out.print("Search by customer name or order ID: ");
        String query = scanner.nextLine().trim().toLowerCase();
        List<Order> filtered = orderService.restaurantOrders(owner.getRestaurant()).stream()
                .filter(o -> query.isBlank()
                        || o.getCustomer().getUsername().toLowerCase().contains(query)
                        || o.getOrderId().toLowerCase().contains(query))
                .toList();
        printOrders(filtered);
    }

    private void updateOwnerOrderStatus(User owner) {
        List<Order> orders = orderService.restaurantOrders(owner.getRestaurant());
        Order order = chooseFromList("Select order", orders);
        if (order == null) {
            return;
        }
        List<OrderStatus> allowed = List.of(OrderStatus.PLACED, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP);
        OrderStatus status = chooseFromList("Select new status", allowed);
        if (status == null) {
            return;
        }
        orderService.updateStatus(order, status);
        System.out.println("Order status updated.");
    }

    private void deliveryMenu() {
        while (true) {
            System.out.println("\nDelivery Menu\n1) View delivery orders\n2) Update order status\n0) Logout");
            int choice = readInt("Choose option: ");
            switch (choice) {
                case 1 -> printOrders(orderService.deliveryOrders());
                case 2 -> updateDeliveryStatus();
                case 0 -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void updateDeliveryStatus() {
        List<Order> orders = orderService.deliveryOrders();
        Order order = chooseFromList("Select order", orders);
        if (order == null) {
            return;
        }
        List<OrderStatus> allowed = List.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED);
        OrderStatus status = chooseFromList("Select status", allowed);
        if (status == null) {
            return;
        }
        orderService.updateStatus(order, status);
        System.out.println("Delivery status updated.");
    }

    private <T> T chooseFromList(String title, List<T> items) {
        if (items.isEmpty()) {
            System.out.println("No entries available.");
            return null;
        }
        System.out.println(title + ":");
        for (int i = 0; i < items.size(); i++) {
            System.out.println((i + 1) + ") " + items.get(i));
        }
        int idx = readInt("Choose number (0 to cancel): ");
        if (idx <= 0 || idx > items.size()) {
            return null;
        }
        return items.get(idx - 1);
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private Role chooseRole() {
        System.out.println("Select role: 1) CUSTOMER 2) RESTAURANT_OWNER 3) DELIVERY_PERSON");
        return switch (readInt("Role number: ")) {
            case 1 -> Role.CUSTOMER;
            case 2 -> Role.RESTAURANT_OWNER;
            case 3 -> Role.DELIVERY_PERSON;
            default -> null;
        };
    }

    private void printOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            System.out.println("No orders found.");
            return;
        }
        System.out.println("\n---- Orders ----");
        orders.forEach(o -> System.out.printf(
                "%s | Customer: %s | Restaurant: %s | Item: %s | Qty: %d | Status: %s | Address: %s%n",
                o.getOrderId(),
                o.getCustomer().getUsername(),
                o.getRestaurant().getName(),
                o.getMenuItem().getName(),
                o.getQuantity(),
                o.getStatus(),
                o.getDeliveryAddress()
        ));
    }

    private void seedData() {
        if (restaurantRepository.count() > 0) {
            return;
        }
        Restaurant spiceHub = restaurantRepository.save("Spice Hub");
        Restaurant pizzaPoint = restaurantRepository.save("Pizza Point");

        menuItemRepository.save("Paneer Bowl", 180.0, spiceHub);
        menuItemRepository.save("Veg Biryani", 220.0, spiceHub);
        menuItemRepository.save("Margherita Pizza", 250.0, pizzaPoint);
        menuItemRepository.save("Farmhouse Pizza", 320.0, pizzaPoint);

        userRepository.save("customer1", "pass", Role.CUSTOMER, null);
        userRepository.save("owner_spice", "pass", Role.RESTAURANT_OWNER, spiceHub);
        userRepository.save("owner_pizza", "pass", Role.RESTAURANT_OWNER, pizzaPoint);
        userRepository.save("delivery1", "pass", Role.DELIVERY_PERSON, null);
    }
}
