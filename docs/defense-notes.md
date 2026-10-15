# Defense notes

## Walkthrough

1. Explain the order processing problem.
2. Show the UML diagram.
3. Show the two public methods in `OrderFacade`.
4. Show the four service fields and the placement sequence.
5. Show inventory validation before stock changes.
6. Show the rollback method and repeated cancellation check.
7. Run the console demo.
8. Run the tests.

## Live demonstration

Run `Main` from the compiled `out` folder.

| Step | Expected result |
| --- | --- |
| Place one laptop and two mice | `PLACED`, total KZT 360000.00, tracking number. |
| Cancel that order | `CANCELLED`; the original total remains in the order record. |
| Use token `DECLINED` | Payment error; reserved stock is released. |
| Use address `UNSERVICEABLE` | Shipping error after payment; payment is refunded and stock is released. |
| Order ten laptops | Success; the earlier attempts did not leave stock reserved. |

Run `OrderTests` after the demo. Explain that each test creates its own state where needed. The test runner does not depend on the `-ea` option.

## Questions and answers

**Why use a facade?**

The client would otherwise need four services and knowledge of their execution order. The facade centralizes that coordination.

**Why is this more than a wrapper around one call?**

It coordinates stock reservation, payment, shipping, and notification. It also reverses completed steps after failure.

**What does the client know?**

It knows the facade and domain values. It receives an order result or an order error.

**Can another client use a subsystem directly?**

Yes. The subsystem classes and their methods are public. None refers to `OrderFacade`. The tests demonstrate direct use.

**What happens when payment fails?**

The facade calls rollback. There is no shipment or charge to remove. Inventory releases the reservation.

**What happens when shipping fails?**

The payment has already succeeded. Rollback refunds it and releases the stock reservation.

**Why does rollback call all three methods?**

Each method safely handles a missing record. This keeps the reverse sequence in one place.

**What happens if cancellation is repeated?**

The facade detects `CANCELLED` and returns the saved order. It does not restore stock twice.

**Why combine duplicate product entries?**

Separate entries can exceed stock when added together. Inventory checks their combined quantity before it changes stock.

**Why use records and `List.copyOf`?**

Records keep domain values simple. The copied list prevents a caller from changing saved order items.

**Why use `BigDecimal`?**

It stores decimal money values without binary floating-point rounding errors. Prices are constructed from decimal strings.

**How are notifications sent?**

They are prepared in an in-memory outbox. The program does not send email. Duplicate order status events produce no extra message.

**What are the limitations?**

State is temporary, and each facade owns separate service state. The program uses one thread. All placed orders can be cancelled.

**Would this recovery work unchanged with real services?**

No. Real refunds or cancellations can fail. A production system needs persistent recovery records, retry rules, and external operation identifiers.

**Is a facade the same as an adapter?**

No. An adapter changes an interface to fit another expected interface. This facade simplifies access to several existing services.
