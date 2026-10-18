package com.ankit.orderservice;

public final class OrderExceptions {
    private OrderExceptions() {}
    public static class NotFound extends RuntimeException { public NotFound(Long id) { super("Order #" + id + " not found"); } }
    public static class InvalidTransition extends RuntimeException { public InvalidTransition(String message) { super(message); } }
    public static class Unauthorized extends RuntimeException { public Unauthorized() { super("Admin login is missing or expired"); } }
    public static class AdminDisabled extends RuntimeException { public AdminDisabled() { super("Admin login is not configured on this server"); } }
    public static class CsrfRejected extends RuntimeException { public CsrfRejected() { super("Security token is missing or invalid. Refresh and try again."); } }
    public static class RateLimited extends RuntimeException { public RateLimited() { super("Too many login attempts. Try again in 15 minutes."); } }
    public static class InvalidBulkRequest extends RuntimeException { public InvalidBulkRequest() { super("Select between 1 and 500 orders."); } }
}
