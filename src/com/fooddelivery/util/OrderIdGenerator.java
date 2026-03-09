package com.fooddelivery.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class to generate unique order IDs.
 */
public final class OrderIdGenerator {
    private static final AtomicInteger COUNTER = new AtomicInteger(1000);

    private OrderIdGenerator() {
        // Prevent object creation.
    }

    public static String generateOrderId() {
        return "ORD" + COUNTER.getAndIncrement();
    }
}
