package com.fooddelivery.ui;

import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.OrderService;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class LoginFrame extends JFrame {
    private final AuthService authService;
    private final OrderService orderService;

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);

    public LoginFrame(AuthService authService, OrderService orderService) {
        this.authService = authService;
        this.orderService = orderService;

        setTitle("Online Food Delivery Tracker - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(460, 320));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        JLabel heading = new JLabel("Online Food Delivery Tracker", SwingConstants.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 20));
        heading.setBorder(BorderFactory.createEmptyBorder(18, 12, 2, 12));

        JLabel subHeading = new JLabel("Mini Project Login", SwingConstants.CENTER);
        subHeading.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(heading, BorderLayout.CENTER);
        topPanel.add(subHeading, BorderLayout.SOUTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 20, 5, 20),
                BorderFactory.createTitledBorder("Enter Credentials")
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

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> doLogin());

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            usernameField.setText("");
            passwordField.setText("");
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        buttonPanel.add(resetButton);

        JLabel defaultInfo = new JLabel(
                "Demo user: " + AuthService.DEFAULT_USERNAME + " / " + AuthService.DEFAULT_PASSWORD,
                SwingConstants.CENTER
        );
        defaultInfo.setBorder(BorderFactory.createEmptyBorder(4, 8, 12, 8));

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.add(buttonPanel);
        southPanel.add(defaultInfo);

        add(topPanel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        boolean success = authService.authenticate(username, password);
        if (!success) {
            JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        dispose();
        new MainFrame(username, authService, orderService).setVisible(true);
    }
}
