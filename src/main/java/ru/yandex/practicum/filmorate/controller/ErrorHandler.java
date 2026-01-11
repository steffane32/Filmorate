package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.sql.SQLException;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationException(final ValidationException e) {
        log.info("Ошибка валидации: {}", e.getMessage());
        return Map.of("error", "Ошибка валидации", "message", e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFoundException(final NotFoundException e) {
        log.info("Объект не найден: {}", e.getMessage());
        return Map.of("error", "Объект не найден", "message", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Ошибка валидации");

        log.info("Ошибка валидации входных данных: {}", errorMessage);
        return Map.of("error", "Ошибка валидации", "message", errorMessage);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgumentException(final IllegalArgumentException e) {
        log.info("Некорректный аргумент: {}", e.getMessage());
        return Map.of("error", "Некорректный запрос", "message", e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDataIntegrityViolationException(final DataIntegrityViolationException e) {
        log.info("Нарушение целостности данных: {}", e.getMessage());
        String causeMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        return Map.of("error", "Конфликт данных", "message", "Нарушение целостности данных: " + causeMessage);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDuplicateKeyException(final DuplicateKeyException e) {
        log.info("Дублирование уникального ключа: {}", e.getMessage());
        return Map.of("error", "Конфликт данных", "message", "Запись с такими данными уже существует");
    }

    @ExceptionHandler(DataRetrievalFailureException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleDataRetrievalFailureException(final DataRetrievalFailureException e) {
        log.error("Ошибка получения данных из БД: {}", e.getMessage(), e);
        return Map.of("error", "Ошибка при работе с базой данных");
    }

    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleSQLException(final SQLException e) {
        log.error("Ошибка SQL: {}", e.getMessage(), e);
        return Map.of("error", "Ошибка базы данных: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleException(final Exception e) {
        log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);
        return Map.of("error", "Внутренняя ошибка сервера",
                "message", "Произошла непредвиденная ошибка");
    }
}