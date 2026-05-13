FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder /app/target/*.jar app.jar
USER spring:spring
EXPOSE 8084
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]

