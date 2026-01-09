package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @Autowired
    public FilmService(FilmStorage filmStorage) {
        this.filmStorage = filmStorage;
    }

    public List<Film> findAll() {
        log.debug("Запрос на получение всех фильмов");
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        log.info("Создание нового фильма: {}", film.getName());
        validateFilm(film);

        Film createdFilm = filmStorage.create(film);
        log.info("Фильм успешно создан: ID={}, название='{}'", createdFilm.getId(), createdFilm.getName());
        return createdFilm;
    }

    public Film update(Film film) {
        log.info("Обновление фильма: ID={}, название='{}'", film.getId(), film.getName());
        validateFilm(film);

        if (film.getId() == null) {
            log.error("Попытка обновления фильма без ID");
            throw new ValidationException("ID фильма должен быть указан");
        }

        // Проверяем существование фильма
        filmStorage.findById(film.getId());

        Film updatedFilm = filmStorage.update(film);
        log.info("Фильм успешно обновлён: ID={}", updatedFilm.getId());
        return updatedFilm;
    }

    public Film findById(Long id) {
        log.debug("Поиск фильма по ID: {}", id);
        Film film = filmStorage.findById(id);
        if (film == null) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
        return film;
    }

    public void addLike(Long filmId, Long userId) {
        log.info("Добавление лайка: фильм {}, пользователь {}", filmId, userId);

        // Получаем фильм из хранилища
        Film film = filmStorage.findById(filmId);

        // Проверяем, есть ли уже лайк
        Set<Long> likes = film.getLikes();
        if (likes.contains(userId)) {
            log.warn("Пользователь {} уже поставил лайк фильму {}", userId, filmId);
            throw new ValidationException("Пользователь уже поставил лайк этому фильму");
        }

        // Создаем новый фильм с обновленными лайками
        Film updatedFilm = createUpdatedFilm(film);

        // Добавляем лайк
        updatedFilm.getLikes().add(userId);

        // Сохраняем обновленный фильм
        filmStorage.update(updatedFilm);

        log.info("Лайк добавлен: фильм {}, пользователь {}, всего лайков: {}",
                filmId, userId, updatedFilm.getLikes().size());
    }

    public void removeLike(Long filmId, Long userId) {
        log.info("Удаление лайка: фильм {}, пользователь {}", filmId, userId);

        Film film = filmStorage.findById(filmId);
        Set<Long> likes = film.getLikes();

        if (!likes.contains(userId)) {
            log.warn("Лайк от пользователя {} не найден для фильма {}", userId, filmId);
            throw new ValidationException("Лайк от пользователя не найден");
        }

        // Создаем новый фильм с обновленными лайками
        Film updatedFilm = createUpdatedFilm(film);

        // Удаляем лайк
        updatedFilm.getLikes().remove(userId);

        // Сохраняем обновленный фильм
        filmStorage.update(updatedFilm);

        log.info("Лайк удалён: фильм {}, пользователь {}, осталось лайков: {}",
                filmId, userId, updatedFilm.getLikes().size());
    }

    public List<Film> getPopularFilms(int count) {
        log.debug("Запрос популярных фильмов, count={}", count);

        if (count <= 0) {
            log.error("Некорректный параметр count: {}", count);
            throw new ValidationException("Параметр count должен быть положительным");
        }

        List<Film> popularFilms = filmStorage.findAll().stream()
                .sorted(Comparator.comparingInt((Film f) -> f.getLikes().size()).reversed())
                .limit(count)
                .collect(Collectors.toList());

        log.debug("Возвращено {} популярных фильмов", popularFilms.size());
        return popularFilms;
    }

    private void validateFilm(Film film) {
        // Проверка названия
        if (film.getName() == null || film.getName().isBlank()) {
            log.error("Пустое название фильма");
            throw new ValidationException("Название не может быть пустым");
        }

        // Проверка описания
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.error("Описание слишком длинное: {} символов (максимум 200)", film.getDescription().length());
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
    }

    // Вспомогательный метод для создания копии фильма
    private Film createUpdatedFilm(Film original) {
        Film updated = new Film();
        updated.setId(original.getId());
        updated.setName(original.getName());
        updated.setDescription(original.getDescription());
        updated.setReleaseDate(original.getReleaseDate());
        updated.setDuration(original.getDuration());

        // Копируем существующие лайки
        Set<Long> likesCopy = new java.util.HashSet<>(original.getLikes());
        updated.setLikes(likesCopy);

        return updated;
    }
}