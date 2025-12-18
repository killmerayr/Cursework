# Этап 1: Сборка приложения
FROM maven:3.8.8-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Этап 2: Запуск приложения
FROM eclipse-temurin:11-jre
WORKDIR /app

# Установка необходимых библиотек для JavaFX (GTK, X11 и т.д.)
RUN apt-get update && apt-get install -y \
    libgtk-3-0 \
    libglu1-mesa \
    libxtst6 \
    libxxf86vm1 \
    libcanberra-gtk3-module \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/rating-system.jar app.jar

# Команда для запуска
ENTRYPOINT ["java", "-jar", "app.jar"]
