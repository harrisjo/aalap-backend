# Step 1: Build the JAR using the included Maven Wrapper
FROM eclipse-temurin:25-jdk AS build
COPY . .
# Fix permissions for the wrapper and build
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Step 2: Run the JAR
FROM eclipse-temurin:25-jre
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]