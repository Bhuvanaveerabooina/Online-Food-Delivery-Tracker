package com.fooddelivery.config;

import com.fooddelivery.model.User;

public final class SecurityContext {
    private static User currentUser;

    private SecurityContext() {
    }

    public static void login(User user) {
        currentUser = user;
    }

    public static User currentUser() {
        return currentUser;
    }

    public static void logout() {
        currentUser = null;
    }
}
