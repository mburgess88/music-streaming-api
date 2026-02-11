# Build stage
FROM amazoncorretto:11 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn ./.mvn
COPY mvnw .
RUN chmod +x mvnw
COPY src ./src
RUN ./mvnw clean package -Dmaven.test.skip=true

# Run stage
FROM amazoncorretto:11-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
