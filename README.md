# Online Food Delivery Tracker (Vaadin Flow + Spring Boot)

## Project Overview
This project is now a **Java-only web application** built with **Spring Boot + Vaadin Flow**.
It provides role-based login and dashboards for:
- Customer
- Restaurant Owner
- Delivery Person

No Swing, no separate frontend, and no manual HTML/CSS/JavaScript coding is required.

## Tech Stack
- Java 17+
- Spring Boot 3
- Vaadin Flow 24
- Spring Data JPA
- H2 file database (persistent)

## Main Class to Run
`com.fooddelivery.OnlineFoodDeliveryTrackerApplication`

## Functional Modules
### 1) Login (Role-based)
- Username
- Password
- Role dropdown:
  - CUSTOMER
  - RESTAURANT_OWNER
  - DELIVERY_PERSON

### 2) Customer Dashboard
- Place order with:
  - Restaurant dropdown
  - Menu item dropdown
  - Auto item price display
  - Quantity
  - Delivery address
- View own order history in grid
- Track status updates

### 3) Restaurant Owner Dashboard
- View orders only for owner's restaurant
- Filter/search by order ID or customer name
- View customer details and address
- Update valid processing states:
  - PLACED -> PREPARING
  - PREPARING -> READY_FOR_PICKUP

### 4) Delivery Dashboard
- View delivery-relevant orders
- View customer name and address
- Update status:
  - READY_FOR_PICKUP -> OUT_FOR_DELIVERY
  - OUT_FOR_DELIVERY -> DELIVERED

## Persistence
- Data is stored in H2 file DB: `./data/foodtrackerdb`
- Records remain after restart
- Stores users, restaurants, menu items, and orders

## Demo Users
- Customer: `customer1 / cust123`
- Restaurant Owner: `owner_spice / owner123`
- Delivery Person: `delivery1 / del123`

## Run Instructions
From project root:

```bash
mvn clean spring-boot:run
```

Open in browser:
- `http://localhost:8080`

## Build / Verify
```bash
mvn clean test
```

## Vaadin Views Included
- `LoginView`
- `CustomerDashboardView`
- `RestaurantOwnerDashboardView`
- `DeliveryDashboardView`
