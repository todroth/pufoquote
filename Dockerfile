FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache ffmpeg
WORKDIR /app
COPY target/pufoquote-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
