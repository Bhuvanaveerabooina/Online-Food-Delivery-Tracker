package com.fooddelivery.ui;

import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Route("owner")
@PageTitle("Restaurant Owner Dashboard")
public class RestaurantOwnerDashboardView extends VerticalLayout implements BeforeEnterObserver {

    private final SessionService sessionService;
    private final OrderService orderService;
    private final Grid<Order> orderGrid = new Grid<>(Order.class, false);
    private List<Order> ownerOrders;

    public RestaurantOwnerDashboardView(SessionService sessionService, OrderService orderService) {
        this.sessionService = sessionService;
        this.orderService = orderService;

        User owner = sessionService.getCurrentUser();

        TextField filterField = new TextField("Search by Order ID or Customer");
        filterField.setPlaceholder("Type and press enter");
        filterField.addValueChangeListener(e -> applyFilter(e.getValue()));

        ComboBox<OrderStatus> nextStatus = new ComboBox<>("Next Status");
        nextStatus.setItems(OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP);

        Button updateStatus = new Button("Update Selected Order", e -> {
            Order selected = orderGrid.asSingleSelect().getValue();
            if (selected == null || nextStatus.isEmpty()) {
                Notification.show("Select order and status.");
                return;
            }
            try {
                orderService.updateStatus(selected, nextStatus.getValue());
                Notification.show("Status updated.");
                reload(owner);
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage());
            }
        });

        Button logout = new Button("Logout", e -> {
            sessionService.clear();
            getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        });

        orderGrid.addColumn(Order::getId).setHeader("Order ID");
        orderGrid.addColumn(order -> order.getCustomer().getUsername()).setHeader("Customer");
        orderGrid.addColumn(order -> order.getDeliveryAddress()).setHeader("Address");
        orderGrid.addColumn(order -> order.getMenuItem().getName()).setHeader("Item");
        orderGrid.addColumn(Order::getQuantity).setHeader("Qty");
        orderGrid.addColumn(Order::getStatus).setHeader("Status");

        add(new HorizontalLayout(filterField, nextStatus, updateStatus, logout), orderGrid);
        reload(owner);
    }

    private void reload(User owner) {
        ownerOrders = orderService.getOrdersForRestaurantOwner(owner);
        orderGrid.setItems(ownerOrders);
    }

    private void applyFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            orderGrid.setItems(ownerOrders);
            return;
        }
        String keyword = filter.toLowerCase(Locale.ROOT);
        List<Order> filtered = ownerOrders.stream()
                .filter(order -> String.valueOf(order.getId()).contains(keyword)
                        || order.getCustomer().getUsername().toLowerCase(Locale.ROOT).contains(keyword))
                .collect(Collectors.toList());
        orderGrid.setItems(filtered);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!sessionService.hasRole(Role.RESTAURANT_OWNER)) {
            event.forwardTo(LoginView.class);
        }
    }
}
