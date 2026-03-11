package com.fooddelivery.ui;

import com.fooddelivery.model.Role;
import com.fooddelivery.model.User;
import com.fooddelivery.service.AuthService;
import com.fooddelivery.service.SessionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("")
@PageTitle("Login | Online Food Delivery Tracker")
public class LoginView extends VerticalLayout {

    public LoginView(AuthService authService, SessionService sessionService) {
        setWidthFull();
        setAlignItems(Alignment.CENTER);

        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        ComboBox<Role> role = new ComboBox<>("Role");
        role.setItems(Role.values());

        Button login = new Button("Login", event -> {
            if (username.isEmpty() || password.isEmpty() || role.isEmpty()) {
                Notification.show("All fields are required.");
                return;
            }
            try {
                User user = authService.login(username.getValue().trim(), password.getValue(), role.getValue());
                sessionService.setCurrentUser(user);
                navigateByRole(user.getRole());
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage());
            }
        });

        add(username, password, role, login);
    }

    private void navigateByRole(Role role) {
        if (role == Role.CUSTOMER) {
            getUI().ifPresent(ui -> ui.navigate(CustomerDashboardView.class));
        } else if (role == Role.RESTAURANT_OWNER) {
            getUI().ifPresent(ui -> ui.navigate(RestaurantOwnerDashboardView.class));
        } else {
            getUI().ifPresent(ui -> ui.navigate(DeliveryDashboardView.class));
        }
    }
}
