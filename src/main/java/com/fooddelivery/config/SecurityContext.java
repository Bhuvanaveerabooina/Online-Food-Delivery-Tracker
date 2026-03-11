package com.fooddelivery.config;

import com.fooddelivery.model.User;
import com.vaadin.flow.server.VaadinSession;

public final class SecurityContext {
    private static final String USER_KEY = "loggedUser";

    private SecurityContext() {}

    public static void login(User user) {
        VaadinSession.getCurrent().setAttribute(USER_KEY, user);
    }

    public static User currentUser() {
        return (User) VaadinSession.getCurrent().getAttribute(USER_KEY);
    }

    public static void logout() {
        VaadinSession.getCurrent().setAttribute(USER_KEY, null);
    }
}
