# Fáze 1: Sestavení aplikace pomocí Maven a JDK 21
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Nejprve zkopírujeme pom.xml pro efektivní cachování maven závislostí
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Zkopírujeme zdrojové kódy a sestavíme spustitelný JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Fáze 2: Spustitelný kontejner s lehkým JRE 21
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Vytvoření neprivilegovaného uživatele pro běh aplikace
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /build/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
