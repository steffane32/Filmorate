package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationException(final ValidationException e) {
        log.warn("⚠️ Ошибка валидации: {}", e.getMessage());
        return Map.of("error", "Ошибка валидации", "message", e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFoundException(final NotFoundException e) {
        log.warn("🔍 Объект не найден: {}", e.getMessage());
        return Map.of("error", "Объект не найден", "message", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Ошибка валидации");

        log.warn("⚠️ Ошибка валидации входных данных: {}", errorMessage);
        return Map.of("error", "Ошибка валидации", "message", errorMessage);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgumentException(final IllegalArgumentException e) {
        log.warn("⚠️ Некорректный аргумент: {}", e.getMessage());
        return Map.of("error", "Некорректный запрос", "message", e.getMessage());
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDataIntegrityViolationException(
            final org.springframework.dao.DataIntegrityViolationException e) {
        log.warn("⚠️ Нарушение целостности данных: {}", e.getMessage());
        return Map.of("error", "Конфликт данных",
                "message", "Нарушение целостности данных: " + e.getCause().getMessage());
    }

    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDuplicateKeyException(
            final org.springframework.dao.DuplicateKeyException e) {
        log.warn("⚠️ Дублирование уникального ключа: {}", e.getMessage());
        return Map.of("error", "Конфликт данных", "message", "Запись с такими данными уже существует");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleException(final Exception e) {
        log.error("❌ Внутренняя ошибка сервера: {}", e.getMessage(), e);
        return Map.of("error", "Внутренняя ошибка сервера",
                "message", "Произошла непредвиденная ошибка: " + e.getClass().getSimpleName());
    }
}