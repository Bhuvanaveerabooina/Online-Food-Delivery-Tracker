package com.fooddelivery.app;

import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.service.OrderService;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Main class for the console-based Online Food Delivery Tracker app.
 */
public class OnlineFoodDeliveryTracker {
    private final OrderService orderService = new OrderService();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new OnlineFoodDeliveryTracker().start();
    }

    private void start() {
        System.out.println("===== Online Food Delivery Tracker =====");

        boolean running = true;
        while (running) {
            printMenu();
            int choice = getIntegerInput("Enter your choice: ");

            switch (choice) {
                case 1:
                    placeOrderFlow();
                    break;
                case 2:
                    trackDeliveryStatusFlow();
                    break;
                case 3:
                    showOrderHistoryFlow();
                    break;
                case 4:
                    running = false;
                    System.out.println("Thank you for using Online Food Delivery Tracker!");
                    break;
                default:
                    System.out.println("Invalid choice. Please select between 1 and 4.");
            }
        }

        scanner.close();
    }

    private void printMenu() {
        System.out.println("\n1. Place Order");
        System.out.println("2. Delivery Status");
        System.out.println("3. Order History");
        System.out.println("4. Exit");
    }

    private void placeOrderFlow() {
        try {
            System.out.print("Enter customer name: ");
            String customerName = scanner.nextLine().trim();

            System.out.print("Enter food item name: ");
            String itemName = scanner.nextLine().trim();

            int quantity = getIntegerInput("Enter quantity: ");

            Order order = orderService.placeOrder(customerName, itemName, quantity);
            System.out.println("Order placed successfully. Your Order ID is " + order.getOrderId());
            System.out.println("Current status: " + order.getStatus());
        } catch (IllegalArgumentException e) {
            System.out.println("Could not place order: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error while placing order: " + e.getMessage());
        }
    }

    private void trackDeliveryStatusFlow() {
        System.out.print("Enter order ID: ");
        String orderId = scanner.nextLine().trim();

        Optional<Order> orderOptional = orderService.findOrderById(orderId);
        if (orderOptional.isPresent()) {
            Order order = orderOptional.get();
            System.out.println("Order " + order.getOrderId() + " status: " + order.getStatus());
        } else {
            System.out.println("No order found with ID " + orderId);
        }
    }

    private void showOrderHistoryFlow() {
        List<Order> orders = orderService.getOrderHistory();
        if (orders.isEmpty()) {
            System.out.println("No orders placed yet.");
            return;
        }

        System.out.println("\n===== Order History =====");
        orders.forEach(System.out::println);

        long deliveredCount = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .count();

        System.out.println("Total Orders: " + orders.size());
        System.out.println("Delivered Orders: " + deliveredCount);
    }

    private int getIntegerInput(String message) {
        while (true) {
            try {
                System.out.print(message);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}
