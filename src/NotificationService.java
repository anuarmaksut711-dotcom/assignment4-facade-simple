import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationService {
    private final List<String> outbox = new ArrayList<>();
    private final Set<String> sentEvents = new HashSet<>();

    public void notifyCustomer(Order order, Customer customer) {
        String eventKey = order.id() + ":" + order.status();
        if (!sentEvents.add(eventKey)) {
            return;
        }
        String details = order.status() == OrderStatus.PLACED
                ? "placed, total KZT " + order.total() + ", tracking " + order.trackingNumber()
                : "cancelled, payment refunded and stock released";
        outbox.add("To: " + customer.email() + "; Hello " + customer.name()
                + ", order " + order.id() + " is " + details + ".");
    }

    public List<String> getMessages() {
        return List.copyOf(outbox);
    }
}
