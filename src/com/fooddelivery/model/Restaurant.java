package com.fooddelivery.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Restaurant implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final List<MenuItem> menuItems = new ArrayList<>();

    public Restaurant(String name, List<MenuItem> menuItems) {
        this.name = name;
        if (menuItems != null) {
            this.menuItems.addAll(menuItems);
        }
    }

    public String getName() {
        return name;
    }

    public List<MenuItem> getMenuItems() {
        return Collections.unmodifiableList(menuItems);
    }
}
