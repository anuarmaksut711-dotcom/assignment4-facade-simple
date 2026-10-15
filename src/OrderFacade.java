import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OrderFacade {
    private final InventoryService inventory = new InventoryService();
    private final PaymentProcessor payment = new PaymentProcessor();
    private final ShippingService shipping = new ShippingService();
    private final NotificationService notification = new NotificationService();
    private final Map<String, OrderDetails> orders = new HashMap<>();

    public Order placeOrder(List<CartItem> cart, Customer customer) {
        if (customer == null || cart == null || cart.isEmpty()
                || cart.stream().anyMatch(item -> item == null)) {
            throw new OrderException("A customer and a nonempty cart are required.");
        }
        List<CartItem> items = List.copyOf(cart);
        String orderId = UUID.randomUUID().toString();
        BigDecimal total;
        String trackingNumber;

        try {
            total = inventory.reserve(orderId, items);
            payment.charge(orderId, total, customer.paymentToken());
            trackingNumber = shipping.arrange(orderId, customer);
        } catch (OrderException error) {
            rollback(orderId);
            throw new OrderException("Order could not be placed: " + error.getMessage(), error);
        }

        Order order = new Order(orderId, items, total, trackingNumber, OrderStatus.PLACED);
        orders.put(orderId, new OrderDetails(order, customer));
        notification.notifyCustomer(order, customer);
        return order;
    }

    public Order cancelOrder(String orderId) {
        OrderDetails details = orders.get(orderId);
        if (details == null) {
            throw new OrderException("Order was not found: " + orderId);
        }
        if (details.order().status() == OrderStatus.CANCELLED) {
            return details.order();
        }

        rollback(orderId);
        Order oldOrder = details.order();
        Order cancelledOrder = new Order(oldOrder.id(), oldOrder.items(), oldOrder.total(),
                oldOrder.trackingNumber(), OrderStatus.CANCELLED);
        orders.put(orderId, new OrderDetails(cancelledOrder, details.customer()));
        notification.notifyCustomer(cancelledOrder, details.customer());
        return cancelledOrder;
    }

    private void rollback(String orderId) {
        shipping.cancel(orderId);
        payment.refund(orderId);
        inventory.release(orderId);
    }

    private record OrderDetails(Order order, Customer customer) { }
}
