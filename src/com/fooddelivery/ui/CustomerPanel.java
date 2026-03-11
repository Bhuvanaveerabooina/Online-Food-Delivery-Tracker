package com.fooddelivery.ui;

import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Order;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.UserAccount;
import com.fooddelivery.service.OrderService;
import com.fooddelivery.service.RestaurantService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CustomerPanel extends JPanel {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final UserAccount user;
    private final OrderService orderService;
    private final RestaurantService restaurantService;

    private final JComboBox<String> restaurantCombo = new JComboBox<>();
    private final JComboBox<String> itemCombo = new JComboBox<>();
    private final JLabel priceLabel = new JLabel("0.00");
    private final JTextField customerNameField = new JTextField(14);
    private final JTextField quantityField = new JTextField(8);
    private final JTextField addressField = new JTextField(18);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Order ID", "Restaurant", "Item", "Qty", "Total", "Address", "Status", "Time"}, 0
    );

    public CustomerPanel(UserAccount user, OrderService orderService, RestaurantService restaurantService) {
        this.user = user;
        this.orderService = orderService;
        this.restaurantService = restaurantService;
        setLayout(new BorderLayout(10, 10));

        add(buildFormPanel(), BorderLayout.NORTH);
        add(new JScrollPane(new JTable(tableModel)), BorderLayout.CENTER);

        loadRestaurants();
        refreshOrders();
        new Timer(3000, e -> refreshOrders()).start();
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Place Order"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        customerNameField.setText(user.getUsername());

        restaurantCombo.addActionListener(e -> loadItemsForRestaurant());
        itemCombo.addActionListener(e -> updatePriceLabel());

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Customer Name:"), gbc);
        gbc.gridx = 1; panel.add(customerNameField, gbc);

        gbc.gridx = 2; panel.add(new JLabel("Restaurant:"), gbc);
        gbc.gridx = 3; panel.add(restaurantCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Food Item:"), gbc);
        gbc.gridx = 1; panel.add(itemCombo, gbc);

        gbc.gridx = 2; panel.add(new JLabel("Price:"), gbc);
        gbc.gridx = 3; panel.add(priceLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1; panel.add(quantityField, gbc);

        gbc.gridx = 2; panel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 3; panel.add(addressField, gbc);

        JButton placeOrderButton = new JButton("Place Order");
        placeOrderButton.addActionListener(e -> placeOrder());
        gbc.gridx = 3; gbc.gridy = 3; panel.add(placeOrderButton, gbc);

        return panel;
    }

    private void loadRestaurants() {
        restaurantCombo.removeAllItems();
        List<Restaurant> restaurants = restaurantService.getAllRestaurants();
        for (Restaurant restaurant : restaurants) {
            restaurantCombo.addItem(restaurant.getName());
        }
        if (!restaurants.isEmpty()) {
            restaurantCombo.setSelectedIndex(0);
            loadItemsForRestaurant();
        }
    }

    private void loadItemsForRestaurant() {
        itemCombo.removeAllItems();
        String selectedRestaurant = (String) restaurantCombo.getSelectedItem();
        if (selectedRestaurant == null) {
            return;
        }

        restaurantService.findByName(selectedRestaurant).ifPresent(restaurant -> {
            for (MenuItem item : restaurant.getMenuItems()) {
                itemCombo.addItem(item.getName());
            }
        });

        if (itemCombo.getItemCount() > 0) {
            itemCombo.setSelectedIndex(0);
            updatePriceLabel();
        }
    }

    private void updatePriceLabel() {
        String selectedRestaurant = (String) restaurantCombo.getSelectedItem();
        String selectedItem = (String) itemCombo.getSelectedItem();
        if (selectedRestaurant == null || selectedItem == null) {
            priceLabel.setText("0.00");
            return;
        }

        restaurantService.findByName(selectedRestaurant).ifPresent(restaurant ->
                restaurant.getMenuItems().stream()
                        .filter(menuItem -> menuItem.getName().equalsIgnoreCase(selectedItem))
                        .findFirst()
                        .ifPresent(menuItem -> priceLabel.setText(String.format("%.2f", menuItem.getPrice())))
        );
    }

    private void placeOrder() {
        try {
            String selectedRestaurant = (String) restaurantCombo.getSelectedItem();
            String selectedItem = (String) itemCombo.getSelectedItem();
            int quantity = Integer.parseInt(quantityField.getText().trim());
            double unitPrice = Double.parseDouble(priceLabel.getText());

            Order order = orderService.placeOrder(user.getUsername(), customerNameField.getText(), selectedRestaurant,
                    selectedItem, unitPrice, quantity, addressField.getText());

            JOptionPane.showMessageDialog(this, "Order placed successfully. Order ID: " + order.getOrderId());
            refreshOrders();
            quantityField.setText("");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity must be numeric.", "Validation", JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void refreshOrders() {
        tableModel.setRowCount(0);
        List<Order> orders = orderService.getOrdersForCustomer(user.getUsername());
        for (Order order : orders) {
            tableModel.addRow(new Object[]{
                    order.getOrderId(),
                    order.getRestaurantName(),
                    order.getItemName(),
                    order.getQuantity(),
                    String.format("%.2f", order.getTotalPrice()),
                    order.getAddress(),
                    order.getStatus().getDisplayName(),
                    order.getOrderTime().format(TIME_FORMATTER)
            });
        }
    }
}
