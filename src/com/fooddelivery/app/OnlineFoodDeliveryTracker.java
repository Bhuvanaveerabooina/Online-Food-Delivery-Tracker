package com.fooddelivery.app;

import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.service.OrderService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Main class for the Swing-based Online Food Delivery Tracker app.
 */
public class OnlineFoodDeliveryTracker extends JFrame {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

    private final OrderService orderService = new OrderService();

    // Place Order module fields.
    private final JTextField customerNameField = new JTextField(18);
    private final JTextField itemNameField = new JTextField(18);
    private final JTextField quantityField = new JTextField(6);

    // Delivery Status module fields.
    private final JTextField orderIdField = new JTextField(12);
    private final JLabel liveStatusLabel = new JLabel("Enter an Order ID and click Check Status");
    private final JTextArea statusDetailsArea = new JTextArea(8, 40);

    // Order History module table.
    private final DefaultTableModel historyTableModel = new DefaultTableModel(
            new Object[]{"Order ID", "Customer", "Item", "Qty", "Status", "Order Time"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable historyTable = new JTable(historyTableModel);
    private final JLabel historySummaryLabel = new JLabel("Total Orders: 0 | Delivered: 0");

    private Timer uiRefreshTimer;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            OnlineFoodDeliveryTracker tracker = new OnlineFoodDeliveryTracker();
            tracker.setVisible(true);
        });
    }

    public OnlineFoodDeliveryTracker() {
        setTitle("Online Food Delivery Tracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 620);
        setLocationRelativeTo(null);

        initializeUi();
        refreshEntireUi(); // Load history and status immediately at startup.
        startUiRefreshTimer(); // Keep UI synced with background status simulation.
    }

    private void initializeUi() {
        JPanel rootPanel = new JPanel(new BorderLayout(0, 10));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JLabel heading = new JLabel("Online Food Delivery Tracker", JLabel.CENTER);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 26));
        heading.setForeground(new Color(32, 70, 133));
        rootPanel.add(heading, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        tabbedPane.addTab("Place Order", createPlaceOrderPanel());
        tabbedPane.addTab("Delivery Status", createDeliveryStatusPanel());
        tabbedPane.addTab("Order History", createOrderHistoryPanel());

        rootPanel.add(tabbedPane, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> dispose());
        footerPanel.add(exitButton);
        rootPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(rootPanel);
    }

    private JPanel createPlaceOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createTitledBorder("New Order Information"));

        formPanel.add(buildFieldRow("Customer Name:", customerNameField));
        formPanel.add(buildFieldRow("Food Item:", itemNameField));
        formPanel.add(buildFieldRow("Quantity:", quantityField));

        JButton placeOrderButton = new JButton("Place Order");
        placeOrderButton.addActionListener(e -> placeOrderFlow());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonRow.add(placeOrderButton);
        formPanel.add(buttonRow);

        JTextArea helperText = new JTextArea(
                "Tip: After placing an order, copy or keep the Order ID for tracking.\n"
                        + "Statuses update automatically from Placed → Preparing → Out for Delivery → Delivered.");
        helperText.setEditable(false);
        helperText.setLineWrap(true);
        helperText.setWrapStyleWord(true);
        helperText.setBackground(new Color(245, 248, 255));
        helperText.setBorder(BorderFactory.createTitledBorder("How it works"));

        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(helperText, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDeliveryStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputRow.setBorder(BorderFactory.createTitledBorder("Track an Order"));
        inputRow.add(new JLabel("Order ID:"));
        inputRow.add(orderIdField);

        JButton checkStatusButton = new JButton("Check Status");
        checkStatusButton.addActionListener(e -> trackDeliveryStatusFlow());
        inputRow.add(checkStatusButton);

        liveStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        liveStatusLabel.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 6));

        statusDetailsArea.setEditable(false);
        statusDetailsArea.setLineWrap(true);
        statusDetailsArea.setWrapStyleWord(true);
        statusDetailsArea.setBorder(BorderFactory.createTitledBorder("Order Details"));

        panel.add(inputRow, BorderLayout.NORTH);
        panel.add(liveStatusLabel, BorderLayout.CENTER);
        panel.add(new JScrollPane(statusDetailsArea), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createOrderHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        historyTable.setRowHeight(24);
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JScrollPane tableScrollPane = new JScrollPane(historyTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("All Orders (Loaded from Saved File on Startup)"));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(historySummaryLabel, BorderLayout.WEST);

        JButton refreshButton = new JButton("Refresh Now");
        refreshButton.addActionListener(e -> refreshEntireUi());
        topBar.add(refreshButton, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildFieldRow(String labelText, JTextField field) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(120, 26));
        row.add(label);
        row.add(field);
        row.add(Box.createHorizontalStrut(10));
        return row;
    }

    private void placeOrderFlow() {
        try {
            String customerName = customerNameField.getText().trim();
            String itemName = itemNameField.getText().trim();
            int quantity = Integer.parseInt(quantityField.getText().trim());

            if (customerName.isEmpty() || itemName.isEmpty()) {
                throw new IllegalArgumentException("Customer name and food item are required.");
            }

            Order order = orderService.placeOrder(customerName, itemName, quantity);
            orderIdField.setText(order.getOrderId());

            JOptionPane.showMessageDialog(this,
                    "Order placed successfully!\nOrder ID: " + order.getOrderId(),
                    "Order Placed",
                    JOptionPane.INFORMATION_MESSAGE);

            customerNameField.setText("");
            itemNameField.setText("");
            quantityField.setText("");

            refreshEntireUi();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Quantity must be a valid number.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this,
                    e.getMessage(),
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void trackDeliveryStatusFlow() {
        String orderId = orderIdField.getText().trim();
        if (orderId.isEmpty()) {
            liveStatusLabel.setText("Please enter an Order ID.");
            statusDetailsArea.setText("No order selected.");
            return;
        }

        updateStatusSection(orderId);
    }

    private void updateStatusSection(String orderId) {
        Optional<Order> orderOptional = orderService.findOrderById(orderId);

        if (orderOptional.isPresent()) {
            Order order = orderOptional.get();
            liveStatusLabel.setText("Order " + order.getOrderId() + " is currently: " + order.getStatus());
            statusDetailsArea.setText(
                    "Customer: " + order.getCustomerName() + "\n"
                            + "Food Item: " + order.getItemName() + "\n"
                            + "Quantity: " + order.getQuantity() + "\n"
                            + "Order Time: " + order.getOrderTime().format(TIME_FORMATTER) + "\n"
                            + "Status: " + order.getStatus());
        } else {
            liveStatusLabel.setText("No order found for ID: " + orderId);
            statusDetailsArea.setText("Please verify the order ID and try again.");
        }
    }

    private void refreshEntireUi() {
        refreshHistoryTable();

        String trackedOrderId = orderIdField.getText().trim();
        if (!trackedOrderId.isEmpty()) {
            updateStatusSection(trackedOrderId);
        }
    }

    private void refreshHistoryTable() {
        List<Order> orders = orderService.getOrderHistory();

        historyTableModel.setRowCount(0);
        for (Order order : orders) {
            historyTableModel.addRow(new Object[]{
                    order.getOrderId(),
                    order.getCustomerName(),
                    order.getItemName(),
                    order.getQuantity(),
                    order.getStatus(),
                    order.getOrderTime().format(TIME_FORMATTER)
            });
        }

        long deliveredCount = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .count();
        historySummaryLabel.setText("Total Orders: " + orders.size() + " | Delivered: " + deliveredCount);
    }

    private void startUiRefreshTimer() {
        uiRefreshTimer = new Timer(1000, e -> refreshEntireUi());
        uiRefreshTimer.start();
    }
}
