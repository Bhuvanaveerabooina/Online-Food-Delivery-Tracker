package com.fooddelivery.ui;

import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.model.Role;
import com.fooddelivery.service.OrderService;
import com.fooddelivery.service.SessionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;

@Route("delivery")
@PageTitle("Delivery Dashboard")
public class DeliveryDashboardView extends VerticalLayout implements BeforeEnterObserver {

    private final SessionService sessionService;
    private final OrderService orderService;
    private final Grid<Order> orderGrid = new Grid<>(Order.class, false);

    public DeliveryDashboardView(SessionService sessionService, OrderService orderService) {
        this.sessionService = sessionService;
        this.orderService = orderService;

        Button markOutForDelivery = new Button("Start Delivery", e -> updateSelected(OrderStatus.OUT_FOR_DELIVERY));
        Button markDelivered = new Button("Mark Delivered", e -> updateSelected(OrderStatus.DELIVERED));
        Button logout = new Button("Logout", e -> {
            sessionService.clear();
            getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        });

        orderGrid.addColumn(Order::getId).setHeader("Order ID");
        orderGrid.addColumn(order -> order.getRestaurant().getName()).setHeader("Restaurant");
        orderGrid.addColumn(order -> order.getCustomer().getUsername()).setHeader("Customer");
        orderGrid.addColumn(Order::getDeliveryAddress).setHeader("Address");
        orderGrid.addColumn(Order::getStatus).setHeader("Status");

        add(new HorizontalLayout(markOutForDelivery, markDelivered, logout), orderGrid);
        refresh();
    }

    private void updateSelected(OrderStatus nextStatus) {
        Order selected = orderGrid.asSingleSelect().getValue();
        if (selected == null) {
            Notification.show("Select an order first.");
            return;
        }
        try {
            orderService.updateStatus(selected, nextStatus);
            Notification.show("Status updated.");
            refresh();
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void refresh() {
        orderGrid.setItems(orderService.getDeliveryOrders());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!sessionService.hasRole(Role.DELIVERY_PERSON)) {
            event.forwardTo(LoginView.class);
        }
    }
}
