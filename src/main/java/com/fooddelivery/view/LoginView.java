package com.fooddelivery.view;

import com.fooddelivery.config.SecurityContext;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.Role;
import com.fooddelivery.repo.RestaurantRepository;
import com.fooddelivery.service.AuthService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("")
@PageTitle("Login | Food Delivery Tracker")
public class LoginView extends VerticalLayout {
    public LoginView(AuthService authService, RestaurantRepository restaurantRepository) {
        setWidth("360px");
        setSpacing(true);
        setPadding(true);
        setAlignSelf(Alignment.CENTER);

        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        ComboBox<Role> role = new ComboBox<>("Role");
        role.setItems(Role.values());

        ComboBox<Restaurant> ownerRestaurant = new ComboBox<>("Restaurant (for owner registration)");
        ownerRestaurant.setItems(restaurantRepository.findAll());
        ownerRestaurant.setItemLabelGenerator(Restaurant::getName);

        Button login = new Button("Login", e -> {
            if (username.isBlank() || password.isEmpty() || role.isEmpty()) {
                Notification.show("Enter username, password and role.");
                return;
            }
            authService.authenticate(username.getValue(), password.getValue(), role.getValue())
                    .ifPresentOrElse(user -> {
                        SecurityContext.login(user);
                        getUI().ifPresent(ui -> ui.navigate("dashboard"));
                    }, () -> Notification.show("Invalid username/password/role."));
        });

        Button register = new Button("Register", e -> {
            if (username.isBlank() || password.isEmpty() || role.isEmpty()) {
                Notification.show("Enter username, password and role.");
                return;
            }
            try {
                authService.register(username.getValue(), password.getValue(), role.getValue(), ownerRestaurant.getValue());
                Notification.show("Registration successful. You can now login.");
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage());
            }
        });

        add(username, password, role, ownerRestaurant, login, register);
    }
}
