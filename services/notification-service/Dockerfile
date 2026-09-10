FROM maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
ENV MAVEN_OPTS=-Xmx512m
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -ntp -DskipTests package
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && useradd -r -u 10001 app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER app
ENTRYPOINT ["java","-jar","app.jar"]

