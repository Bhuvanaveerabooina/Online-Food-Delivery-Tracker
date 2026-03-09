# Online Food Delivery Tracker (Core Java Console App)

A beginner-friendly **menu-driven console application** to manage online food delivery orders.

## Project Structure

```text
Online-Food-Delivery-Tracker/
├── src/
│   └── com/
│       └── fooddelivery/
│           ├── app/
│           │   └── OnlineFoodDeliveryTracker.java
│           ├── model/
│           │   ├── Order.java
│           │   └── OrderStatus.java
│           ├── service/
│           │   ├── OrderOperations.java
│           │   └── OrderService.java
│           └── util/
│               └── OrderIdGenerator.java
└── README.md
```

## Concepts Used

- OOP (classes/objects)
- Encapsulation (private fields with getters/setters)
- Abstraction (service interface)
- Interface-based design (`OrderOperations`)
- Collections (`ArrayList`)
- Exception handling (`try-catch`, input validation)
- Enums (`OrderStatus`)
- Java 8 Streams (search/filter/order history)
- Basic Threading (automatic status progression)

## Module Mapping

1. **Place Order**
   - Takes customer name, item name, quantity
   - Generates a unique order ID
   - Saves order in memory
2. **Delivery Status**
   - Finds an order by ID and shows current status
3. **Order History**
   - Shows all previously placed orders
   - Displays total and delivered order count

## Brief Explanation of Each Class

- **OnlineFoodDeliveryTracker** (`app`)
  - Main class with menu loop and user interaction.
- **Order** (`model`)
  - Represents one order object (ID, customer, item, quantity, status, time).
- **OrderStatus** (`model`)
  - Enum containing: `PLACED`, `PREPARING`, `OUT_FOR_DELIVERY`, `DELIVERED`.
- **OrderOperations** (`service`)
  - Interface defining core order operations.
- **OrderService** (`service`)
  - Implements business logic and order storage using `ArrayList`.
  - Uses a background thread to simulate delivery status updates.
- **OrderIdGenerator** (`util`)
  - Utility class that generates unique IDs like `ORD1000`, `ORD1001`.

## How to Compile and Run (Terminal)

From the project root:

```bash
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out com.fooddelivery.app.OnlineFoodDeliveryTracker
```

## Notes for Mini Project

- In-memory storage is used (`ArrayList`), so data resets when app closes.
- Code is intentionally simple and readable for student-level understanding.
