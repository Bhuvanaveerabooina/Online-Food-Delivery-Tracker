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

    /**
     * Keeps the ID counter in sync with saved history.
     * Example: if max order is ORD1050, next generated ID should be ORD1051.
     */
    public static void syncCounterFromExistingId(String orderId) {
        if (orderId == null || !orderId.toUpperCase().startsWith("ORD")) {
            return;
        }

        try {
            int numericPart = Integer.parseInt(orderId.substring(3));
            int nextValue = numericPart + 1;

            // Update only if the stored value is greater than current counter.
            COUNTER.updateAndGet(current -> Math.max(current, nextValue));
        } catch (NumberFormatException ignored) {
            // Ignore malformed IDs and keep current counter.
        }
    }
}
