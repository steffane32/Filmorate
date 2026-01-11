package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.GenreStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreStorage genreStorage;

    public List<Genre> findAll() {
        log.debug("Запрос всех жанров");
        List<Genre> genres = genreStorage.findAll();
        log.info("Получено {} жанров", genres.size());
        return genres;
    }

    public Genre findById(Long id) {
        log.debug("Поиск жанра по ID: {}", id);
        return genreStorage.findById(id)
                .orElseThrow(() -> {
                    log.warn("Жанр с ID {} не найден", id);
                    return new NotFoundException("Жанр с id=" + id + " не найден");
                });
    }

    public Genre findByName(String name) {
        log.debug("Поиск жанра по имени: {}", name);
        return genreStorage.findByName(name)
                .orElseThrow(() -> {
                    log.warn("Жанр с именем '{}' не найден", name);
                    return new NotFoundException("Жанр с именем '" + name + "' не найден");
                });
    }

    public Genre create(Genre genre) {
        log.info("Создание жанра: {}", genre.getName());

        // Проверяем что такого жанра еще нет
        genreStorage.findByName(genre.getName()).ifPresent(existing -> {
            log.warn("Попытка создать дубликат жанра: {}", genre.getName());
            throw new IllegalArgumentException("Жанр с именем '" + genre.getName() + "' уже существует");
        });

        Genre createdGenre = genreStorage.create(genre);
        log.info("✅ Жанр создан: ID={}, name={}", createdGenre.getId(), createdGenre.getName());
        return createdGenre;
    }

    public Genre update(Genre genre) {
        log.info("Обновление жанра: ID={}, name={}", genre.getId(), genre.getName());

        if (genre.getId() == null) {
            log.error("Попытка обновления жанра без ID");
            throw new IllegalArgumentException("ID жанра должен быть указан");
        }

        // Проверяем существование
        findById(genre.getId());

        Genre updatedGenre = genreStorage.update(genre);
        log.info("✅ Жанр обновлён: ID={}", updatedGenre.getId());
        return updatedGenre;
    }

    public void delete(Long id) {
        log.info("Удаление жанра: ID={}", id);
        genreStorage.delete(id);
        log.info("✅ Жанр удалён: ID={}", id);
    }

    public boolean existsById(Long id) {
        return genreStorage.existsById(id);
    }

    // Методы для работы с жанрами фильма
    public List<Genre> getFilmGenres(Long filmId) {
        log.debug("Получение жанров фильма: ID={}", filmId);
        List<Genre> genres = genreStorage.getFilmGenres(filmId);
        log.debug("Найдено {} жанров для фильма {}", genres.size(), filmId);
        return genres;
    }

    public void addGenreToFilm(Long filmId, Long genreId) {
        log.info("Добавление жанра {} фильму {}", genreId, filmId);

        // Проверяем существование жанра
        findById(genreId);

        genreStorage.addGenreToFilm(filmId, genreId);
        log.info("✅ Жанр добавлен фильму");
    }

    public void removeGenreFromFilm(Long filmId, Long genreId) {
        log.info("Удаление жанра {} у фильма {}", genreId, filmId);
        genreStorage.removeGenreFromFilm(filmId, genreId);
        log.info("✅ Жанр удалён у фильма");
    }
}