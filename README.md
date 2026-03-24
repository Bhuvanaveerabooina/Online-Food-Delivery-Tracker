# Online Food Delivery Tracker (Java Role-Based Web App)

A beginner-friendly **role-based food delivery tracker** with simple login, registration, and dashboards for:

- Customer
- Restaurant Owner
- Delivery Person

## Features

1. **Role-based login & registration**
   - Select role while login/registering
   - Redirect to role-specific dashboard
   <img width="1217" height="667" alt="image" src="https://github.com/user-attachments/assets/6d8563e6-135b-4e04-9de6-87d3b99e535e" />

2. **Customer dashboard**
   - Place order with restaurant and item dropdowns
   - Auto price display for selected menu item
   - Quantity + total price support
   - View own order history and check own order status
   <img width="1516" height="862" alt="image" src="https://github.com/user-attachments/assets/575a0c27-f140-4cbc-b0c8-20d74287b342" />
    <img width="1735" height="815" alt="image" src="https://github.com/user-attachments/assets/cf2830c6-b9b8-4fcf-8e48-ec26682cde66" />


3. **Restaurant owner dashboard**
   - View only orders for owner restaurant
   - Filter by customer name and order status
   - Update status: `PLACED`, `PREPARING`, `OUT_FOR_DELIVERY`
   <img width="1577" height="840" alt="image" src="https://github.com/user-attachments/assets/10ed66c7-4d16-4d93-be0b-3eb545ff760d" />

4. **Delivery person dashboard**
   - View orders that are out for delivery
   - Mark delivered orders as `DELIVERED`
   <img width="1536" height="862" alt="image" src="https://github.com/user-attachments/assets/08b1ae47-9624-4f21-a487-7d26b1cab96c" />

5. **Simple persistent storage**
   - Users and orders are saved to local serialized files

## Seed Data

### Sample users

- `customer / customer123` (Customer)
- `owner / owner123` (Restaurant Owner, mapped to Spice Garden)
- `delivery / delivery123` (Delivery Person)

### Sample restaurants and menu

- Spice Garden
  - Paneer Biryani
  - Veg Thali
- Urban Pizza
  - Margherita Pizza
  - Pasta Alfredo

### Sample orders

A few initial orders are seeded with statuses:
`PLACED`, `PREPARING`, `OUT_FOR_DELIVERY`.

## Tech Used

- Java `com.sun.net.httpserver.HttpServer`
- Simple HTML/CSS/JavaScript (served by Java)
- OOP + service layer
- Java Serialization for persistence

## Project Structure

```text
src/com/fooddelivery/
├── app/
│   └── OnlineFoodDeliveryTracker.java
├── model/
│   ├── MenuItem.java
│   ├── Order.java
│   ├── OrderStatus.java
│   ├── Restaurant.java
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
