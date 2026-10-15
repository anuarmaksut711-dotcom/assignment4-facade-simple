public record Customer(String name, String email, String address, String paymentToken) {
    public Customer {
        if (name == null || name.isBlank() || email == null || email.isBlank()
                || address == null || address.isBlank()
                || paymentToken == null || paymentToken.isBlank()) {
            throw new IllegalArgumentException("Customer details must not be blank.");
        }
        name = name.trim();
        email = email.trim();
        address = address.trim();
        paymentToken = paymentToken.trim();
    }
}
