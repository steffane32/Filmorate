package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;

@Slf4j
@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();
    private long nextId = 1;

    @Override
    public List<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public Film create(Film film) {
        long id = nextId++;
        film.setId(id);

        // Создаем копию
        Film filmToSave = copyFilm(film);
        films.put(id, filmToSave);
        return filmToSave;
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

        // Создаем копию
        Film filmToSave = copyFilm(film);
        films.put(id, filmToSave);
        return filmToSave;
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

        return copyFilm(film);
    }

    @Override
    public void delete(Long id) {
        if (!films.containsKey(id)) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
        films.remove(id);
    }

    private Film copyFilm(Film original) {
        Film copy = new Film();
        copy.setId(original.getId());
        copy.setName(original.getName());
        copy.setDescription(original.getDescription());
        copy.setReleaseDate(original.getReleaseDate());
        copy.setDuration(original.getDuration());

        // Копируем лайки
        Set<Long> likesCopy = new HashSet<>(original.getLikes());
        copy.setLikes(likesCopy);

        return copy;
    }
}