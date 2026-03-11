package com.fooddelivery.ui;

import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.model.UserAccount;
import com.fooddelivery.service.OrderService;

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
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RestaurantOwnerPanel extends JPanel {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final UserAccount user;
    private final OrderService orderService;
    private final JTextField searchField = new JTextField(20);
    private final JComboBox<OrderStatus> statusCombo = new JComboBox<>(new OrderStatus[]{
            OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP
    });
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Order ID", "Customer", "Address", "Item", "Qty", "Status", "Time"}, 0
    );

    public RestaurantOwnerPanel(UserAccount user, OrderService orderService) {
        this.user = user;
        this.orderService = orderService;
        setLayout(new BorderLayout(10, 10));

        add(buildTopBar(), BorderLayout.NORTH);
        add(new JScrollPane(new JTable(tableModel)), BorderLayout.CENTER);
        refreshOrders();
        new Timer(3000, e -> refreshOrders()).start();
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBorder(BorderFactory.createTitledBorder("Restaurant: " + user.getRestaurantName()));

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> refreshOrders());

        JButton updateButton = new JButton("Update Status");
        updateButton.addActionListener(e -> updateSelectedOrderStatus());

        top.add(new JLabel("Search by Order ID / Customer:"));
        top.add(searchField);
        top.add(searchButton);
        top.add(new JLabel("Set Status:"));
        top.add(statusCombo);
        top.add(updateButton);
        return top;
    }

    private void updateSelectedOrderStatus() {
        int selectedRow = getTable().getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select an order first.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String orderId = tableModel.getValueAt(selectedRow, 0).toString();
        OrderStatus nextStatus = (OrderStatus) statusCombo.getSelectedItem();
        boolean updated = orderService.updateRestaurantOrderStatus(user.getRestaurantName(), orderId, nextStatus);

        if (!updated) {
            JOptionPane.showMessageDialog(this,
                    "Invalid status transition. Allowed: PLACED -> PREPARING -> READY_FOR_PICKUP",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        refreshOrders();
    }

    private void refreshOrders() {
        tableModel.setRowCount(0);
        String query = searchField.getText();
        List<Order> orders = orderService.searchOrdersForRestaurant(user.getRestaurantName(), query);

        for (Order order : orders) {
            tableModel.addRow(new Object[]{
                    order.getOrderId(),
                    order.getCustomerName(),
                    order.getAddress(),
                    order.getItemName(),
                    order.getQuantity(),
                    order.getStatus().getDisplayName(),
                    order.getOrderTime().format(TIME_FORMATTER)
            });
        }
    }

    private JTable getTable() {
        return (JTable) ((JScrollPane) getComponent(1)).getViewport().getView();
    }
}
