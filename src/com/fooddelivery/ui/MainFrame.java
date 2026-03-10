package com.fooddelivery.ui;

import com.fooddelivery.model.Order;
import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.OrderService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class MainFrame extends JFrame {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

    private final String loggedInUser;
    private final AuthService authService;
    private final OrderService orderService;

    private final JTextField customerNameField = new JTextField(15);
    private final JTextField itemNameField = new JTextField(15);
    private final JTextField quantityField = new JTextField(15);
    private final JTextField statusOrderIdField = new JTextField(15);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Order ID", "Customer", "Item", "Qty", "Status", "Placed At"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable historyTable = new JTable(tableModel);
    private final JTextArea messageArea = new JTextArea(4, 20);

    public MainFrame(String loggedInUser, AuthService authService, OrderService orderService) {
        this.loggedInUser = loggedInUser;
        this.authService = authService;
        this.orderService = orderService;

        setTitle("Online Food Delivery Tracker - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(980, 650));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenterContent(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        refreshHistoryTable();
        startAutoRefresh();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 12, 4, 12));

        JLabel title = new JLabel("Online Food Delivery Tracker", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(20f));

        JLabel userLabel = new JLabel("Logged in as: " + loggedInUser + "   ");
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());

        JPanel rightPanel = new JPanel();
        rightPanel.add(userLabel);
        rightPanel.add(logoutButton);

        header.add(title, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    private JPanel buildCenterContent() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        JPanel formsPanel = new JPanel(new GridBagLayout());
        formsPanel.setBorder(BorderFactory.createTitledBorder("Order Actions"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formsPanel.add(new JLabel("Customer Name:"), gbc);
        gbc.gridx = 1;
        formsPanel.add(customerNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formsPanel.add(new JLabel("Food Item:"), gbc);
        gbc.gridx = 1;
        formsPanel.add(itemNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formsPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        formsPanel.add(quantityField, gbc);

        JButton placeOrderButton = new JButton("Place Order");
        placeOrderButton.addActionListener(e -> placeOrder());
        gbc.gridx = 1;
        gbc.gridy = 3;
        formsPanel.add(placeOrderButton, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        formsPanel.add(new JLabel("Track by Order ID:"), gbc);
        gbc.gridx = 3;
        formsPanel.add(statusOrderIdField, gbc);

        JButton checkStatusButton = new JButton("Check Delivery Status");
        checkStatusButton.addActionListener(e -> checkStatus());
        gbc.gridx = 3;
        gbc.gridy = 1;
        formsPanel.add(checkStatusButton, gbc);

        JButton refreshButton = new JButton("Refresh History");
        refreshButton.addActionListener(e -> refreshHistoryTable());
        gbc.gridy = 2;
        formsPanel.add(refreshButton, gbc);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Order History"));
        historyTable.setRowHeight(24);
        tablePanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        root.add(formsPanel, BorderLayout.NORTH);
        root.add(tablePanel, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createTitledBorder("Status / Notifications"));

        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setText("Welcome, " + loggedInUser + ". Place an order to begin tracking.");

        footer.add(new JScrollPane(messageArea), BorderLayout.CENTER);
        return footer;
    }

    private void placeOrder() {
        try {
            int quantity = Integer.parseInt(quantityField.getText().trim());
            Order order = orderService.placeOrder(
                    loggedInUser,
                    customerNameField.getText(),
                    itemNameField.getText(),
                    quantity
            );
            appendMessage("Order placed successfully: " + order.getOrderId());
            statusOrderIdField.setText(order.getOrderId());
            refreshHistoryTable();
            clearOrderForm();
        } catch (NumberFormatException ex) {
            showError("Quantity must be a number.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void checkStatus() {
        String orderId = statusOrderIdField.getText().trim();
        Optional<Order> order = orderService.findOrderById(loggedInUser, orderId);

        if (order.isEmpty()) {
            showError("Order not found for ID: " + orderId);
            return;
        }

        appendMessage("Order " + order.get().getOrderId() + " status: " + order.get().getStatus().getDisplayName());
        refreshHistoryTable();
    }

    private void refreshHistoryTable() {
        orderService.refreshStatusesFromElapsedTime();
        List<Order> orders = orderService.getOrderHistory(loggedInUser);
        tableModel.setRowCount(0);

        orders.forEach(order -> tableModel.addRow(new Object[]{
                order.getOrderId(),
                order.getCustomerName(),
                order.getItemName(),
                order.getQuantity(),
                order.getStatus().getDisplayName(),
                order.getOrderTime().format(TIME_FORMATTER)
        }));
    }

    private void startAutoRefresh() {
        Timer timer = new Timer(2000, event -> refreshHistoryTable());
        timer.start();
    }

    private void clearOrderForm() {
        customerNameField.setText("");
        itemNameField.setText("");
        quantityField.setText("");
    }

    private void showError(String message) {
        appendMessage("Error: " + message);
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    private void appendMessage(String message) {
        messageArea.append("\n" + message);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Do you want to logout?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        dispose();
        new LoginFrame(authService, orderService).setVisible(true);
    }
}
