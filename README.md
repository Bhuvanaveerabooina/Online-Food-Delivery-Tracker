# Online Food Delivery Tracker (Java Swing Desktop App)

A beginner-friendly **GUI desktop application** to manage online food delivery orders.

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
- Exception handling (input validation)
- Enums (`OrderStatus`)
- Java 8 Streams (search/filter/order history)
- Basic Threading (automatic status progression)
- Java Swing (`JFrame`, `JButton`, `JTextField`, `JTextArea`, `JOptionPane`)

## Features

1. **Place Order**
   - Enter customer name, food item, and quantity
   - Generates a unique order ID
   - Saves order in memory
2. **Delivery Status**
   - Enter order ID and check current status
3. **Order History**
   - Shows all previously placed orders
   - Displays total and delivered order count
4. **Exit**
   - Closes the desktop application

## Brief Explanation of Each Class

- **OnlineFoodDeliveryTracker** (`app`)
  - Main Swing window and button actions.
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

## How to Compile and Run

From the project root:

```bash
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out com.fooddelivery.app.OnlineFoodDeliveryTracker
```

## Notes

- In-memory storage is used (`ArrayList`), so data resets when app closes.
- Backend/business logic is preserved and reused by the Swing UI.
- Code is intentionally simple and readable for student-level understanding.
