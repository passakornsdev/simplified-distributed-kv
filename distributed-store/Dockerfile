FROM amazoncorretto:25 AS build

WORKDIR /workspace

RUN dnf install -y tar gzip && dnf clean all

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw --batch-mode dependency:go-offline

COPY src src
RUN ./mvnw --batch-mode package -DskipTests

FROM amazoncorretto:25

WORKDIR /app

COPY --from=build /workspace/target/distributed-store-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

USER 1001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
