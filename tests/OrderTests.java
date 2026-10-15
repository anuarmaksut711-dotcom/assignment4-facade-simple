import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OrderTests {
    private static int passed;

    public static void main(String[] args) {
        successfulOrderHasCorrectTotal();
        declinedPaymentRestoresStock();
        shippingFailureRestoresStock();
        cancellationIsSafeToRepeat();
        invalidCartDoesNotChangeStock();
        duplicateItemsAreCheckedTogether();
        orderKeepsAnImmutableCartSnapshot();
        unknownCancellationFails();
        subsystemRefundAndShippingCancellationAreRepeatable();
        notificationsAreUniqueAndReadOnly();
        invalidDomainDataIsRejected();
        System.out.println("All " + passed + " checks passed.");
    }

    private static void successfulOrderHasCorrectTotal() {
        Order order = new OrderFacade().placeOrder(List.of(
                new CartItem("LAPTOP", 1), new CartItem("MOUSE", 2)), customer());
        check(order.status() == OrderStatus.PLACED, "Successful order status");
        check(order.total().compareTo(new BigDecimal("360000.00")) == 0, "Order total");
        check(order.trackingNumber().equals("AIT-" + order.id()), "Tracking number");
    }

    private static void declinedPaymentRestoresStock() {
        OrderFacade facade = new OrderFacade();
        List<CartItem> fullStock = List.of(new CartItem("LAPTOP", 10));
        Customer declined = new Customer("Anuar", "anuar@example.com", "Astana", "DECLINED");
        expectFailure(() -> facade.placeOrder(fullStock, declined), OrderException.class,
                "Payment was declined");
        check(facade.placeOrder(fullStock, customer()).status() == OrderStatus.PLACED,
                "Payment failure releases the whole reservation");
    }

    private static void shippingFailureRestoresStock() {
        OrderFacade facade = new OrderFacade();
        List<CartItem> fullStock = List.of(new CartItem("KEYBOARD", 5));
        Customer remote = new Customer("Anuar", "anuar@example.com", "UNSERVICEABLE", "APPROVED");
        expectFailure(() -> facade.placeOrder(fullStock, remote), OrderException.class,
                "Shipping is unavailable");
        check(facade.placeOrder(fullStock, customer()).status() == OrderStatus.PLACED,
                "Shipping failure releases the whole reservation");
    }

    private static void cancellationIsSafeToRepeat() {
        OrderFacade facade = new OrderFacade();
        List<CartItem> fullStock = List.of(new CartItem("LAPTOP", 10));
        Order first = facade.placeOrder(fullStock, customer());
        expectFailure(() -> facade.placeOrder(List.of(new CartItem("LAPTOP", 1)), customer()),
                OrderException.class, "Not enough stock");
        Order cancelled = facade.cancelOrder(first.id());
        check(cancelled.status() == OrderStatus.CANCELLED, "Cancellation status");
        check(facade.cancelOrder(first.id()).equals(cancelled), "Repeated cancellation result");
        facade.placeOrder(fullStock, customer());
        expectFailure(() -> facade.placeOrder(List.of(new CartItem("LAPTOP", 1)), customer()),
                OrderException.class, "Not enough stock");
    }

    private static void invalidCartDoesNotChangeStock() {
        OrderFacade facade = new OrderFacade();
        expectFailure(() -> facade.placeOrder(null, customer()), OrderException.class,
                "nonempty cart");
        expectFailure(() -> facade.placeOrder(List.of(), customer()), OrderException.class,
                "nonempty cart");
        expectFailure(() -> facade.placeOrder(Arrays.asList(new CartItem("MOUSE", 1), null),
                customer()), OrderException.class, "nonempty cart");
        expectFailure(() -> facade.placeOrder(List.of(new CartItem("MOUSE", 1)), null),
                OrderException.class, "customer");
        expectFailure(() -> facade.placeOrder(List.of(new CartItem("MOUSE", 20),
                new CartItem("UNKNOWN", 1)), customer()), OrderException.class, "Unknown product");
        check(facade.placeOrder(List.of(new CartItem("MOUSE", 20)), customer()).status()
                == OrderStatus.PLACED, "Validation failures do not reserve stock");
    }

    private static void duplicateItemsAreCheckedTogether() {
        InventoryService inventory = new InventoryService();
        expectFailure(() -> inventory.reserve("null-item", Arrays.asList(
                new CartItem("MOUSE", 1), null)), OrderException.class, "valid item");
        check(inventory.getAvailableStock("MOUSE") == 20, "Invalid direct cart keeps stock intact");
        expectFailure(() -> inventory.reserve("duplicate", List.of(
                new CartItem("KEYBOARD", 3), new CartItem("KEYBOARD", 3))),
                OrderException.class, "Not enough stock");
        check(inventory.getAvailableStock("KEYBOARD") == 5, "Duplicate cart rejection is atomic");
        expectFailure(() -> inventory.reserve("overflow", List.of(
                new CartItem("MOUSE", Integer.MAX_VALUE), new CartItem("MOUSE", 1))),
                OrderException.class, "too large");
        check(inventory.getAvailableStock("MOUSE") == 20, "Quantity overflow keeps stock intact");
        BigDecimal total = inventory.reserve("valid", List.of(
                new CartItem("MOUSE", 2), new CartItem("MOUSE", 3)));
        check(total.compareTo(new BigDecimal("25000.00")) == 0, "Duplicate quantities total");
        check(inventory.getAvailableStock("MOUSE") == 15, "Duplicate quantities reservation");
        inventory.release("valid");
        inventory.release("valid");
        check(inventory.getAvailableStock("MOUSE") == 20, "Repeated release does not add extra stock");
    }

    private static void orderKeepsAnImmutableCartSnapshot() {
        List<CartItem> cart = new ArrayList<>(List.of(new CartItem("MOUSE", 1)));
        Order order = new OrderFacade().placeOrder(cart, customer());
        cart.clear();
        check(order.items().size() == 1, "Caller cannot change the saved cart");
        expectFailure(() -> order.items().clear(), UnsupportedOperationException.class, null);
    }

    private static void unknownCancellationFails() {
        OrderFacade facade = new OrderFacade();
        expectFailure(() -> facade.cancelOrder("missing"), OrderException.class, "not found");
        expectFailure(() -> facade.cancelOrder(null), OrderException.class, "not found");
    }

    private static void subsystemRefundAndShippingCancellationAreRepeatable() {
        PaymentProcessor payment = new PaymentProcessor();
        payment.charge("direct", new BigDecimal("5000.00"), "APPROVED");
        check(payment.isCharged("direct"), "Direct payment service is usable");
        expectFailure(() -> payment.charge("direct", new BigDecimal("5000.00"), "APPROVED"),
                OrderException.class, "already been charged");
        payment.refund("direct");
        payment.refund("direct");
        check(!payment.isCharged("direct"), "Refund removes the charge and is repeatable");
        ShippingService shipping = new ShippingService();
        check(shipping.arrange("direct", customer()).equals("AIT-direct"), "Direct shipment");
        check(shipping.hasShipment("direct"), "Shipment is stored");
        shipping.cancel("direct");
        shipping.cancel("direct");
        check(!shipping.hasShipment("direct"), "Shipment cancellation is repeatable");
    }

    private static void notificationsAreUniqueAndReadOnly() {
        NotificationService notification = new NotificationService();
        Order placed = new Order("message", List.of(new CartItem("MOUSE", 1)),
                new BigDecimal("5000.00"), "AIT-message", OrderStatus.PLACED);
        notification.notifyCustomer(placed, customer());
        notification.notifyCustomer(placed, customer());
        notification.notifyCustomer(new Order(placed.id(), placed.items(), placed.total(),
                placed.trackingNumber(), OrderStatus.CANCELLED), customer());
        check(notification.getMessages().size() == 2, "One notification per order status");
        check(notification.getMessages().get(0).contains("anuar@example.com"), "Notification recipient");
        check(notification.getMessages().get(1).contains("refunded"), "Cancellation details");
        expectFailure(() -> notification.getMessages().clear(), UnsupportedOperationException.class, null);
    }

    private static void invalidDomainDataIsRejected() {
        expectFailure(() -> new CartItem("MOUSE", 0), IllegalArgumentException.class, "positive");
        expectFailure(() -> new CartItem(" ", 1), IllegalArgumentException.class, "required");
        expectFailure(() -> new Customer("Anuar", " ", "Astana", "APPROVED"),
                IllegalArgumentException.class, "blank");
    }

    private static Customer customer() {
        return new Customer("Anuar", "anuar@example.com", "Astana", "APPROVED");
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        passed++;
    }

    private static void expectFailure(Runnable action, Class<? extends RuntimeException> type,
                                      String messagePart) {
        try {
            action.run();
        } catch (RuntimeException error) {
            check(type.isInstance(error), "Expected " + type.getSimpleName() + ", got " + error);
            if (messagePart != null) {
                check(error.getMessage().contains(messagePart), "Expected error text: " + messagePart);
            }
            return;
        }
        throw new AssertionError("Expected " + type.getSimpleName() + " but no exception was thrown.");
    }
}
