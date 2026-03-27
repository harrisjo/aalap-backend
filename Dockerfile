# Stage 1: Build the JAR
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy the source code (filtered by your .dockerignore)
COPY . .

# Build the project
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: The Runtime Stage
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copy ONLY the finished jar from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]

