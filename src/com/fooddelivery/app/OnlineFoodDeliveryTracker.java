package com.fooddelivery.app;

import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.OrderService;
import com.fooddelivery.service.RestaurantService;
import com.fooddelivery.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class OnlineFoodDeliveryTracker {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setSystemLookAndFeel();
            AuthService authService = new AuthService();
            OrderService orderService = new OrderService();
            RestaurantService restaurantService = new RestaurantService();
            new LoginFrame(authService, orderService, restaurantService).setVisible(true);
        });
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }
}
