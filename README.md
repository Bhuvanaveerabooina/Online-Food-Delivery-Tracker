# Online Food Delivery Tracker (Java Real-Time Web App)

A beginner-friendly **localhost web application** to manage online food delivery orders with **role-based login** and **persistent order history**.

## Features

1. **Role-based Login & Registration**
   - Create account and choose role from dropdown: `CUSTOMER`, `RESTAURANT_OWNER`, `DELIVERY_PERSON`
   - Users are stored on disk and available after app restart
2. **Customer Portal**
   - Place order with customer name, address, restaurant, item, item price, quantity
   - See own order history and status updates
3. **Restaurant Owner Portal**
   - See full order status dashboard
   - Filter order list by customer name
4. **Delivery Person Portal**
   - View all order details including customer address
   - Mark order as delivered using order ID
5. **Real-Time Tracking**
   - Background status progression: `PLACED → PREPARING → OUT_FOR_DELIVERY → AWAITING_CUSTOMER_VERIFICATION`
   - Browser history table auto-refreshes every 2 seconds
6. **Order History Persistence**
   - Orders are saved to local files and loaded at startup
   - History remains available even after closing the app

## Tech Used

- Java (`com.sun.net.httpserver.HttpServer`) for backend
- Simple HTML/CSS/JavaScript frontend (served by Java)
- OOP + service layer design
- Java Serialization for persistent storage
- Basic multithreading for status simulation

## Project Structure

```text
src/com/fooddelivery/
├── app/
│   └── OnlineFoodDeliveryTracker.java
├── model/
│   ├── Order.java
│   ├── OrderStatus.java
│   ├── UserAccount.java
│   └── UserRole.java
├── service/
│   ├── AuthService.java
│   ├── OrderFileStore.java
│   ├── OrderOperations.java
│   ├── OrderService.java
│   └── UserFileStore.java
└── util/
    └── OrderIdGenerator.java
```

## How to Compile and Run

From project root:

```bash
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out com.fooddelivery.app.OnlineFoodDeliveryTracker
```

Then open:

- `http://localhost:8080`

## Data Files

Stored in your home directory:

- `~/.online-food-delivery-tracker/orders-data.ser`
- `~/.online-food-delivery-tracker/users-data.ser`
