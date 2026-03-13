package com.fooddelivery.view;

import com.fooddelivery.config.SecurityContext;
import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.Role;
import com.fooddelivery.model.User;
import com.fooddelivery.repo.MenuItemRepository;
import com.fooddelivery.repo.OrderRepository;
import com.fooddelivery.repo.RestaurantRepository;
import com.fooddelivery.service.OrderService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route("dashboard")
@PageTitle("Dashboard | Food Delivery Tracker")
public class DashboardView extends VerticalLayout implements BeforeEnterObserver {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public DashboardView(RestaurantRepository restaurantRepository,
                         MenuItemRepository menuItemRepository,
                         OrderRepository orderRepository,
                         OrderService orderService) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;

        setPadding(true);
        setSpacing(true);
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

        add(new HorizontalLayout(new H2("Welcome, " + user.getUsername()), logout));

        if (user.getRole() == Role.CUSTOMER) {
            buildCustomerDashboard(user);
        } else if (user.getRole() == Role.RESTAURANT_OWNER) {
            buildRestaurantOwnerDashboard(user);
        } else {
            buildDeliveryDashboard();
        }
    }

    private void buildCustomerDashboard(User customer) {
        ComboBox<Restaurant> restaurantBox = new ComboBox<>("Restaurant");
        restaurantBox.setItems(restaurantRepository.findAll());
        restaurantBox.setItemLabelGenerator(Restaurant::getName);

        ComboBox<MenuItem> itemBox = new ComboBox<>("Item");
        itemBox.setItemLabelGenerator(item -> item.getName() + " (₹" + item.getPrice() + ")");

        TextField priceField = new TextField("Item Price");
        priceField.setReadOnly(true);

        restaurantBox.addValueChangeListener(e -> {
            Restaurant selected = e.getValue();
            itemBox.setItems(selected == null ? List.of() : menuItemRepository.findByRestaurantOrderByName(selected));
            itemBox.clear();
            priceField.clear();
        });

        itemBox.addValueChangeListener(e -> {
            MenuItem item = e.getValue();
            priceField.setValue(item == null ? "" : String.valueOf(item.getPrice()));
        });

        IntegerField quantity = new IntegerField("Quantity");
        quantity.setMin(1);
        quantity.setValue(1);

        TextArea address = new TextArea("Delivery Address");

        Button placeOrder = new Button("Place Order", e -> {
            try {
                if (restaurantBox.isEmpty() || itemBox.isEmpty()) {
                    Notification.show("Select restaurant and item.");
                    return;
                }
                orderService.placeOrder(customer, restaurantBox.getValue(), itemBox.getValue(), quantity.getValue(), address.getValue());
                Notification.show("Order placed successfully.");
                getUI().ifPresent(ui -> ui.getPage().reload());
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        Grid<Order> history = new Grid<>(Order.class, false);
        history.addColumn(Order::getOrderId).setHeader("Order ID");
        history.addColumn(o -> o.getRestaurant().getName()).setHeader("Restaurant");
        history.addColumn(o -> o.getMenuItem().getName()).setHeader("Item");
        history.addColumn(Order::getQuantity).setHeader("Qty");
        history.addColumn(Order::getDeliveryAddress).setHeader("Address");
        history.addColumn(o -> o.getStatus().name()).setHeader("Delivery Status");
        history.setItems(orderRepository.findByCustomerOrderByCreatedAtDesc(customer));

        add(restaurantBox, itemBox, priceField, quantity, address, placeOrder, new H2("My Orders & Delivery Status"), history);
    }

    private void buildRestaurantOwnerDashboard(User owner) {
        TextField filter = new TextField("Filter by Customer Name or Order ID");
        Grid<Order> grid = new Grid<>(Order.class, false);
        grid.addColumn(Order::getOrderId).setHeader("Order ID");
        grid.addColumn(o -> o.getCustomer().getUsername()).setHeader("Customer");
        grid.addColumn(Order::getDeliveryAddress).setHeader("Address");
        grid.addColumn(o -> o.getMenuItem().getName()).setHeader("Item");
        grid.addColumn(Order::getQuantity).setHeader("Qty");
        grid.addColumn(o -> o.getStatus().name()).setHeader("Status");

        grid.addComponentColumn(order -> {
            ComboBox<OrderStatus> status = new ComboBox<>();
            status.setItems(OrderStatus.PLACED, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY);
            status.setValue(order.getStatus());
            Button update = new Button("Update", e -> {
                orderService.updateStatus(order, status.getValue());
                Notification.show("Status updated.");
                getUI().ifPresent(ui -> ui.getPage().reload());
            });
            return new HorizontalLayout(status, update);
        }).setHeader("Restaurant Action");

        List<Order> ownerOrders = orderRepository.findByRestaurantOrderByCreatedAtDesc(owner.getRestaurant());
        grid.setItems(ownerOrders);

        filter.addValueChangeListener(e -> {
            String q = e.getValue() == null ? "" : e.getValue().toLowerCase();
            grid.setItems(ownerOrders.stream()
                    .filter(o -> o.getOrderId().toLowerCase().contains(q)
                            || o.getCustomer().getUsername().toLowerCase().contains(q))
                    .toList());
        });

        add(filter, grid);
    }

    private void buildDeliveryDashboard() {
        Grid<Order> grid = new Grid<>(Order.class, false);
        grid.addColumn(Order::getOrderId).setHeader("Order ID");
        grid.addColumn(o -> o.getCustomer().getUsername()).setHeader("Customer");
        grid.addColumn(Order::getDeliveryAddress).setHeader("Address");
        grid.addColumn(o -> o.getRestaurant().getName()).setHeader("Restaurant");
        grid.addColumn(o -> o.getStatus().name()).setHeader("Status");
        grid.addComponentColumn(order -> new Button("Mark Delivered", e -> {
            orderService.updateStatus(order, OrderStatus.DELIVERED);
            Notification.show("Order marked delivered.");
            getUI().ifPresent(ui -> ui.getPage().reload());
        })).setHeader("Action");

        grid.setItems(orderService.deliveryOrders());
        add(grid);
    }
}
