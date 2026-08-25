FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /opt/app  
COPY . .
RUN mvn clean package -DskipTests

# segundo linux
FROM eclipse-temurin:21-alpine-3.21
WORKDIR /opt/app
COPY --from=build /opt/app/target/cp01-api01-0.0.1-SNAPSHOT.jar /opt/app/app.jar
CMD ["java", "-jar", "app.jar"]

