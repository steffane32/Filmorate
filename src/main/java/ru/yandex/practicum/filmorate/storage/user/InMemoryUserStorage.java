package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@Qualifier("inMemoryUserStorage")
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private final Map<Long, Set<Long>> friendships = new HashMap<>();
    private long nextId = 1;

    @Override
    public List<User> findAll() {
        log.debug("Получение всех пользователей, всего: {}", users.size());
        return new ArrayList<>(users.values());
    }

    @Override
    public User create(User user) {
        log.info("Создание пользователя: {}", user.getEmail());

        // Валидация основных полей
        validateUserFields(user);

        // Устанавливаем ID
        user.setId(nextId++);

        // Сохраняем пользователя
        users.put(user.getId(), user);

        // Инициализируем пустой список друзей
        friendships.put(user.getId(), new HashSet<>());

        log.info("Пользователь создан с ID: {}", user.getId());
        return user;
    }

    @Override
    public User update(User user) {
        log.info("Обновление пользователя с ID: {}", user.getId());

        Long id = user.getId();

        if (id == null) {
            throw new IllegalArgumentException("ID пользователя должен быть указан");
        }

        if (!users.containsKey(id)) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }

        // Валидация основных полей
        validateUserFields(user);

        // Обновляем пользователя
        users.put(id, user);

        log.info("Пользователь с ID {} обновлен", id);
        return user;
    }

    @Override
    public User findById(Long id) {
        log.debug("Поиск пользователя по ID: {}", id);

        if (id == null) {
            throw new IllegalArgumentException("ID пользователя не может быть null");
        }

        User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }

        return user;
    }

    @Override
    public void delete(Long id) {
        log.info("Удаление пользователя с ID: {}", id);

        if (!users.containsKey(id)) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }

        // Удаляем пользователя
        users.remove(id);

        // Удаляем его из списков друзей
        friendships.remove(id);

        // Удаляем его из списков друзей других пользователей
        for (Set<Long> friendSet : friendships.values()) {
            friendSet.remove(id);
        }

        log.info("Пользователь с ID {} удален", id);
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        log.info("Добавление в друзья: {} -> {}", userId, friendId);

        // Проверяем существование пользователей
        validateUsersExist(userId, friendId);

        // Проверяем, что не добавляют сами себя
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Пользователь не может добавить сам себя в друзья");
        }

        // Получаем списки друзей
        Set<Long> userFriends = friendships.get(userId);
        Set<Long> friendFriends = friendships.get(friendId);

        // Проверяем, не друзья ли уже
        if (userFriends.contains(friendId)) {
            throw new IllegalArgumentException("Пользователь уже в друзьях");
        }

        // Добавляем взаимную дружбу
        userFriends.add(friendId);
        friendFriends.add(userId);

        log.info("Дружба добавлена: {} и {}", userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        log.info("Удаление из друзей: {} -> {}", userId, friendId);

        // Проверяем существование пользователей
        validateUsersExist(userId, friendId);

        // Получаем списки друзей
        Set<Long> userFriends = friendships.get(userId);
        Set<Long> friendFriends = friendships.get(friendId);

        // Проверяем, являются ли они друзьями
        if (!userFriends.contains(friendId)) {
            throw new IllegalArgumentException("Пользователь не является другом");
        }

        // Удаляем взаимную дружбу
        userFriends.remove(friendId);
        friendFriends.remove(userId);

        log.info("Дружба удалена: {} и {}", userId, friendId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        log.debug("Получение друзей пользователя с ID: {}", userId);

        // Проверяем существование пользователя
        validateUserExists(userId);

        // Получаем ID друзей
        Set<Long> friendIds = friendships.get(userId);
        List<User> friends = new ArrayList<>();

        // Получаем объекты пользователей
        for (Long friendId : friendIds) {
            User friend = users.get(friendId);
            if (friend != null) {
                friends.add(friend);
            }
        }

        log.debug("Найдено {} друзей для пользователя {}", friends.size(), userId);
        return friends;
    }

    @Override
    public List<User> getCommonFriends(Long userId1, Long userId2) {
        log.debug("Поиск общих друзей для {} и {}", userId1, userId2);

        // Проверяем существование пользователей
        validateUserExists(userId1);
        validateUserExists(userId2);

        // Получаем списки друзей
        Set<Long> friends1 = friendships.get(userId1);
        Set<Long> friends2 = friendships.get(userId2);

        // Находим пересечение (общих друзей)
        Set<Long> commonIds = new HashSet<>(friends1);
        commonIds.retainAll(friends2);

        // Получаем объекты пользователей
        List<User> commonFriends = new ArrayList<>();
        for (Long friendId : commonIds) {
            User friend = users.get(friendId);
            if (friend != null) {
                commonFriends.add(friend);
            }
        }

        log.debug("Найдено {} общих друзей для {} и {}", commonFriends.size(), userId1, userId2);
        return commonFriends;
    }

    // ============ НОВЫЕ МЕТОДЫ ИЗ ИНТЕРФЕЙСА ============

    @Override
    public boolean existsById(Long id) {
        return users.containsKey(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return users.values().stream()
                .anyMatch(user -> user.getEmail().equals(email));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.values().stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst();
    }

    @Override
    public long count() {
        return users.size();
    }

    @Override
    public List<User> findUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        return userIds.stream()
                .filter(users::containsKey)
                .map(users::get)
                .collect(Collectors.toList());
    }

    @Override
    public void confirmFriendship(Long userId, Long friendId) {
        log.info("Подтверждение дружбы между {} и {}", userId, friendId);

        // В in-memory хранилище дружба уже двусторонняя, так что просто логируем
        log.info("Дружба уже подтверждена в in-memory хранилище");
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    private void validateUserFields(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new IllegalArgumentException("Логин не может быть пустым");
        }

        if (user.getLogin().contains(" ")) {
            throw new IllegalArgumentException("Логин не может содержать пробелы");
        }
    }

    private void validateUserExists(Long userId) {
        if (!users.containsKey(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }

    private void validateUsersExist(Long userId1, Long userId2) {
        validateUserExists(userId1);
        validateUserExists(userId2);
    }
}