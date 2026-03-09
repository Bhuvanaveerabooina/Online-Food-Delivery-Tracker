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
│           │   ├── OrderFileStore.java
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
- File handling with Java serialization (`ObjectOutputStream` / `ObjectInputStream`)

## Features

1. **Place Order**
   - Enter customer name, food item, and quantity
   - Generates a unique order ID
   - Saves order in memory and local file
2. **Delivery Status**
   - Enter order ID and check current status
3. **Order History**
   - Shows all previously placed orders
   - Displays total and delivered order count
   - Reads from saved data loaded at startup
4. **Exit**
   - Closes the desktop application

## Brief Explanation of Each Class

- **OnlineFoodDeliveryTracker** (`app`)
  - Main Swing window and button actions.
- **Order** (`model`)
  - Represents one order object (ID, customer, item, quantity, status, time).
  - Implements `Serializable` so orders can be stored in a local file.
- **OrderStatus** (`model`)
  - Enum containing: `PLACED`, `PREPARING`, `OUT_FOR_DELIVERY`, `DELIVERED`.
- **OrderOperations** (`service`)
  - Interface defining core order operations.
- **OrderService** (`service`)
  - Implements business logic and order storage using `ArrayList`.
  - Loads existing orders when app starts.
  - Saves orders whenever a new order is placed or status changes.
  - Uses a background thread to simulate delivery status updates.
- **OrderFileStore** (`service`)
  - Handles reading/writing order history in `orders-data.ser`.
- **OrderIdGenerator** (`util`)
  - Utility class that generates unique IDs like `ORD1000`, `ORD1001`.
  - Syncs counter from loaded orders to avoid duplicate IDs.

## How to Compile and Run

From the project root:

```bash
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out com.fooddelivery.app.OnlineFoodDeliveryTracker
```

## Notes

- Orders are now saved in `orders-data.ser` in the app's working folder.
- On restart, saved orders are automatically loaded into the app.
- GUI flow and existing features are preserved.
- Code is intentionally simple and readable for student-level understanding.
