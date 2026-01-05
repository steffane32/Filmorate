package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {

    private final Map<Long, Film> films = new HashMap<>();
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @GetMapping
    public Collection<Film> findAll() {
        log.info("Получен запрос на получение всех фильмов. Текущее количество: {}", films.size());
        return films.values();
    }

    @PostMapping
    public Film create(@RequestBody Film film) {
        log.info("Получен запрос на создание фильма: {}", film);
        validateFilm(film);
        film.setId(getNextId());
        films.put(film.getId(), film);
        log.info("Фильм успешно создан: {}", film);
        return film;
    }

    @PutMapping
    public Film update(@RequestBody Film film) {
        log.info("Получен запрос на обновление фильма: {}", film);
        validateFilm(film);

        if (film.getId() == null) {
            String errorMessage = "Id должен быть указан";
            log.error(errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (!films.containsKey(film.getId())) {
            String errorMessage = "Фильм с id = " + film.getId() + " не найден";
            log.error(errorMessage);
            throw new ValidationException(errorMessage);
        }

        films.put(film.getId(), film);
        log.info("Фильм успешно обновлен: {}", film);
        return film;
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            String errorMessage = "Название не может быть пустым";
            log.error(errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            String errorMessage = "Максимальная длина описания — 200 символов";
            log.error(errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (film.getReleaseDate() == null) {
            String errorMessage = "Дата релиза должна быть указана";
            log.error(errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            String errorMessage = "Дата релиза — не раньше 28 декабря 1895 года";
            log.error(errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (film.getDuration() == null || film.getDuration() <= 0) {
            String errorMessage = "Продолжительность фильма должна быть положительным числом";
            log.error(errorMessage);
            throw new ValidationException(errorMessage);
        }
    }

    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}