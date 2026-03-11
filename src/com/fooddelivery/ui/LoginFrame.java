package com.fooddelivery.ui;

import com.fooddelivery.model.Role;
import com.fooddelivery.model.UserAccount;
import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.OrderService;
import com.fooddelivery.service.RestaurantService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Optional;

public class LoginFrame extends JFrame {
    private final AuthService authService;
    private final OrderService orderService;
    private final RestaurantService restaurantService;

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JComboBox<Role> roleCombo = new JComboBox<>(Role.values());

    public LoginFrame(AuthService authService, OrderService orderService, RestaurantService restaurantService) {
        this.authService = authService;
        this.orderService = orderService;
        this.restaurantService = restaurantService;

        setTitle("Online Food Delivery Tracker - Role Based Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(460, 300));
        setLocationRelativeTo(null);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 15, 10, 15),
                BorderFactory.createTitledBorder("Login")
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        formPanel.add(roleCombo, gbc);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> doLogin());
        gbc.gridx = 1;
        gbc.gridy = 3;
        formPanel.add(loginButton, gbc);

        JLabel infoLabel = new JLabel("Demo: customer1/cust123, owner_spice/owner123, delivery1/del123");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        add(formPanel, BorderLayout.CENTER);
        add(infoLabel, BorderLayout.SOUTH);
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        Role selectedRole = (Role) roleCombo.getSelectedItem();

        if (username.isEmpty() || password.isEmpty() || selectedRole == null) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Optional<UserAccount> user = authService.authenticate(username, password, selectedRole);
        if (!user.isPresent()) {
            JOptionPane.showMessageDialog(this, "Invalid username, password, or role.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        dispose();
        new DashboardFrame(user.get(), authService, orderService, restaurantService).setVisible(true);
    }
}
