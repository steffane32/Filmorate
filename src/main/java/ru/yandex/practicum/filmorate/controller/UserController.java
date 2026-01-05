package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final Map<Long, User> users = new HashMap<>();

    @GetMapping
    public Collection<User> findAll() {
        log.info("Получен запрос на получение всех пользователей. Текущее количество: {}", users.size());
        return users.values();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        log.info("Получен запрос на создание пользователя: {}", user);
        validateUser(user);
        user.setId(getNextId());

        // Если имя пустое, используем логин
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Имя пользователя пустое, установлен логин: {}", user.getLogin());
        }

        users.put(user.getId(), user);
        log.info("Пользователь успешно создан: {}", user);
        return user;
    }

    @PutMapping
    public User update(@RequestBody User user) {
        log.info("Получен запрос на обновление пользователя: {}", user);
        validateUser(user);

        if (user.getId() == null) {
            String errorMessage = "Id должен быть указан";
            log.error(errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (!users.containsKey(user.getId())) {
            String errorMessage = "Пользователь с id = " + user.getId() + " не найден";
            log.error(errorMessage);
            throw new ValidationException(errorMessage);
        }

        User existingUser = users.get(user.getId());

        // Обновляем поля
        existingUser.setEmail(user.getEmail());
        existingUser.setLogin(user.getLogin());

        // Если имя пустое, используем логин
        if (user.getName() == null || user.getName().isBlank()) {
            existingUser.setName(user.getLogin());
            log.debug("Имя пользователя пустое, установлен логин: {}", user.getLogin());
        } else {
            existingUser.setName(user.getName());
        }

        existingUser.setBirthday(user.getBirthday());

        log.info("Пользователь успешно обновлен: {}", existingUser);
        return existingUser;
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            String errorMessage = "Электронная почта не может быть пустой";
            log.error(errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (!user.getEmail().contains("@")) {
            String errorMessage = "Электронная почта должна содержать символ @";
            log.error(errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            String errorMessage = "Логин не может быть пустым";
            log.error(errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (user.getLogin().contains(" ")) {
            String errorMessage = "Логин не может содержать пробелы";
            log.error(errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            String errorMessage = "Дата рождения не может быть в будущем";
            log.error(errorMessage);
            throw new ValidationException(errorMessage);
        }
    }

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}