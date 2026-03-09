package com.fooddelivery.model;

/**
 * Represents the lifecycle of an order.
 */
public enum OrderStatus {
    PLACED,
    PREPARING,
    OUT_FOR_DELIVERY,
    AWAITING_CUSTOMER_VERIFICATION,
    DELIVERED
}
