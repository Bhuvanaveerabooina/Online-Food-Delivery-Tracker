package com.fooddelivery.ui;

import com.fooddelivery.model.Order;
import com.fooddelivery.model.UserAccount;
import com.fooddelivery.service.OrderService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DeliveryPanel extends JPanel {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final UserAccount user;
    private final OrderService orderService;
    private final JTable table;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Order ID", "Restaurant", "Customer", "Address", "Status", "Assigned To", "Time"}, 0
    );

    public DeliveryPanel(UserAccount user, OrderService orderService) {
        this.user = user;
        this.orderService = orderService;
        setLayout(new BorderLayout(10, 10));

        table = new JTable(tableModel);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton pickupButton = new JButton("Start Delivery");
        pickupButton.addActionListener(e -> startDelivery());
        JButton deliveredButton = new JButton("Mark Delivered");
        deliveredButton.addActionListener(e -> markDelivered());

        top.add(new JLabel("Manage your delivery orders"));
        top.add(pickupButton);
        top.add(deliveredButton);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshOrders();
        new Timer(3000, e -> refreshOrders()).start();
    }

    private void startDelivery() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an order first.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String orderId = tableModel.getValueAt(row, 0).toString();
        boolean updated = orderService.markOutForDelivery(user.getUsername(), orderId);
        if (!updated) {
            JOptionPane.showMessageDialog(this,
                    "Only READY_FOR_PICKUP orders can be started.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        refreshOrders();
    }

    private void markDelivered() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an order first.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String orderId = tableModel.getValueAt(row, 0).toString();
        boolean updated = orderService.markDelivered(user.getUsername(), orderId);
        if (!updated) {
            JOptionPane.showMessageDialog(this,
                    "Only your OUT_FOR_DELIVERY orders can be marked DELIVERED.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        refreshOrders();
    }

    private void refreshOrders() {
        tableModel.setRowCount(0);
        List<Order> orders = orderService.getOrdersForDeliveryPerson(user.getUsername());

        for (Order order : orders) {
            tableModel.addRow(new Object[]{
                    order.getOrderId(),
                    order.getRestaurantName(),
                    order.getCustomerName(),
                    order.getAddress(),
                    order.getStatus().getDisplayName(),
                    order.getAssignedDeliveryUsername() == null ? "Unassigned" : order.getAssignedDeliveryUsername(),
                    order.getOrderTime().format(TIME_FORMATTER)
            });
        }
    }
}
