# Online Food Delivery Tracker (Java Swing Mini Project)

A polished, beginner-friendly **Java desktop application** for tracking food delivery orders with login, persistent storage, real-time status progression, and clear module separation.

## Features

- **Login Screen (Authentication)**
  - App starts with a dedicated login screen.
  - Includes a default demo user:
    - Username: `student`
    - Password: `student123`
- **Place Order Module**
  - Validates customer name, food item, and quantity.
  - Generates a unique order ID (`ORD1000`, `ORD1001`, ...).
  - Saves the order permanently.
  - Instantly refreshes order history after placement.
- **Delivery Status Module**
  - Search status using order ID.
  - Shows clear order lifecycle status.
  - Status updates in real time.
- **Order History Module**
  - Uses `JTable` to display all saved orders.
  - Loads automatically on app startup.
  - Persists after closing and reopening the app.
- **Automatic Status Progression**
  - Uses beginner-friendly background scheduling and Swing UI refresh.
  - Progression path:
    - `PLACED -> PREPARING -> OUT_FOR_DELIVERY -> DELIVERED`
- **Logout Support**
  - User can safely logout and return to login screen.
- **Status/Notification Area**
  - Clear messages for placement, lookup, validation, and errors.

## Main Class to Run

```text
com.fooddelivery.app.OnlineFoodDeliveryTracker
```

## Project Structure

```text
src/com/fooddelivery/
├── app/
│   └── OnlineFoodDeliveryTracker.java      # Application entry point
├── model/
│   ├── Order.java                          # Order entity
│   ├── OrderStatus.java                    # Status enum
│   └── UserAccount.java                    # User entity
├── service/
│   ├── AuthService.java                    # Login/auth logic + default user
│   ├── OrderFileStore.java                 # Order persistence
│   ├── OrderOperations.java                # Order interface abstraction
│   ├── OrderService.java                   # Order business logic + status updates
│   └── UserFileStore.java                  # User persistence
├── ui/
│   ├── LoginFrame.java                     # Login UI
│   └── MainFrame.java                      # Main dashboard UI
└── util/
    └── OrderIdGenerator.java               # Unique order ID generation
```

## Java Concepts Used

- OOP (classes/objects)
- Encapsulation
- Abstraction (`OrderOperations` interface)
- Enums (`OrderStatus`)
- Collections (`List`)
- Exception handling (validation and parsing)
- Java 8 Streams (search/filter/sort)
- Basic multithreading/background scheduling (`ScheduledExecutorService`)
- Swing `Timer` for regular UI refresh
- File handling with serialization for persistence

## Persistence Details

Data files are stored in:

```text
~/.online-food-delivery-tracker/
├── orders-data.ser
└── users-data.ser
```

This ensures history and users remain available across normal restarts.

## Compile and Run

From project root:

```bash
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out com.fooddelivery.app.OnlineFoodDeliveryTracker
```

## Notes for Mini Project Submission

- Fully Java-based desktop app (no web framework).
- Clean UI with organized sections and table-based history.
- Persistent order history and authentication included.
- Real-time status behavior implemented in a beginner-friendly way.
