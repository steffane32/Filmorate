package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

@Slf4j
@Repository
@Primary
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        log.info("UserDbStorage инициализирован");
    }

    private final RowMapper<User> userRowMapper = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getLong("user_id"));
            user.setEmail(rs.getString("email"));
            user.setLogin(rs.getString("login"));
            user.setName(rs.getString("user_name"));

            Date birthday = rs.getDate("birthday");
            if (birthday != null) {
                user.setBirthday(birthday.toLocalDate());
            }

            // Загружаем друзей для пользователя
            Long userId = user.getId();
            user.getFriends().addAll(getUserFriends(userId));

            return user;
        }
    };

    @Override
    public List<User> findAll() {
        log.info("Запрос всех пользователей из БД");
        String sql = "SELECT * FROM users ORDER BY user_id";
        List<User> users = jdbcTemplate.query(sql, userRowMapper);
        log.info("Загружено {} пользователей из БД", users.size());
        return users;
    }

    @Override
    public User create(User user) {
        log.info("Сохранение пользователя в БД: email={}", user.getEmail());

        String sql = "INSERT INTO users (email, login, user_name, birthday) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, user.getEmail());
                ps.setString(2, user.getLogin());
                ps.setString(3, user.getName());

                if (user.getBirthday() != null) {
                    ps.setDate(4, Date.valueOf(user.getBirthday()));
                } else {
                    ps.setDate(4, null);
                }

                return ps;
            }, keyHolder);

            // Получаем сгенерированный ID
            Number key = keyHolder.getKey();
            if (key != null) {
                user.setId(key.longValue());
                log.info("Пользователь сохранён в БД с ID: {}, email={}", user.getId(), user.getEmail());
            } else {
                log.error("Не удалось получить сгенерированный ID для пользователя");
                throw new RuntimeException("Не удалось получить ID пользователя");
            }

        } catch (DataAccessException e) {
            log.error("Ошибка при сохранении пользователя в БД: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при сохранении пользователя", e);
        }

        return user;
    }

    @Override
    public User update(User user) {
        log.info("Обновление пользователя в БД: ID={}", user.getId());

        if (user.getId() == null) {
            throw new IllegalArgumentException("ID пользователя должен быть указан");
        }

        String sql = "UPDATE users SET email = ?, login = ?, user_name = ?, birthday = ? WHERE user_id = ?";

        try {
            int rowsUpdated = jdbcTemplate.update(sql,
                    user.getEmail(),
                    user.getLogin(),
                    user.getName(),
                    user.getBirthday() != null ? Date.valueOf(user.getBirthday()) : null,
                    user.getId()
            );

            if (rowsUpdated == 0) {
                throw new NotFoundException("Пользователь с id=" + user.getId() + " не найден");
            }

            log.info("Пользователь обновлён в БД: ID={}", user.getId());
            return findById(user.getId());

        } catch (DataAccessException e) {
            log.error("Ошибка при обновлении пользователя в БД: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при обновлении пользователя", e);
        }
    }

    @Override
    public User findById(Long id) {
        log.info("Поиск пользователя в БД по ID: {}", id);

        if (id == null) {
            throw new IllegalArgumentException("ID пользователя не может быть null");
        }

        try {
            String sql = "SELECT * FROM users WHERE user_id = ?";
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, id);
            log.info("Пользователь найден: ID={}, email={}", user.getId(), user.getEmail());
            return user;
        } catch (EmptyResultDataAccessException e) {
            log.error("Пользователь с ID={} не найден в БД", id);
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
    }

    @Override
    public void delete(Long id) {
        log.info("Удаление пользователя из БД: ID={}", id);

        String sql = "DELETE FROM users WHERE user_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, id);

        if (rowsDeleted == 0) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }

        log.info("Пользователь удалён из БД: ID={}", id);
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        log.info("Добавление друга (одностороннее): {} -> {}", userId, friendId);

        // Проверяем существование пользователей
        findById(userId);
        findById(friendId);

        // Проверяем, что не добавляют самого себя
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Пользователь не может добавить самого себя в друзья");
        }

        try {
            // Добавляем одностороннюю дружбу
            String sql = "INSERT INTO friendships (user_id, friend_id) VALUES (?, ?)";
            jdbcTemplate.update(sql, userId, friendId);
            log.info("Друг добавлен (односторонне): {} -> {}", userId, friendId);
        } catch (DataAccessException e) {
            if (e.getMessage().contains("PRIMARY KEY") || e.getMessage().contains("unique constraint")) {
                log.info("Пользователь уже в друзьях: {} -> {}", userId, friendId);
                // Если уже друзья, не выбрасываем исключение - просто игнорируем
                return;
            }
            log.error("Ошибка при добавлении друга в БД: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось добавить друга", e);
        }
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        log.info("Удаление друга (одностороннее): {} -> {}", userId, friendId);

        // Проверяем существование пользователей
        findById(userId);
        findById(friendId);

        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, userId, friendId);

        if (rowsDeleted == 0) {
            log.info("Дружба не найдена: {} -> {}", userId, friendId);
            // В односторонней дружбе не нужно выбрасывать исключение, если дружбы нет
            // Просто ничего не делаем
            return;
        }

        log.info("Друг удалён (односторонне): {} -> {}", userId, friendId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        log.info("Получение друзей пользователя (одностороннее): ID={}", userId);

        // Проверяем существование пользователя
        findById(userId);

        String sql = "SELECT u.* FROM users u " +
                "JOIN friendships f ON u.user_id = f.friend_id " +
                "WHERE f.user_id = ? " +
                "ORDER BY u.user_id";

        try {
            List<User> friends = jdbcTemplate.query(sql, userRowMapper, userId);
            log.info("Найдено {} друзей для пользователя {}", friends.size(), userId);
            return friends;
        } catch (Exception e) {
            log.error("Ошибка при получении друзей пользователя {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении друзей пользователя", e);
        }
    }

    @Override
    public List<User> getCommonFriends(Long userId1, Long userId2) {
        log.info("Поиск общих друзей: {} и {}", userId1, userId2);

        // Проверяем существование пользователей
        findById(userId1);
        findById(userId2);

        String sql = "SELECT u.* FROM users u " +
                "WHERE u.user_id IN (" +
                "  SELECT f1.friend_id FROM friendships f1 " +
                "  WHERE f1.user_id = ?" +
                ") " +
                "AND u.user_id IN (" +
                "  SELECT f2.friend_id FROM friendships f2 " +
                "  WHERE f2.user_id = ?" +
                ") " +
                "ORDER BY u.user_id";

        try {
            List<User> commonFriends = jdbcTemplate.query(sql, userRowMapper, userId1, userId2);
            log.info("Найдено {} общих друзей для {} и {}", commonFriends.size(), userId1, userId2);
            return commonFriends;
        } catch (Exception e) {
            log.error("Ошибка при поиске общих друзей для {} и {}: {}", userId1, userId2, e.getMessage(), e);
            throw new RuntimeException("Ошибка при поиске общих друзей", e);
        }
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Ошибка при проверке существования пользователя {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Ошибка при проверке существования пользователя", e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Ошибка при проверке email {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Ошибка при проверке email", e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, email);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Ошибка при поиске пользователя по email {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Ошибка при поиске пользователя по email", e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM users";
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Ошибка при подсчете пользователей: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при подсчете пользователей", e);
        }
    }

    @Override
    public List<User> findUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        String inClause = String.join(",", Collections.nCopies(userIds.size(), "?"));
        String sql = String.format("SELECT * FROM users WHERE user_id IN (%s) ORDER BY user_id", inClause);

        try {
            return jdbcTemplate.query(sql, userRowMapper, userIds.toArray());
        } catch (Exception e) {
            log.error("Ошибка при поиске пользователей по IDs {}: {}", userIds, e.getMessage(), e);
            throw new RuntimeException("Ошибка при поиске пользователей по IDs", e);
        }
    }

    @Override
    public void confirmFriendship(Long userId, Long friendId) {
        log.info("Подтверждение дружбы между {} и {}", userId, friendId);
        // В односторонней дружбе подтверждение не требуется
        addFriend(userId, friendId);
    }

    /**
     * Получает ID друзей пользователя
     */
    private Set<Long> getUserFriends(Long userId) {
        String sql = "SELECT friend_id FROM friendships WHERE user_id = ?";
        try {
            List<Long> friendIds = jdbcTemplate.query(sql,
                    (rs, rowNum) -> rs.getLong("friend_id"), userId);
            return new HashSet<>(friendIds);
        } catch (Exception e) {
            log.error("Ошибка при получении друзей пользователя {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении друзей пользователя", e);
        }
    }

    /**
     * Проверяет, являются ли пользователи друзьями (односторонне)
     */
    private boolean isFriends(Long userId1, Long userId2) {
        String sql = "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ?";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId1, userId2);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Ошибка при проверке дружбы между {} и {}: {}", userId1, userId2, e.getMessage(), e);
            throw new RuntimeException("Ошибка при проверке дружбы", e);
        }
    }
}