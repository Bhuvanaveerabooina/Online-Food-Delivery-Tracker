package com.fooddelivery.view;

import com.fooddelivery.config.SecurityContext;
import com.fooddelivery.model.*;
import com.fooddelivery.repo.MenuItemRepository;
import com.fooddelivery.repo.RestaurantRepository;
import com.fooddelivery.service.OrderService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route("dashboard")
@PageTitle("Dashboard | Online Food Delivery Tracker")
public class DashboardView extends VerticalLayout implements BeforeEnterObserver {

    private final OrderService orderService;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public DashboardView(OrderService orderService,
                         RestaurantRepository restaurantRepository,
                         MenuItemRepository menuItemRepository) {
        this.orderService = orderService;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        User user = SecurityContext.currentUser();
        if (user == null) {
            event.forwardTo("");
            return;
        }

        Button logout = new Button("Logout", e -> {
            SecurityContext.logout();
            getUI().ifPresent(ui -> ui.navigate(""));
        });

        add(new H2("Welcome, " + user.getUsername()), new Paragraph("Role: " + user.getRole()), logout);

        switch (user.getRole()) {
            case CUSTOMER -> add(buildCustomerSection(user));
            case RESTAURANT_OWNER -> add(buildOwnerSection(user));
            case DELIVERY_PERSON -> add(buildDeliverySection());
        }
    }

    private VerticalLayout buildCustomerSection(User user) {
        VerticalLayout section = new VerticalLayout();
        section.add(new H3("Customer Dashboard"));

        ComboBox<Restaurant> restaurantBox = new ComboBox<>("Restaurant");
        restaurantBox.setItems(restaurantRepository.findAll());
        restaurantBox.setItemLabelGenerator(Restaurant::getName);

        ComboBox<MenuItem> itemBox = new ComboBox<>("Item");
        itemBox.setItemLabelGenerator(MenuItem::getName);

        TextField priceField = new TextField("Item Price");
        priceField.setReadOnly(true);

        IntegerField quantity = new IntegerField("Quantity");
        quantity.setMin(1);
        quantity.setValue(1);

        TextField address = new TextField("Delivery Address");

        restaurantBox.addValueChangeListener(e -> {
            Restaurant restaurant = e.getValue();
            itemBox.clear();
            priceField.clear();
            itemBox.setItems(restaurant == null ? List.of() : menuItemRepository.findByRestaurant(restaurant));
        });

        itemBox.addValueChangeListener(e -> {
            MenuItem item = e.getValue();
            priceField.setValue(item == null ? "" : String.valueOf(item.getPrice()));
        });

        Grid<Order> historyGrid = orderGrid();
        Runnable refresh = () -> historyGrid.setItems(orderService.customerOrders(user));

        Button placeOrder = new Button("Place Order", e -> {
            try {
                if (restaurantBox.getValue() == null || itemBox.getValue() == null || address.getValue().isBlank()) {
                    throw new IllegalArgumentException("Please fill all order fields.");
                }
                Order order = orderService.placeOrder(
                        user,
                        restaurantBox.getValue(),
                        itemBox.getValue(),
                        quantity.getValue() == null ? 0 : quantity.getValue(),
                        address.getValue().trim()
                );
                Notification.show("Order placed: " + order.getOrderId());
                refresh.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        refresh.run();
        section.add(restaurantBox, itemBox, priceField, quantity, address, placeOrder, new H3("My Orders"), historyGrid);
        return section;
    }

    private VerticalLayout buildOwnerSection(User user) {
        VerticalLayout section = new VerticalLayout();
        section.add(new H3("Restaurant Owner Dashboard"));

        TextField search = new TextField("Search by Customer Name or Order ID");
        Grid<Order> grid = orderGrid();
        ComboBox<OrderStatus> statusBox = new ComboBox<>("Update Status");
        statusBox.setItems(OrderStatus.PLACED, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP);

        Button updateStatus = new Button("Apply Status", e -> {
            Order selected = grid.asSingleSelect().getValue();
            if (selected == null || statusBox.getValue() == null) {
                Notification.show("Select an order and status first.");
                return;
            }
            orderService.updateStatus(selected, statusBox.getValue());
            refreshOwnerOrders(user, search.getValue(), grid);
            Notification.show("Status updated.");
        });

        search.addValueChangeListener(e -> refreshOwnerOrders(user, e.getValue(), grid));
        refreshOwnerOrders(user, "", grid);

        section.add(new Paragraph("Showing orders for: " + user.getRestaurant().getName()), search, grid, statusBox, updateStatus);
        return section;
    }

    private void refreshOwnerOrders(User user, String searchTerm, Grid<Order> grid) {
        String query = searchTerm == null ? "" : searchTerm.trim().toLowerCase();
        List<Order> filtered = orderService.restaurantOrders(user.getRestaurant()).stream()
                .filter(o -> query.isBlank()
                        || o.getCustomer().getUsername().toLowerCase().contains(query)
                        || o.getOrderId().toLowerCase().contains(query))
                .toList();
        grid.setItems(filtered);
    }

    private VerticalLayout buildDeliverySection() {
        VerticalLayout section = new VerticalLayout();
        section.add(new H3("Delivery Person Dashboard"));

        Grid<Order> grid = orderGrid();
        Runnable refresh = () -> grid.setItems(orderService.deliveryOrders());

        Button outForDelivery = new Button("Mark OUT_FOR_DELIVERY", e -> {
            Order selected = grid.asSingleSelect().getValue();
            if (selected == null) {
                Notification.show("Select an order first.");
                return;
            }
            orderService.updateStatus(selected, OrderStatus.OUT_FOR_DELIVERY);
            refresh.run();
        });

        Button delivered = new Button("Mark DELIVERED", e -> {
            Order selected = grid.asSingleSelect().getValue();
            if (selected == null) {
                Notification.show("Select an order first.");
                return;
            }
            orderService.updateStatus(selected, OrderStatus.DELIVERED);
            refresh.run();
        });

        refresh.run();
        section.add(grid, new HorizontalLayout(outForDelivery, delivered));
        return section;
    }

    private Grid<Order> orderGrid() {
        Grid<Order> grid = new Grid<>(Order.class, false);
        grid.addColumn(Order::getOrderId).setHeader("Order ID");
        grid.addColumn(order -> order.getCustomer().getUsername()).setHeader("Customer");
        grid.addColumn(order -> order.getRestaurant().getName()).setHeader("Restaurant");
        grid.addColumn(order -> order.getMenuItem().getName()).setHeader("Item");
        grid.addColumn(Order::getItemPrice).setHeader("Price");
        grid.addColumn(Order::getQuantity).setHeader("Quantity");
        grid.addColumn(Order::getDeliveryAddress).setHeader("Address");
        grid.addColumn(order -> order.getStatus().name()).setHeader("Status");
        return grid;
    }
}
