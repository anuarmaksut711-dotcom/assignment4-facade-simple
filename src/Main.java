import java.util.List;

public class Main {
    public static void main(String[] args) {
        OrderFacade shop = new OrderFacade();
        Customer customer = new Customer("Anuar", "anuar@example.com",
                "Astana, Mangilik El 55", "APPROVED");
        List<CartItem> cart = List.of(new CartItem("LAPTOP", 1), new CartItem("MOUSE", 2));

        System.out.println("1. Successful order");
        Order order = shop.placeOrder(cart, customer);
        printOrder(order);

        System.out.println("\n2. Cancel the order");
        printOrder(shop.cancelOrder(order.id()));

        System.out.println("\n3. Declined payment");
        Customer declined = new Customer("Anuar", "anuar@example.com",
                customer.address(), "DECLINED");
        demonstrateFailure(shop, cart, declined);

        System.out.println("\n4. Shipping failure after payment");
        Customer remote = new Customer("Anuar", "anuar@example.com",
                "UNSERVICEABLE", "APPROVED");
        demonstrateFailure(shop, cart, remote);

        System.out.println("\n5. Order after failed attempts (stock was restored)");
        printOrder(shop.placeOrder(List.of(new CartItem("LAPTOP", 10)), customer));
    }

    private static void demonstrateFailure(OrderFacade shop, List<CartItem> cart,
                                           Customer customer) {
        try {
            shop.placeOrder(cart, customer);
        } catch (OrderException error) {
            System.out.println(error.getMessage());
        }
    }

    private static void printOrder(Order order) {
        System.out.println("Order: " + order.id());
        System.out.println("Status: " + order.status());
        System.out.println("Total: KZT " + order.total());
        System.out.println("Tracking: " + order.trackingNumber());
    }
}
