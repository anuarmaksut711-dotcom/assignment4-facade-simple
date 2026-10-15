import java.math.BigDecimal;
import java.util.List;

public record Order(String id, List<CartItem> items, BigDecimal total,
                    String trackingNumber, OrderStatus status) {
    public Order {
        items = List.copyOf(items);
    }
}
