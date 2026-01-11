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

            log.debug("Сопоставлен пользователь из БД: ID={}, email={}",
                    user.getId(), user.getEmail());
            return user;
        }
    };

    @Override
    public List<User> findAll() {
        log.debug("Запрос всех пользователей из БД");
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
                log.info("✅ Пользователь сохранён в БД с ID: {}, email={}",
                        user.getId(), user.getEmail());
            } else {
                log.warn("Не удалось получить сгенерированный ID для пользователя");
                // Альтернативный способ получить ID
                String getIdSql = "SELECT user_id FROM users WHERE email = ?";
                try {
                    Long id = jdbcTemplate.queryForObject(getIdSql, Long.class, user.getEmail());
                    user.setId(id);
                    log.info("Получен ID из отдельного запроса: {}", id);
                } catch (Exception e) {
                    log.error("Не удалось получить ID пользователя: {}", e.getMessage());
                    throw new RuntimeException("Не удалось получить ID пользователя", e);
                }
            }

        } catch (DataAccessException e) {
            if (e.getMessage().contains("Unique index or primary key violation") ||
                    e.getMessage().contains("constraint") && e.getMessage().contains("email")) {
                log.error("Пользователь с email {} уже существует", user.getEmail());
                throw new IllegalArgumentException("Пользователь с email " + user.getEmail() + " уже существует");
            }
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

        // Проверяем существование пользователя
        if (!existsById(user.getId())) {
            throw new NotFoundException("Пользователь с id=" + user.getId() + " не найден");
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

            log.info("✅ Пользователь обновлён в БД: ID={}, строк обновлено: {}", user.getId(), rowsUpdated);
            return findById(user.getId());

        } catch (DataAccessException e) {
            if (e.getMessage().contains("Unique index or primary key violation") ||
                    e.getMessage().contains("constraint") && e.getMessage().contains("email")) {
                log.error("Пользователь с email {} уже существует", user.getEmail());
                throw new IllegalArgumentException("Пользователь с email " + user.getEmail() + " уже существует");
            }
            log.error("Ошибка при обновлении пользователя в БД: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при обновлении пользователя", e);
        }
    }

    @Override
    public User findById(Long id) {
        log.debug("Поиск пользователя в БД по ID: {}", id);

        if (id == null) {
            throw new IllegalArgumentException("ID пользователя не может быть null");
        }

        try {
            String sql = "SELECT * FROM users WHERE user_id = ?";
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, id);

            // Загружаем друзей для пользователя
            loadUserFriends(user);

            log.debug("Пользователь найден: ID={}, email={}", user.getId(), user.getEmail());
            return user;
        } catch (EmptyResultDataAccessException e) {
            log.warn("Пользователь с ID={} не найден в БД", id);
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
    }

    @Override
    public void delete(Long id) {
        log.info("Удаление пользователя из БД: ID={}", id);

        // Проверяем существование пользователя
        if (!existsById(id)) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }

        String sql = "DELETE FROM users WHERE user_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, id);

        log.info("✅ Пользователь удалён из БД: ID={}, строк удалено: {}", id, rowsDeleted);
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        log.info("Добавление друга (одностороннее): {} -> {}", userId, friendId);

        // Проверяем, что пользователи существуют
        if (!existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        if (!existsById(friendId)) {
            throw new NotFoundException("Пользователь с id=" + friendId + " не найден");
        }

        // Проверяем, что не добавляют самого себя
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Пользователь не может добавить самого себя в друзья");
        }

        // Проверяем, не являются ли уже друзьями
        if (isFriends(userId, friendId)) {
            throw new IllegalArgumentException("Пользователь уже в друзьях");
        }

        try {
            // Исправляем: используем 'REQUESTED' вместо 'PENDING'
            String sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'REQUESTED')";
            jdbcTemplate.update(sql, userId, friendId);
            log.info("✅ Друг добавлен (односторонне): {} -> {}", userId, friendId);
        } catch (DataAccessException e) {
            if (e.getMessage().contains("PRIMARY KEY") || e.getMessage().contains("unique constraint")) {
                throw new IllegalArgumentException("Пользователь уже в друзьях");
            }
            log.error("Ошибка при добавлении друга в БД: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось добавить друга: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        log.info("Удаление друга (одностороннее): {} -> {}", userId, friendId);

        // Проверяем существование пользователей
        if (!existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        if (!existsById(friendId)) {
            throw new NotFoundException("Пользователь с id=" + friendId + " не найден");
        }

        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, userId, friendId);

        // ИЗМЕНЕНИЕ: Не бросаем исключение, если дружбы не существует
        // Просто логируем и завершаем операцию
        if (rowsDeleted == 0) {
            log.info("⚠️ Дружба не найдена (или уже удалена): {} -> {}", userId, friendId);
            // Не бросаем исключение - удаление несуществующей дружбы считается успешным
            return;
        }

        log.info("✅ Друг удалён (односторонне): {} -> {}", userId, friendId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        log.debug("Получение друзей пользователя (одностороннее): ID={}", userId);

        // Проверяем существование пользователя
        if (!existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        // Односторонняя дружба: получаем только тех, кого пользователь добавил в друзья
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendships f ON u.user_id = f.friend_id " +
                "WHERE f.user_id = ? " +
                "ORDER BY u.user_id";

        List<User> friends = jdbcTemplate.query(sql, userRowMapper, userId);
        log.info("Найдено {} друзей для пользователя {}", friends.size(), userId);
        return friends;
    }

    @Override
    public List<User> getCommonFriends(Long userId1, Long userId2) {
        log.debug("Поиск общих друзей: {} и {}", userId1, userId2);

        // Проверяем существование пользователей
        if (!existsById(userId1)) {
            throw new NotFoundException("Пользователь с id=" + userId1 + " не найден");
        }
        if (!existsById(userId2)) {
            throw new NotFoundException("Пользователь с id=" + userId2 + " не найден");
        }

        // Общие друзья: пользователи, которых ОБА добавили в друзья
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

        List<User> commonFriends = jdbcTemplate.query(sql, userRowMapper, userId1, userId2);
        log.info("Найдено {} общих друзей для {} и {}", commonFriends.size(), userId1, userId2);
        return commonFriends;
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Ошибка при проверке существования пользователя {}: {}", id, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Ошибка при проверке email {}: {}", email, e.getMessage());
            return false;
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
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM users";
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Ошибка при подсчете пользователей: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public List<User> findUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        String inClause = String.join(",", Collections.nCopies(userIds.size(), "?"));
        String sql = String.format("SELECT * FROM users WHERE user_id IN (%s) ORDER BY user_id", inClause);

        return jdbcTemplate.query(sql, userRowMapper, userIds.toArray());
    }

    @Override
    public void confirmFriendship(Long userId, Long friendId) {
        log.info("Подтверждение дружбы между {} и {}", userId, friendId);

        // Проверяем существование пользователей
        if (!existsById(userId) || !existsById(friendId)) {
            throw new NotFoundException("Один из пользователей не найден");
        }

        // Обновляем статус дружбы на CONFIRMED
        String sql = "UPDATE friendships SET status = 'CONFIRMED' " +
                "WHERE user_id = ? AND friend_id = ?";

        int rowsUpdated = jdbcTemplate.update(sql, userId, friendId);

        if (rowsUpdated == 0) {
            log.warn("Запрос дружбы не найден: {} -> {}", userId, friendId);
            // Можно создать обратную дружбу, если нужно
            sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'CONFIRMED')";
            jdbcTemplate.update(sql, userId, friendId);
        }

        log.info("✅ Дружба подтверждена между {} и {}", userId, friendId);
    }

    /**
     * Загружает друзей пользователя
     */
    private void loadUserFriends(User user) {
        if (user == null || user.getId() == null) {
            return;
        }

        String sql = "SELECT friend_id FROM friendships WHERE user_id = ?";
        try {
            List<Long> friendIds = jdbcTemplate.query(sql,
                    (rs, rowNum) -> rs.getLong("friend_id"), user.getId());

            // Очищаем текущий список и добавляем загруженных друзей
            user.getFriends().clear();
            user.getFriends().addAll(friendIds);

            log.debug("Загружено {} друзей для пользователя {}", friendIds.size(), user.getId());
        } catch (Exception e) {
            log.error("Ошибка при загрузке друзей пользователя {}: {}", user.getId(), e.getMessage());
        }
    }

    /**
     * Проверяет, являются ли пользователи друзьями (односторонняя проверка)
     */
    private boolean isFriends(Long userId1, Long userId2) {
        String sql = "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ?";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId1, userId2);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Ошибка при проверке дружбы между {} и {}: {}", userId1, userId2, e.getMessage());
            return false;
        }
    }

    /**
     * Получает ID подтвержденных друзей (двусторонняя проверка)
     */
    private Set<Long> getConfirmedFriends(Long userId) {
        String sql = "SELECT friend_id FROM friendships f1 " +
                "WHERE f1.user_id = ? AND f1.status = 'CONFIRMED' " +
                "AND EXISTS (" +
                "  SELECT 1 FROM friendships f2 " +
                "  WHERE f2.user_id = f1.friend_id AND f2.friend_id = ? AND f2.status = 'CONFIRMED'" +
                ")";
        try {
            List<Long> friendIds = jdbcTemplate.query(sql,
                    (rs, rowNum) -> rs.getLong("friend_id"), userId, userId);
            return new HashSet<>(friendIds);
        } catch (Exception e) {
            log.error("Ошибка при получении подтвержденных друзей пользователя {}: {}", userId, e.getMessage());
            return new HashSet<>();
        }
    }

    /**
     * Получает количество друзей пользователя
     */
    public int getFriendsCount(Long userId) {
        String sql = "SELECT COUNT(*) FROM friendships WHERE user_id = ?";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Ошибка при подсчете друзей пользователя {}: {}", userId, e.getMessage());
            return 0;
        }
    }

    /**
     * Проверяет, есть ли неподтвержденные заявки в друзья
     */
    public boolean hasPendingFriendRequest(Long fromUserId, Long toUserId) {
        String sql = "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ? AND status = 'REQUESTED'";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, fromUserId, toUserId);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Ошибка при проверке заявки в друзья {} -> {}: {}", fromUserId, toUserId, e.getMessage());
            return false;
        }
    }

    /**
     * Получает список пользователей, отправивших заявки в друзья
     */
    public List<User> getFriendRequests(Long userId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendships f ON u.user_id = f.user_id " +
                "WHERE f.friend_id = ? AND f.status = 'REQUESTED' " +
                "ORDER BY u.user_id";

        return jdbcTemplate.query(sql, userRowMapper, userId);
    }

    /**
     * Получает список пользователей, которым отправлены заявки в друзья
     */
    public List<User> getSentFriendRequests(Long userId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendships f ON u.user_id = f.friend_id " +
                "WHERE f.user_id = ? AND f.status = 'REQUESTED' " +
                "ORDER BY u.user_id";

        return jdbcTemplate.query(sql, userRowMapper, userId);
    }
}