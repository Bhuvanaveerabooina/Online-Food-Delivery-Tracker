package com.fooddelivery.service;

import com.fooddelivery.model.Role;
import com.fooddelivery.model.User;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private static final String CURRENT_USER = "currentUser";

    public void setCurrentUser(User user) {
        VaadinSession.getCurrent().setAttribute(CURRENT_USER, user);
    }

    public User getCurrentUser() {
        return (User) VaadinSession.getCurrent().getAttribute(CURRENT_USER);
    }

    public void clear() {
        VaadinSession.getCurrent().setAttribute(CURRENT_USER, null);
    }

    public boolean hasRole(Role role) {
        User user = getCurrentUser();
        return user != null && user.getRole() == role;
    }
}
