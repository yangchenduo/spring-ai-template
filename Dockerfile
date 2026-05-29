FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY spring-ai-template-app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
