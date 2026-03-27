# Step 1: Build the JAR (Multi-stage build)
FROM maven:3.9.9-eclipse-temurin-25 AS build
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run the JAR
FROM eclipse-temurin:25-jre
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]