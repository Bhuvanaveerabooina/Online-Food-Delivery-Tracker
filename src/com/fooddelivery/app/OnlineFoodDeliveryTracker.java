package com.fooddelivery.app;

import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.service.OrderService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Optional;

/**
 * Main class for the Swing-based Online Food Delivery Tracker app.
 *
 * This class only handles UI interactions. All business logic is still managed by OrderService.
 */
public class OnlineFoodDeliveryTracker extends JFrame {
    // Reuse the existing backend service without changing its logic.
    private final OrderService orderService = new OrderService();

    // Input fields used by different actions.
    private final JTextField customerNameField = new JTextField(12);
    private final JTextField itemNameField = new JTextField(12);
    private final JTextField quantityField = new JTextField(6);
    private final JTextField orderIdField = new JTextField(10);

    // Output area to display user-friendly summaries.
    private final JTextArea displayArea = new JTextArea(14, 50);

    public static void main(String[] args) {
        // Swing applications should be created on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            OnlineFoodDeliveryTracker tracker = new OnlineFoodDeliveryTracker();
            tracker.setVisible(true);
        });
    }

    public OnlineFoodDeliveryTracker() {
        setTitle("Online Food Delivery Tracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(780, 500);
        setLocationRelativeTo(null); // Center the window.

        initializeUi();
    }

    private void initializeUi() {
        // Top section: title and form fields.
        JPanel topPanel = new JPanel(new BorderLayout());

        JLabel heading = new JLabel("Online Food Delivery Tracker", JLabel.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 22));
        heading.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));
        topPanel.add(heading, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Order Details"));

        inputPanel.add(new JLabel("Customer:"));
        inputPanel.add(customerNameField);

        inputPanel.add(new JLabel("Food Item:"));
        inputPanel.add(itemNameField);

        inputPanel.add(new JLabel("Quantity:"));
        inputPanel.add(quantityField);

        inputPanel.add(new JLabel("Order ID:"));
        inputPanel.add(orderIdField);

        topPanel.add(inputPanel, BorderLayout.CENTER);

        // Middle section: buttons requested in the prompt.
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 10));

        JButton placeOrderButton = new JButton("Place Order");
        JButton deliveryStatusButton = new JButton("Delivery Status");
        JButton orderHistoryButton = new JButton("Order History");
        JButton exitButton = new JButton("Exit");

        buttonPanel.add(placeOrderButton);
        buttonPanel.add(deliveryStatusButton);
        buttonPanel.add(orderHistoryButton);
        buttonPanel.add(exitButton);

        // Bottom section: display area for status/history text.
        displayArea.setEditable(false);
        displayArea.setLineWrap(true);
        displayArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(displayArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Output"));

        // Add all sections to the window.
        add(topPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        // Button handlers.
        placeOrderButton.addActionListener(e -> placeOrderFlow());
        deliveryStatusButton.addActionListener(e -> trackDeliveryStatusFlow());
        orderHistoryButton.addActionListener(e -> showOrderHistoryFlow());
        exitButton.addActionListener(e -> System.exit(0));
    }

    private void placeOrderFlow() {
        try {
            String customerName = customerNameField.getText().trim();
            String itemName = itemNameField.getText().trim();
            int quantity = Integer.parseInt(quantityField.getText().trim());

            Order order = orderService.placeOrder(customerName, itemName, quantity);

            String message = "Order placed successfully!\n"
                    + "Order ID: " + order.getOrderId() + "\n"
                    + "Current Status: " + order.getStatus();

            // Show immediate feedback with a dialog box.
            JOptionPane.showMessageDialog(this, message, "Order Placed", JOptionPane.INFORMATION_MESSAGE);
            displayArea.setText(message);

            // Helpful auto-fill for quick status checks.
            orderIdField.setText(order.getOrderId());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantity must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, "Could not place order: " + e.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unexpected error while placing order: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void trackDeliveryStatusFlow() {
        String orderId = orderIdField.getText().trim();
        Optional<Order> orderOptional = orderService.findOrderById(orderId);

        if (orderOptional.isPresent()) {
            Order order = orderOptional.get();
            String message = "Order " + order.getOrderId() + " status: " + order.getStatus();
            displayArea.setText(message);
            JOptionPane.showMessageDialog(this, message, "Delivery Status", JOptionPane.INFORMATION_MESSAGE);
        } else {
            String message = "No order found with ID " + orderId;
            displayArea.setText(message);
            JOptionPane.showMessageDialog(this, message, "Delivery Status", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showOrderHistoryFlow() {
        List<Order> orders = orderService.getOrderHistory();

        if (orders.isEmpty()) {
            displayArea.setText("No orders placed yet.");
            JOptionPane.showMessageDialog(this, "No orders placed yet.", "Order History", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        long deliveredCount = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .count();

        StringBuilder builder = new StringBuilder();
        builder.append("===== Order History =====\n");

        for (Order order : orders) {
            builder.append(order).append("\n");
        }

        builder.append("\nTotal Orders: ").append(orders.size());
        builder.append("\nDelivered Orders: ").append(deliveredCount);

        displayArea.setText(builder.toString());
        JOptionPane.showMessageDialog(this,
                "Total Orders: " + orders.size() + "\nDelivered Orders: " + deliveredCount,
                "Order History Summary",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
