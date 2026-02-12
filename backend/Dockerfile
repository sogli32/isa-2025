FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Kopiraj Maven wrapper i pom.xml
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Preuzmi dependency-je (cache layer)
RUN ./mvnw dependency:go-offline

# Kopiraj source code
COPY src ./src

# Kompajliraj aplikaciju
RUN ./mvnw clean package -DskipTests

# Production image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Kopiraj JAR iz build stage-a
COPY --from=build /app/target/*.jar app.jar

# Eksponuj port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Pokreni aplikaciju
ENTRYPOINT ["java", "-jar", "app.jar"]
