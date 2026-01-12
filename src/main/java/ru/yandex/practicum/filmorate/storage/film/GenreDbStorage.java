package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class GenreDbStorage implements GenreStorage {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Genre> genreRowMapper = new RowMapper<Genre>() {
        @Override
        public Genre mapRow(ResultSet rs, int rowNum) throws SQLException {
            Genre genre = new Genre();
            genre.setId(rs.getLong("genre_id"));
            genre.setName(rs.getString("name"));
            return genre;
        }
    };

    @Override
    public List<Genre> findAll() {
        log.debug("Запрос всех жанров из БД");
        String sql = "SELECT * FROM genres ORDER BY genre_id";
        List<Genre> genres = jdbcTemplate.query(sql, genreRowMapper);
        log.info("Загружено {} жанров из БД", genres.size());
        return genres;
    }

    @Override
    public Optional<Genre> findById(Long id) {
        log.debug("Поиск жанра по ID: {}", id);
        String sql = "SELECT * FROM genres WHERE genre_id = ?";
        try {
            Genre genre = jdbcTemplate.queryForObject(sql, genreRowMapper, id);
            return Optional.ofNullable(genre);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Жанр с ID {} не найден", id);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Genre> findByName(String name) {
        log.debug("Поиск жанра по имени: {}", name);
        String sql = "SELECT * FROM genres WHERE name = ?";
        try {
            Genre genre = jdbcTemplate.queryForObject(sql, genreRowMapper, name);
            return Optional.ofNullable(genre);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Жанр с именем '{}' не найден", name);
            return Optional.empty();
        }
    }

    @Override
    public Genre create(Genre genre) {
        log.info("Создание жанра: {}", genre.getName());

        String sql = "INSERT INTO genres (name) VALUES (?)";

        jdbcTemplate.update(sql, genre.getName());

        // Получаем ID
        String getIdSql = "SELECT genre_id FROM genres WHERE name = ?";
        Long id = jdbcTemplate.queryForObject(getIdSql, Long.class, genre.getName());
        genre.setId(id);

        log.info("✅ Жанр создан с ID: {}", id);
        return genre;
    }

    @Override
    public Genre update(Genre genre) {
        log.info("Обновление жанра: ID={}, name={}", genre.getId(), genre.getName());

        String sql = "UPDATE genres SET name = ? WHERE genre_id = ?";

        int rowsUpdated = jdbcTemplate.update(sql,
                genre.getName(),
                genre.getId()
        );

        if (rowsUpdated == 0) {
            log.warn("Жанр с ID={} не найден для обновления", genre.getId());
            throw new NotFoundException("Жанр с id=" + genre.getId() + " не найден");
        }

        log.info("✅ Жанр обновлён: ID={}", genre.getId());
        return genre;
    }

    @Override
    public void delete(Long id) {
        log.info("Удаление жанра: ID={}", id);

        String sql = "DELETE FROM genres WHERE genre_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, id);

        if (rowsDeleted == 0) {
            log.warn("Жанр с ID={} не найден для удаления", id);
            throw new NotFoundException("Жанр с id=" + id + " не найден");
        }

        log.info("✅ Жанр удалён: ID={}", id);
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM genres WHERE genre_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public List<Genre> getFilmGenres(Long filmId) {
        log.debug("Получение жанров для фильма: ID={}", filmId);

        String sql = "SELECT g.* FROM genres g " +
                "JOIN film_genres fg ON g.genre_id = fg.genre_id " +
                "WHERE fg.film_id = ? " +
                "ORDER BY g.genre_id";

        List<Genre> genres = jdbcTemplate.query(sql, genreRowMapper, filmId);
        log.debug("Найдено {} жанров для фильма {}", genres.size(), filmId);
        return genres;
    }

    @Override
    public void addGenreToFilm(Long filmId, Long genreId) {
        log.info("Добавление жанра {} фильму {}", genreId, filmId);

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, genreId);

        log.info("✅ Жанр {} добавлен фильму {}", genreId, filmId);
    }

    @Override
    public void removeGenreFromFilm(Long filmId, Long genreId) {
        log.info("Удаление жанра {} у фильма {}", genreId, filmId);

        String sql = "DELETE FROM film_genres WHERE film_id = ? AND genre_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, filmId, genreId);

        if (rowsDeleted == 0) {
            log.warn("Жанр {} не найден у фильма {}", genreId, filmId);
            throw new NotFoundException("Жанр не найден у фильма");
        }

        log.info("✅ Жанр {} удалён у фильма {}", genreId, filmId);
    }
}