FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./mvnw package -DskipTests -pl mem0-server -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/mem0-server/target/*.jar app.jar

ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
