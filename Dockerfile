FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY settings.gradle.kts .
COPY build.gradle.kts .
RUN ./gradlew --no-daemon dependencies --configuration runtimeClasspath
COPY src src
RUN ./gradlew --no-daemon test shadowJar

FROM eclipse-temurin:21-jre-noble
WORKDIR /app
COPY --from=build /app/build/libs/*-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
