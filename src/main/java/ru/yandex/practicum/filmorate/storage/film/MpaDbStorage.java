package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class MpaDbStorage implements MpaStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MpaDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        log.info("MpaDbStorage инициализирован");
    }

    private final RowMapper<Mpa> mpaRowMapper = new RowMapper<Mpa>() {
        @Override
        public Mpa mapRow(ResultSet rs, int rowNum) throws SQLException {
            Mpa mpa = new Mpa();
            mpa.setId(rs.getLong("mpa_id"));
            mpa.setName(rs.getString("name"));
            mpa.setDescription(rs.getString("description"));
            return mpa;
        }
    };

    @Override
    public List<Mpa> findAll() {
        log.debug("Запрос всех рейтингов MPA из БД");
        String sql = "SELECT * FROM mpa_ratings ORDER BY mpa_id";
        List<Mpa> mpaList = jdbcTemplate.query(sql, mpaRowMapper);
        log.info("Загружено {} рейтингов MPA из БД", mpaList.size());
        return mpaList;
    }

    @Override
    public Optional<Mpa> findById(Long id) {
        log.debug("Поиск рейтинга MPA по ID: {}", id);
        String sql = "SELECT * FROM mpa_ratings WHERE mpa_id = ?";
        try {
            Mpa mpa = jdbcTemplate.queryForObject(sql, mpaRowMapper, id);
            return Optional.ofNullable(mpa);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Рейтинг MPA с ID {} не найден", id);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Mpa> findByName(String name) {
        log.debug("Поиск рейтинга MPA по имени: {}", name);
        String sql = "SELECT * FROM mpa_ratings WHERE name = ?";
        try {
            Mpa mpa = jdbcTemplate.queryForObject(sql, mpaRowMapper, name);
            return Optional.ofNullable(mpa);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Рейтинг MPA с именем '{}' не найден", name);
            return Optional.empty();
        }
    }

    @Override
    public Mpa create(Mpa mpa) {
        log.info("Создание рейтинга MPA: {}", mpa.getName());

        String sql = "INSERT INTO mpa_ratings (name, description) VALUES (?, ?)";

        jdbcTemplate.update(sql, mpa.getName(), mpa.getDescription());

        // Получаем ID
        String getIdSql = "SELECT mpa_id FROM mpa_ratings WHERE name = ?";
        Long id = jdbcTemplate.queryForObject(getIdSql, Long.class, mpa.getName());
        mpa.setId(id);

        log.info("✅ Рейтинг MPA создан с ID: {}", id);
        return mpa;
    }

    @Override
    public Mpa update(Mpa mpa) {
        log.info("Обновление рейтинга MPA: ID={}, name={}", mpa.getId(), mpa.getName());

        String sql = "UPDATE mpa_ratings SET name = ?, description = ? WHERE mpa_id = ?";

        int rowsUpdated = jdbcTemplate.update(sql,
                mpa.getName(),
                mpa.getDescription(),
                mpa.getId()
        );

        if (rowsUpdated == 0) {
            log.warn("Рейтинг MPA с ID={} не найден для обновления", mpa.getId());
            throw new NotFoundException("Рейтинг MPA с id=" + mpa.getId() + " не найден");
        }

        log.info("✅ Рейтинг MPA обновлён: ID={}", mpa.getId());
        return mpa;
    }

    @Override
    public void delete(Long id) {
        log.info("Удаление рейтинга MPA: ID={}", id);

        String sql = "DELETE FROM mpa_ratings WHERE mpa_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, id);

        if (rowsDeleted == 0) {
            log.warn("Рейтинг MPA с ID={} не найден для удаления", id);
            throw new NotFoundException("Рейтинг MPA с id=" + id + " не найден");
        }

        log.info("✅ Рейтинг MPA удалён: ID={}", id);
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM mpa_ratings WHERE mpa_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}