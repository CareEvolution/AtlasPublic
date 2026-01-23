# Note: The Dockerfile build context should be the parent directory of "backend",
# so that the .git directory is included in the build context.

FROM maven:3.9.11-eclipse-temurin-21-alpine AS builder

RUN apk add --no-cache curl

WORKDIR /code

ARG OPENTELEMETRY_JAVA_AGENT_VERSION=2.20.1
RUN curl -LSsO https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OPENTELEMETRY_JAVA_AGENT_VERSION}/opentelemetry-javaagent.jar

# Download dependencies
COPY ./backend/pom.xml /code/
COPY ./.git /code/.git
RUN mvn package

# Compile code and repackage it
COPY ./backend/src /code/src
RUN mvn package -DskipTests=true -DskipUnitTests=true

# OHDSI WebAPI running as a Spring Boot application with Java 21
FROM index.docker.io/library/eclipse-temurin:21-jre-alpine

RUN apk --no-cache add curl

# Default Java options. The first entry is a fix for when java reads secure random numbers:
# in a containerized system using /dev/random may reduce entropy too much, causing slowdowns.
# https://ruleoftech.com/2016/avoiding-jvm-delays-caused-by-random-number-generation
ENV DEFAULT_JAVA_OPTS="-Djava.security.egd=file:///dev/./urandom"

# set working directory to a fixed WebAPI directory
WORKDIR /var/lib/ohdsi/webapi

COPY --from=builder /code/opentelemetry-javaagent.jar .

# deploy the just built OHDSI WebAPI war file
# copy resources in order of fewest changes to most changes.
# This way, the libraries step is not duplicated if the dependencies
# do not change.
COPY --from=builder /code/target/webapi.jar .

EXPOSE 8080

USER 101

# Run the jar file
CMD java -jar webapi.jar ${DEFAULT_JAVA_OPTS}
