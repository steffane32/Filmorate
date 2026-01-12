package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component("inMemoryFilmStorage")
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();
    private long nextId = 1;

    @Override
    public List<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public Film create(Film film) {
        film.setId(nextId++);
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(Film film) {
        Long id = film.getId();

        if (id == null) {
            throw new IllegalArgumentException("ID фильма должен быть указан");
        }

        if (!films.containsKey(id)) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }

        films.put(id, film);
        return film;
    }

    @Override
    public Film findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID фильма не может быть null");
        }

        Film film = films.get(id);
        if (film == null) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }

        return film;
    }

    @Override
    public void delete(Long id) {
        if (!films.containsKey(id)) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
        films.remove(id);
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        Film film = findById(filmId);

        if (film.getLikes().contains(userId)) {
            throw new IllegalArgumentException("Пользователь уже поставил лайк");
        }

        film.getLikes().add(userId);
        log.info("Лайк добавлен: фильм {}, пользователь {}", filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        Film film = findById(filmId);

        if (!film.getLikes().remove(userId)) {
            throw new IllegalArgumentException("Лайк не найден");
        }

        log.info("Лайк удалён: фильм {}, пользователь {}", filmId, userId);
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count должен быть положительным");
        }

        return findAll().stream()
                .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .limit(count)
                .collect(Collectors.toList());
    }
}