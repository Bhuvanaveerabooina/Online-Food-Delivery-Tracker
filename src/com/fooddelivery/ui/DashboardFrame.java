package com.fooddelivery.ui;

import com.fooddelivery.model.Role;
import com.fooddelivery.model.UserAccount;
import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.OrderService;
import com.fooddelivery.service.RestaurantService;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;

public class DashboardFrame extends JFrame {

    public DashboardFrame(UserAccount user, AuthService authService, OrderService orderService, RestaurantService restaurantService) {
        setTitle("Online Food Delivery Tracker - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(980, 580));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel userInfo = new JLabel("Logged in as: " + user.getUsername() + " (" + user.getRole().getDisplayName() + ")",
                SwingConstants.LEFT);
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            dispose();
            new LoginFrame(authService, orderService, restaurantService).setVisible(true);
        });
        topPanel.add(userInfo, BorderLayout.WEST);
        topPanel.add(logoutButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        if (user.getRole() == Role.CUSTOMER) {
            add(new CustomerPanel(user, orderService, restaurantService), BorderLayout.CENTER);
        } else if (user.getRole() == Role.RESTAURANT_OWNER) {
            add(new RestaurantOwnerPanel(user, orderService), BorderLayout.CENTER);
        } else {
            add(new DeliveryPanel(user, orderService), BorderLayout.CENTER);
        }
    }
}
