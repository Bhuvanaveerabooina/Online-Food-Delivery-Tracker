package com.fooddelivery.config;

import com.fooddelivery.model.User;
import com.vaadin.flow.server.VaadinSession;

public final class SecurityContext {
    private static final String KEY = "loggedInUser";

    private SecurityContext() {
    }

    public static void login(User user) {
        VaadinSession.getCurrent().setAttribute(KEY, user);
    }

    public static User currentUser() {
        return (User) VaadinSession.getCurrent().getAttribute(KEY);
    }

    public static void logout() {
        VaadinSession.getCurrent().setAttribute(KEY, null);
    }
}
