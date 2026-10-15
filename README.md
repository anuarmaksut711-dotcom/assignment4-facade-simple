# Assignment 4 Facade Pattern

This Java 17 console program places and cancels orders through OrderFacade.
Four services manage stock, payment, shipping, and customer messages.
All services use in-memory data.

## Files

- `src/` - all Java classes, with no package declarations
- `tests/OrderTests.java` - 61 checks
- `docs/` - UML diagram, report source, and defense notes
- `report.pdf` - report

Start the program from `src/Main.java`.

## Build and run

Use JDK 17 or newer. Open a terminal in this folder.

```text
javac --release 17 -d out src/*.java tests/*.java
java -cp out Main
java -cp out OrderTests
```

In IntelliJ IDEA, open the folder and select JDK 17.
Mark `src` as Sources Root. Mark `tests` as Test Sources Root.
Run `Main.main` for the demo or `OrderTests.main` for the checks.

## Demo cases

The demo shows a successful order and its cancellation.
It then shows payment failure, shipping failure, and an order after stock recovery.
The payment token `DECLINED` selects payment failure.
The address `UNSERVICEABLE` selects shipping failure after payment.

| Product | Stock | Unit price in KZT |
| --- | ---: | ---: |
| LAPTOP | 10 | 350000.00 |
| MOUSE | 20 | 5000.00 |
| KEYBOARD | 5 | 15000.00 |

## Design

Main uses only OrderFacade and the data classes.
OrderFacade has two public methods: `placeOrder` and `cancelOrder`.
The services have no reference to OrderFacade. Other clients can use them directly.
A failed payment releases stock. A failed shipment also refunds the payment.
Repeated cancellation does not restore stock twice.

This is a single-thread simulation. It sends no real payments, shipments, or email.
All state is lost when the program ends. Recovery operations cannot fail in this simulation.

## Documents

- [Report](report.pdf)
- [UML diagram](docs/class-diagram.png)
- [UML source](docs/class-diagram.puml)
- [Defense notes](docs/defense-notes.md)

The diagram uses UML 2.5.1 notation and PlantUML 1.2026.8.

Repository: https://github.com/anuarmaksut711-dotcom/assignment4-facade-simple
