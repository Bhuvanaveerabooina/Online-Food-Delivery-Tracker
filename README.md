# Online Food Delivery Tracker (Plain Java Console)

This project runs as a **plain Java console application**.
It does **not** use Spring Boot, Vaadin, or localhost web hosting.

## Why localhost was failing

Your previous merge switched to a web app approach. If that server was not running, `localhost` would refuse the connection. This version removes the web server completely.

## Tech stack

- Java 17
- Maven (build tool)
- In-memory repositories (no DB server required)

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

## Demo users

- Customer: `customer1 / pass`
- Restaurant Owner: `owner_spice / pass` or `owner_pizza / pass`
- Delivery Person: `delivery1 / pass`

## Features

- Role-based login
- Customer can place orders and view order history
- Restaurant owner can view/search own restaurant orders and update status
- Delivery person can view delivery orders and mark status updates
