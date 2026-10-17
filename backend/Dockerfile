# Build multi-stage: compila con Maven, esegue su una JRE leggera.
# Lo stadio di build gira sulla macchina che costruisce ($BUILDPLATFORM, x86 su GitHub):
# il JAR è bytecode, uguale per tutti i processori. Solo lo stadio finale esiste in versione amd64 e arm64.
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system docai
USER docai
COPY --from=build /src/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]