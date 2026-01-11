package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

@Slf4j
@Repository("filmDbStorage")
@Primary
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final MpaStorage mpaStorage;
    private final GenreStorage genreStorage;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate, MpaStorage mpaStorage, GenreStorage genreStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.mpaStorage = mpaStorage;
        this.genreStorage = genreStorage;
        log.info("FilmDbStorage инициализирован");
    }

    private final RowMapper<Film> filmRowMapper = new RowMapper<Film>() {
        @Override
        public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
            Film film = new Film();
            film.setId(rs.getLong("film_id"));
            film.setName(rs.getString("title"));
            film.setDescription(rs.getString("description"));
            film.setReleaseDate(rs.getDate("release_date").toLocalDate());
            film.setDuration(rs.getInt("duration"));

            Long mpaId = rs.getLong("mpa_id");
            if (!rs.wasNull()) {
                mpaStorage.findById(mpaId).ifPresent(film::setMpa);
            }

            return film;
        }
    };

    @Override
    public List<Film> findAll() {
        log.debug("Запрос всех фильмов из БД");
        String sql = "SELECT * FROM films ORDER BY film_id";
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper);

        for (Film film : films) {
            loadFilmDetails(film);
        }

        log.info("Загружено {} фильмов из БД", films.size());
        return films;
    }

    @Override
    public Film create(Film film) {
        log.info("Создание фильма: '{}'", film.getName());

        validateFilmForDatabase(film);

        String sql = "INSERT INTO films (title, description, release_date, duration, mpa_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setLong(5, film.getMpa().getId());
            return ps;
        }, keyHolder);

        film.setId(keyHolder.getKey().longValue());

        // Сохраняем жанры
        saveFilmGenres(film);

        log.info("✅ Фильм создан с ID: {}", film.getId());
        return findById(film.getId());
    }

    @Override
    public Film update(Film film) {
        log.info("Обновление фильма ID={}: '{}'", film.getId(), film.getName());

        validateFilmForDatabase(film);

        // Проверяем существование
        if (!existsById(film.getId())) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }

        String sql = "UPDATE films SET title = ?, description = ?, release_date = ?, " +
                "duration = ?, mpa_id = ? WHERE film_id = ?";

        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId()
        );

        // Обновляем жанры
        updateFilmGenres(film);

        log.info("✅ Фильм обновлён: ID={}", film.getId());
        return findById(film.getId());
    }

    @Override
    public Film findById(Long id) {
        log.debug("Поиск фильма по ID: {}", id);

        String sql = "SELECT * FROM films WHERE film_id = ?";
        try {
            Film film = jdbcTemplate.queryForObject(sql, filmRowMapper, id);
            loadFilmDetails(film);
            return film;
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
    }

    @Override
    public void delete(Long id) {
        log.info("Удаление фильма ID={}", id);

        String sql = "DELETE FROM films WHERE film_id = ?";
        int rows = jdbcTemplate.update(sql, id);

        if (rows == 0) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }

        log.info("✅ Фильм удалён: ID={}", id);
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        log.info("Добавление лайка: фильм {}, пользователь {}", filmId, userId);

        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
        try {
            jdbcTemplate.update(sql, filmId, userId);
            log.info("✅ Лайк добавлен");
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage().contains("PRIMARY KEY") || e.getMessage().contains("unique constraint")) {
                throw new IllegalArgumentException("Пользователь уже поставил лайк этому фильму");
            }
            throw e;
        }
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        log.info("Удаление лайка: фильм {}, пользователь {}", filmId, userId);

        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        int rows = jdbcTemplate.update(sql, filmId, userId);

        if (rows == 0) {
            throw new IllegalArgumentException("Лайк не найден");
        }

        log.info("✅ Лайк удалён");
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        log.info("Запрос популярных фильмов, count={}", count);

        if (count <= 0) {
            throw new IllegalArgumentException("Количество должно быть положительным");
        }

        String sql = "SELECT f.*, COUNT(l.user_id) as like_count " +
                "FROM films f " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "GROUP BY f.film_id " +
                "ORDER BY like_count DESC, f.film_id " +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, count);

        for (Film film : films) {
            loadFilmDetails(film);
        }

        log.info("Получено {} популярных фильмов", films.size());
        return films;
    }

    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM films WHERE film_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    private void loadFilmDetails(Film film) {
        if (film == null || film.getId() == null) return;
        loadFilmGenres(film);
        loadFilmLikes(film);
    }

    private void loadFilmGenres(Film film) {
        String sql = "SELECT g.* FROM genres g " +
                "JOIN film_genres fg ON g.genre_id = fg.genre_id " +
                "WHERE fg.film_id = ? " +
                "ORDER BY g.genre_id";

        try {
            List<Genre> genres = jdbcTemplate.query(sql, new RowMapper<Genre>() {
                @Override
                public Genre mapRow(ResultSet rs, int rowNum) throws SQLException {
                    Genre genre = new Genre();
                    genre.setId(rs.getLong("genre_id"));
                    genre.setName(rs.getString("name"));
                    return genre;
                }
            }, film.getId());

            film.setGenres(new LinkedHashSet<>(genres));
        } catch (Exception e) {
            log.error("Ошибка при загрузке жанров для фильма {}: {}", film.getId(), e.getMessage());
            film.setGenres(new HashSet<>());
        }
    }

    private void loadFilmLikes(Film film) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
        try {
            List<Long> likes = jdbcTemplate.query(sql,
                    (rs, rowNum) -> rs.getLong("user_id"), film.getId());
            film.setLikes(new HashSet<>(likes));
            log.debug("Загружено {} лайков для фильма {}", likes.size(), film.getId());
        } catch (Exception e) {
            log.error("Ошибка при загрузке лайков для фильма {}: {}", film.getId(), e.getMessage());
            film.setLikes(new HashSet<>());
        }
    }

    private void saveFilmGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";

        for (Genre genre : film.getGenres()) {
            jdbcTemplate.update(sql, film.getId(), genre.getId());
        }
    }

    private void updateFilmGenres(Film film) {
        // Удаляем старые жанры
        String deleteSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, film.getId());

        // Добавляем новые
        saveFilmGenres(film);
    }

    private void validateFilmForDatabase(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new IllegalArgumentException("MPA рейтинг должен быть указан");
        }

        // Проверяем существование MPA
        if (!mpaStorage.existsById(film.getMpa().getId())) {
            throw new NotFoundException("MPA рейтинг с id=" + film.getMpa().getId() + " не найден");
        }

        // Проверяем существование жанров
        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                if (!genreStorage.existsById(genre.getId())) {
                    throw new NotFoundException("Жанр с id=" + genre.getId() + " не найден");
                }
            }
        }
    }
}