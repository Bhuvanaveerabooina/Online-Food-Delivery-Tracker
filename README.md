# Online Food Delivery Tracker (Spring Boot + Vaadin)

This project now runs as a **browser-based localhost application** using **Spring Boot + Vaadin Flow** (no Swing/JFrame desktop popup).

## Main requirements implemented

- Role-based login page with:
  - username
  - password
  - role dropdown (`CUSTOMER`, `RESTAURANT_OWNER`, `DELIVERY_PERSON`)
  - login button
- Separate dashboards by role:
  - Customer
  - Restaurant Owner
  - Delivery Person
- Persistent storage using H2 file database (`./data/fooddb*`), so orders remain after restart.
- Customer can place orders with restaurant dropdown, item dropdown, auto price, quantity, and address.
- Customer can view only their own order history.
- Restaurant owner sees only their own restaurant orders and can search by customer name/order ID.
- Delivery person sees delivery-relevant orders and can mark delivered.

## Tech stack

- Java 17
- Spring Boot 3
- Vaadin Flow 24
- Spring Data JPA
- H2 file database (persistent)

## Main class to run

`com.fooddelivery.app.OnlineFoodDeliveryTrackerApplication`

## Localhost URL

`http://localhost:8080`

## Run steps

1. Build:

```bash
mvn clean package
```

2. Run app:

```bash
mvn spring-boot:run
```

3. Open browser:

```text
http://localhost:8080
```

## Demo seeded users

- Customer: `customer1 / pass`
- Restaurant Owner: `owner_spice / pass` or `owner_pizza / pass`
- Delivery Person: `delivery1 / pass`

## Data model

- `User`
- `Role` enum (`CUSTOMER`, `RESTAURANT_OWNER`, `DELIVERY_PERSON`)
- `Restaurant`
- `MenuItem`
- `Order` (order entity)
- `OrderStatus` enum (`PLACED`, `PREPARING`, `READY_FOR_PICKUP`, `OUT_FOR_DELIVERY`, `DELIVERED`)
