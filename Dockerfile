FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY src ./src

RUN mkdir -p out data \
    && javac -d out $(find src -name "*.java")

EXPOSE 8080

CMD ["sh", "-c", "java -cp out com.fooddelivery.app.OnlineFoodDeliveryTracker"]
