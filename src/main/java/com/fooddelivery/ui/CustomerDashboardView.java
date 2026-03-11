package com.fooddelivery.ui;

import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Order;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.Role;
import com.fooddelivery.model.User;
import com.fooddelivery.service.OrderService;
import com.fooddelivery.service.SessionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;

import java.util.List;

@Route("customer")
@PageTitle("Customer Dashboard")
public class CustomerDashboardView extends VerticalLayout implements BeforeEnterObserver {

    private final SessionService sessionService;
    private final OrderService orderService;
    private final Grid<Order> orderGrid = new Grid<>(Order.class, false);

    public CustomerDashboardView(SessionService sessionService, OrderService orderService) {
        this.sessionService = sessionService;
        this.orderService = orderService;

        User customer = sessionService.getCurrentUser();

        ComboBox<Restaurant> restaurantBox = new ComboBox<>("Restaurant");
        restaurantBox.setItems(orderService.getAllRestaurants());
        restaurantBox.setItemLabelGenerator(Restaurant::getName);

        ComboBox<MenuItem> itemBox = new ComboBox<>("Menu Item");
        itemBox.setItemLabelGenerator(MenuItem::getName);

        TextField priceField = new TextField("Price");
        priceField.setReadOnly(true);
        IntegerField quantityField = new IntegerField("Quantity");
        quantityField.setMin(1);
        TextArea addressField = new TextArea("Delivery Address");

        restaurantBox.addValueChangeListener(event -> {
            Restaurant selected = event.getValue();
            if (selected != null) {
                List<MenuItem> items = orderService.getMenuItemsByRestaurant(selected);
                itemBox.setItems(items);
            } else {
                itemBox.clear();
                itemBox.setItems(List.of());
            }
            priceField.clear();
        });

        itemBox.addValueChangeListener(event -> {
            MenuItem item = event.getValue();
            priceField.setValue(item != null ? item.getPrice().toPlainString() : "");
        });

        Button placeOrder = new Button("Place Order", e -> {
            if (restaurantBox.isEmpty() || itemBox.isEmpty() || quantityField.isEmpty() || addressField.isEmpty()) {
                Notification.show("Please fill all fields.");
                return;
            }
            try {
                orderService.placeOrder(customer, restaurantBox.getValue(), itemBox.getValue(), quantityField.getValue(), addressField.getValue());
                Notification.show("Order placed successfully.");
                quantityField.clear();
                addressField.clear();
                refreshGrid(customer);
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage());
            }
        });

        Button logout = new Button("Logout", e -> {
            sessionService.clear();
            getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        });

        orderGrid.addColumn(Order::getId).setHeader("Order ID");
        orderGrid.addColumn(order -> order.getRestaurant().getName()).setHeader("Restaurant");
        orderGrid.addColumn(order -> order.getMenuItem().getName()).setHeader("Item");
        orderGrid.addColumn(Order::getQuantity).setHeader("Qty");
        orderGrid.addColumn(Order::getTotalPrice).setHeader("Total");
        orderGrid.addColumn(Order::getStatus).setHeader("Status");
        orderGrid.addColumn(Order::getCreatedAt).setHeader("Created At");

        add(new HorizontalLayout(restaurantBox, itemBox, priceField), quantityField, addressField,
                new HorizontalLayout(placeOrder, logout), orderGrid);
        refreshGrid(customer);
    }

    private void refreshGrid(User customer) {
        orderGrid.setItems(orderService.getOrdersForCustomer(customer));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!sessionService.hasRole(Role.CUSTOMER)) {
            event.forwardTo(LoginView.class);
        }
    }
}
