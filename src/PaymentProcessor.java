import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class PaymentProcessor {
    private final Map<String, BigDecimal> charges = new HashMap<>();

    public void charge(String orderId, BigDecimal amount, String paymentToken) {
        if (orderId == null || orderId.isBlank() || amount == null
                || amount.signum() <= 0 || paymentToken == null || paymentToken.isBlank()) {
            throw new OrderException("Payment details are invalid.");
        }
        if (charges.containsKey(orderId)) {
            throw new OrderException("This order has already been charged.");
        }
        if ("DECLINED".equalsIgnoreCase(paymentToken)) {
            throw new OrderException("Payment was declined.");
        }
        charges.put(orderId, amount);
    }

    public void refund(String orderId) {
        // Removing an absent charge is safe when compensation is repeated.
        charges.remove(orderId);
    }

    public boolean isCharged(String orderId) {
        return charges.containsKey(orderId);
    }
}
