# =========================
# Build stage
# =========================
FROM maven:3.9.10-eclipse-temurin-17 AS build

WORKDIR /App

# zuerst nur pom.xml kopieren für besseren Cache
COPY pom.xml .

# Dependencies laden
RUN mvn dependency:go-offline

# danach Source kopieren
COPY src ./src

# Anwendung bauen
RUN mvn clean package -DskipTests

# =========================
# Runtime stage
# =========================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /App

# genau eine Jar kopieren und sauber benennen
COPY --from=build /App/target/*.jar /App/app.jar

# optional: warten bis DB erreichbar ist
COPY wait-for-db.sh /usr/local/bin/wait-for-db.sh
RUN chmod +x /usr/local/bin/wait-for-db.sh

EXPOSE 8080

# falls du wait-for-db.sh wirklich benutzen willst:
# ENTRYPOINT ["sh", "-c", "/usr/local/bin/wait-for-db.sh && java -jar /App/app.jar"]

# falls nicht:
ENTRYPOINT ["java", "-jar", "/App/app.jar"]