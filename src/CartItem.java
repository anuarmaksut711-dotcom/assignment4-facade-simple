public record CartItem(String productId, int quantity) {
    public CartItem {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID is required.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        productId = productId.trim();
    }
}
