# Online Food Delivery Tracker (Role-Based Java Swing Mini Project)

## Project Overview
This project is a Java desktop application built using **Swing** for an academic mini project.  
It tracks food delivery orders with a **role-based login system** and separate dashboards for:
- Customer
- Restaurant Owner
- Delivery Person

The application uses local file storage (Java serialization) so data persists across restarts.

## User Roles
### 1) Customer
- Login with role = `CUSTOMER`
- Place order with:
  - Restaurant dropdown
  - Menu item dropdown
  - Auto price display
  - Quantity
  - Delivery address
- Gets a unique order ID when order is placed
- Can view only their own order history and current statuses

### 2) Restaurant Owner
- Login with role = `RESTAURANT_OWNER`
- Can view only orders of their own restaurant
- Can search/filter orders by order ID or customer name
- Can view customer name and address
- Can update status with valid transitions:
  - `PLACED -> PREPARING -> READY_FOR_PICKUP`

### 3) Delivery Person
- Login with role = `DELIVERY_PERSON`
- Can view delivery-relevant orders (`READY_FOR_PICKUP` / `OUT_FOR_DELIVERY`)
- Can start delivery (`READY_FOR_PICKUP -> OUT_FOR_DELIVERY`)
- Can mark delivered (`OUT_FOR_DELIVERY -> DELIVERED`)
- Invalid actions are blocked with validation messages

## Features
- Role-based login screen with username, password, role dropdown
- Separate dashboard/panel per role
- Real-time-like dashboard refresh (timer-based)
- Persistent storage for:
  - Users
  - Restaurants and menu
  - Orders
- Order history retained after reopening app
- Input validations for empty fields, quantity, login, and status transitions
- Java 8 stream filtering for restaurant owner search

## Project Structure
```
src/com/fooddelivery/
  app/
    OnlineFoodDeliveryTracker.java        # Main class to run
  model/
    Role.java
    OrderStatus.java
    UserAccount.java
    Restaurant.java
    MenuItem.java
    Order.java
  service/
    AuthService.java
    OrderService.java
    OrderOperations.java
    RestaurantService.java
    UserFileStore.java
    RestaurantFileStore.java
    OrderFileStore.java
  ui/
    LoginFrame.java
    DashboardFrame.java
    CustomerPanel.java
    RestaurantOwnerPanel.java
    DeliveryPanel.java
```

## Technologies / Concepts Used
- Java (JDK 8+)
- Java Swing (`JFrame`, `JPanel`, `JTable`, `JComboBox`, etc.)
- OOP (encapsulation, separation of concerns)
- Enums for role and status
- Collections (`List`)
- Java 8 Streams for filtering/search
- Exception handling and validation
- File persistence using Java serialization

## Demo Credentials
- Customer: `customer1 / cust123`
- Restaurant Owner: `owner_spice / owner123` (Spice Hub)
- Delivery Person: `delivery1 / del123`

## Compile and Run
From project root:

```bash
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out com.fooddelivery.app.OnlineFoodDeliveryTracker
```

## Notes for Viva / Submission
- Main class: `com.fooddelivery.app.OnlineFoodDeliveryTracker`
- Status updates done by role-based workflow instead of automatic timer simulation
- Data files are stored under:
  - `~/.online-food-delivery-tracker/users-data.ser`
  - `~/.online-food-delivery-tracker/restaurants-data.ser`
  - `~/.online-food-delivery-tracker/orders-data.ser`
