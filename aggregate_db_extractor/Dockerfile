### STAGE 1: build jar with Maven
FROM maven:3-eclipse-temurin-11 as jarBuilder

WORKDIR /code

# Copy all java backend related files into /app
COPY aggregatedbservice/. .

# run maven package
RUN mvn clean package


### STAGE 2: copy the jar to a new smaller image and run the jar

FROM eclipse-temurin:11-alpine

WORKDIR /code

COPY --from=jarBuilder /code/target/app.jar .

ENTRYPOINT ["java","-jar","/code/app.jar"]
