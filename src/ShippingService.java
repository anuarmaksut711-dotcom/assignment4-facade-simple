import java.util.HashMap;
import java.util.Map;

public class ShippingService {
    private final Map<String, String> shipments = new HashMap<>();

    public String arrange(String orderId, Customer customer) {
        if (orderId == null || orderId.isBlank() || customer == null) {
            throw new OrderException("Shipping details are invalid.");
        }
        if (shipments.containsKey(orderId)) {
            throw new OrderException("Shipping is already arranged for this order.");
        }
        if ("UNSERVICEABLE".equalsIgnoreCase(customer.address())) {
            throw new OrderException("Shipping is unavailable for this address.");
        }
        String trackingNumber = "AIT-" + orderId;
        shipments.put(orderId, trackingNumber);
        return trackingNumber;
    }

    public void cancel(String orderId) {
        shipments.remove(orderId);
    }

    public boolean hasShipment(String orderId) {
        return shipments.containsKey(orderId);
    }
}
