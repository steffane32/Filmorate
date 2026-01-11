package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Genre;
import java.util.List;
import java.util.Optional;

public interface GenreStorage {
    List<Genre> findAll();
    Optional<Genre> findById(Long id);
    Optional<Genre> findByName(String name);
    Genre create(Genre genre);
    Genre update(Genre genre);
    void delete(Long id);
    boolean existsById(Long id);

    // Методы для работы с фильмами
    List<Genre> getFilmGenres(Long filmId);
    void addGenreToFilm(Long filmId, Long genreId);
    void removeGenreFromFilm(Long filmId, Long genreId);
}