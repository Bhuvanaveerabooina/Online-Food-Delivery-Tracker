package com.fooddelivery.view;

import com.fooddelivery.config.SecurityContext;
import com.fooddelivery.model.Role;
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
@PageTitle("Login | Online Food Delivery Tracker")
public class LoginView extends VerticalLayout {
    public LoginView(AuthService authService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        ComboBox<Role> role = new ComboBox<>("Role");
        role.setItems(Role.values());
        role.setRequired(true);

        Button loginButton = new Button("Login", event -> {
            if (role.getValue() == null) {
                Notification.show("Please select a role.");
                return;
            }
            authService.authenticate(username.getValue(), password.getValue(), role.getValue())
                    .ifPresentOrElse(user -> {
                        SecurityContext.login(user);
                        getUI().ifPresent(ui -> ui.navigate("dashboard"));
                    }, () -> Notification.show("Invalid username/password/role."));
        });

        add(username, password, role, loginButton);
        setWidth("100%");
        setMaxWidth("420px");
    }
}
