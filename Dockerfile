# Dockerfile
FROM openjdk:21-jdk-slim

WORKDIR /app

# Копируем собранный JAR файл
COPY target/filmorate-0.0.1-SNAPSHOT.jar app.jar

# Создаем директорию для базы данных
RUN mkdir -p /app/db

# Настройки безопасности
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --gid 1001 appuser && \
    chown -R appuser:appgroup /app

USER appuser

# Порт приложения
EXPOSE 8080

# Запуск приложения
ENTRYPOINT ["java", "-jar", "app.jar"]