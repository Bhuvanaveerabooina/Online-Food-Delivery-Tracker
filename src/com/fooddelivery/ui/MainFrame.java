package com.fooddelivery.ui;

import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.OrderService;

import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * Deprecated entry frame. The application now uses role-based dashboards.
 */
public class MainFrame extends JFrame {
    public MainFrame(String username, AuthService authService, OrderService orderService) {
        add(new JLabel("Deprecated. Please login from LoginFrame."));
        setSize(320, 120);
    }
}
