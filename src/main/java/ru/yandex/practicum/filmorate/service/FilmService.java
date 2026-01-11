package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @Autowired
    public FilmService(
            @Qualifier("filmDbStorage") FilmStorage filmStorage,
            @Qualifier("userDbStorage") UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        log.info("FilmService инициализирован");
    }

    public List<Film> findAll() {
        log.debug("Запрос на получение всех фильмов");
        List<Film> films = filmStorage.findAll();
        log.debug("Получено {} фильмов", films.size());
        return films;
    }

    public Film create(Film film) {
        log.info("Создание нового фильма: '{}'", film.getName());
        validateFilm(film);
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        log.info("Обновление фильма: ID={}, название='{}'", film.getId(), film.getName());
        validateFilm(film);
        return filmStorage.update(film);
    }

    public Film findById(Long id) {
        log.debug("Поиск фильма по ID: {}", id);
        return filmStorage.findById(id);
    }

    public void addLike(Long filmId, Long userId) {
        log.info("Добавление лайка: фильм {}, пользователь {}", filmId, userId);

        // Проверяем существование фильма и пользователя
        Film film = filmStorage.findById(filmId);
        User user = userStorage.findById(userId);

        // Проверяем, не поставил ли уже лайк
        if (film.getLikes().contains(userId)) {
            throw new ValidationException("Пользователь уже поставил лайк этому фильму");
        }

        filmStorage.addLike(filmId, userId);
        log.info("✅ Лайк успешно добавлен: фильм {}, пользователь {}", filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        log.info("Удаление лайка: фильм {}, пользователь {}", filmId, userId);

        // Проверяем существование фильма и пользователя
        Film film = filmStorage.findById(filmId);
        User user = userStorage.findById(userId);

        // Проверяем, есть ли лайк
        if (!film.getLikes().contains(userId)) {
            throw new ValidationException("Пользователь не ставил лайк этому фильму");
        }

        filmStorage.removeLike(filmId, userId);
        log.info("✅ Лайк успешно удалён: фильм {}, пользователь {}", filmId, userId);
    }

    public List<Film> getPopularFilms(int count) {
        log.info("Запрос популярных фильмов, количество: {}", count);

        if (count <= 0) {
            log.error("Некорректный параметр count: {}", count);
            throw new ValidationException("Параметр count должен быть положительным");
        }

        List<Film> popularFilms = filmStorage.getPopularFilms(count);
        log.debug("Возвращено {} популярных фильмов", popularFilms.size());
        return popularFilms;
    }

    private void validateFilm(Film film) {
        log.debug("Валидация фильма: '{}'", film.getName());

        // Проверка названия
        if (film.getName() == null || film.getName().isBlank()) {
            log.error("Пустое название фильма");
            throw new ValidationException("Название не может быть пустым");
        }

        // Проверка описания
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.error("Описание слишком длинное: {} символов (максимум 200)",
                    film.getDescription().length());
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }

        // Проверка даты релиза
        if (film.getReleaseDate() == null) {
            log.error("Дата релиза не указана");
            throw new ValidationException("Дата релиза должна быть указана");
        }

        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.error("Некорректная дата релиза: {} (минимум {})",
                    film.getReleaseDate(), MIN_RELEASE_DATE);
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }

        // Проверка продолжительности
        if (film.getDuration() == null || film.getDuration() <= 0) {
            log.error("Некорректная продолжительность: {}", film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительной");
        }

        // Проверка MPA
        if (film.getMpa() == null) {
            log.error("MPA рейтинг не указан");
            throw new ValidationException("MPA рейтинг должен быть указан");
        }

        log.debug("Валидация фильма пройдена успешно");
    }
}