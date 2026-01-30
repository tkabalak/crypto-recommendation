ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre

FROM ${MAVEN_IMAGE} AS build
WORKDIR /workspace
COPY . .
# Build only the application module (and required parents)
RUN mvn -q -pl crypto-recommendation-app -am clean package -DskipTests

FROM ${RUNTIME_IMAGE}
WORKDIR /app

# Create a non-root user
RUN useradd -r -u 10001 appuser

COPY --from=build /workspace/crypto-recommendation-app/target/*.jar /app/app.jar

USER 10001
EXPOSE 8080

# JVM options can be overridden in Kubernetes/containers
ENV JAVA_OPTS=""

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
