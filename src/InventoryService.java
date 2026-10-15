import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventoryService {
    private final Map<String, Integer> stock = new HashMap<>(Map.of(
            "LAPTOP", 10, "MOUSE", 20, "KEYBOARD", 5));
    private final Map<String, BigDecimal> prices = Map.of(
            "LAPTOP", new BigDecimal("350000.00"),
            "MOUSE", new BigDecimal("5000.00"),
            "KEYBOARD", new BigDecimal("15000.00"));
    private final Map<String, Map<String, Integer>> reservations = new HashMap<>();

    public BigDecimal reserve(String orderId, List<CartItem> items) {
        if (orderId == null || orderId.isBlank() || reservations.containsKey(orderId)) {
            throw new OrderException("Order ID must be new and nonblank.");
        }
        if (items == null || items.isEmpty() || items.stream().anyMatch(item -> item == null)) {
            throw new OrderException("The cart must contain at least one valid item.");
        }

        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (CartItem item : items) {
            if (!prices.containsKey(item.productId())) {
                throw new OrderException("Unknown product: " + item.productId());
            }
            try {
                quantities.merge(item.productId(), item.quantity(), Math::addExact);
            } catch (ArithmeticException error) {
                throw new OrderException("Total item quantity is too large.", error);
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> item : quantities.entrySet()) {
            if (stock.get(item.getKey()) < item.getValue()) {
                throw new OrderException("Not enough stock for " + item.getKey());
            }
            total = total.add(prices.get(item.getKey())
                    .multiply(BigDecimal.valueOf(item.getValue())));
        }

        // All cart entries are checked before any stock is changed.
        for (Map.Entry<String, Integer> item : quantities.entrySet()) {
            stock.put(item.getKey(), stock.get(item.getKey()) - item.getValue());
        }
        reservations.put(orderId, quantities);
        return total;
    }

    public void release(String orderId) {
        Map<String, Integer> quantities = reservations.remove(orderId);
        if (quantities == null) {
            return;
        }
        for (Map.Entry<String, Integer> item : quantities.entrySet()) {
            stock.put(item.getKey(), stock.get(item.getKey()) + item.getValue());
        }
    }

    public int getAvailableStock(String productId) {
        if (!stock.containsKey(productId)) {
            throw new OrderException("Unknown product: " + productId);
        }
        return stock.get(productId);
    }
}
