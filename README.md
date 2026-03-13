# Online Food Delivery Tracker (Spring Boot + Vaadin)

This project now runs as a **localhost browser-based Java web app**.
It uses **Spring Boot + Vaadin Flow** (not Swing/JFrame).

## Main entry point

- **Main class:** `com.fooddelivery.app.OnlineFoodDeliveryTrackerApplication`

## Run command

```bash
mvn spring-boot:run
```

## Localhost URL

- `http://localhost:8080`

## Demo login credentials

Use role exactly as listed in the dropdown.

- **CUSTOMER**
  - username: `customer1`
  - password: `pass`
- **RESTAURANT_OWNER**
  - username: `owner_spice`
  - password: `pass`
- **RESTAURANT_OWNER**
  - username: `owner_pizza`
  - password: `pass`
- **DELIVERY_PERSON**
  - username: `delivery1`
  - password: `pass`

## Features implemented

1. Login page (browser)
   - username
   - password
   - role dropdown (`CUSTOMER`, `RESTAURANT_OWNER`, `DELIVERY_PERSON`)
   - Login button
   - Register button

2. Registration
   - creates new users
   - validates duplicate usernames
   - saves role correctly
   - for `RESTAURANT_OWNER`, user must select a restaurant

3. Role-based dashboards
   - **Customer:** place order, select restaurant/item, auto item price, quantity, address, own history/status
   - **Restaurant Owner:** view only own restaurant orders, filter by customer/order ID, update restaurant-side status
   - **Delivery Person:** see delivery orders and mark delivered

4. Persistence
   - Uses file-based H2 DB at `./data/fooddb`
   - users and orders remain after restart

## Notes on old UI

- Old placeholder Swing/desktop flow is no longer the active entry path.
- Active app flow is Vaadin route-based web UI served by Spring Boot.
