# Online Food Delivery Tracker (Localhost Web Login)

This project now runs a lightweight **localhost web application** using Java's built-in HTTP server.
It provides a **single page** with different login options and requirements for:

- Customer
- Restaurant Owner
- Delivery Person

## Tech stack

- Java 17
- Maven
- Java built-in `HttpServer` (`com.sun.net.httpserver.HttpServer`)
- In-memory repositories (no DB required)

## Main class to run

`com.fooddelivery.app.OnlineFoodDeliveryTrackerApplication`

## Run steps

1. Compile:

```bash
mvn clean compile
```

2. Run:

```bash
mvn exec:java -Dexec.mainClass=com.fooddelivery.app.OnlineFoodDeliveryTrackerApplication
```

3. Open in browser:

```text
http://localhost:8080
```

## Demo users and role requirements

- **Customer**: `customer1 / pass`
  - Choose role: `CUSTOMER`
- **Restaurant Owner**: `owner_spice / pass` or `owner_pizza / pass`
  - Choose role: `RESTAURANT_OWNER`
- **Delivery Person**: `delivery1 / pass`
  - Choose role: `DELIVERY_PERSON`

If credentials are correct but role is wrong, login is rejected.
