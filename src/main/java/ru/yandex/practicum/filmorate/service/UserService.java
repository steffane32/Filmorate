package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
        log.info("UserService инициализирован с хранилищем: {}",
                userStorage.getClass().getSimpleName());
    }

    public List<User> findAll() {
        log.info("Запрос на получение всех пользователей");
        List<User> users = userStorage.findAll();
        log.info("Получено {} пользователей", users.size());
        return users;
    }

    public User create(User user) {
        log.info("Создание нового пользователя: email={}", user.getEmail());

        try {
            validateUser(user);

            if (user.getName() == null || user.getName().isBlank()) {
                log.info("Имя пользователя пустое, устанавливаем логин: {}", user.getLogin());
                user.setName(user.getLogin());
            }

            User createdUser = userStorage.create(user);
            log.info("Пользователь успешно создан: ID={}, email={}",
                    createdUser.getId(), createdUser.getEmail());
            return createdUser;

        } catch (Exception e) {
            log.error("Ошибка при создании пользователя: {}", e.getMessage(), e);
            throw e;
        }
    }

    public User update(User user) {
        log.info("Обновление пользователя: ID={}, email={}",
                user.getId(), user.getEmail());

        try {
            validateUser(user);

            if (user.getId() == null) {
                log.error("Попытка обновления пользователя без ID");
                throw new ValidationException("ID пользователя должен быть указан");
            }

            userStorage.findById(user.getId());

            if (user.getName() == null || user.getName().isBlank()) {
                user.setName(user.getLogin());
            }

            User updatedUser = userStorage.update(user);
            log.info("Пользователь успешно обновлён: ID={}", updatedUser.getId());
            return updatedUser;

        } catch (Exception e) {
            log.error("Ошибка при обновлении пользователя: {}", e.getMessage(), e);
            throw e;
        }
    }

    public User findById(Long id) {
        log.info("Поиск пользователя по ID: {}", id);

        try {
            User user = userStorage.findById(id);
            log.info("Пользователь найден: ID={}, email={}", user.getId(), user.getEmail());
            return user;
        } catch (Exception e) {
            log.error("Ошибка при поиске пользователя: {}", e.getMessage());
            throw e;
        }
    }

    public void addFriend(Long userId, Long friendId) {
        log.info("Добавление в друзья: пользователь {} добавляет {}", userId, friendId);

        try {
            if (userId.equals(friendId)) {
                log.error("Попытка добавить себя в друзья: userId={}", userId);
                throw new ValidationException("Пользователь не может добавить сам себя в друзья");
            }

            userStorage.findById(userId);
            userStorage.findById(friendId);

            userStorage.addFriend(userId, friendId);
            log.info("Пользователь {} добавил в друзья пользователя {}", userId, friendId);

        } catch (Exception e) {
            log.error("Ошибка при добавлении в друзья: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void removeFriend(Long userId, Long friendId) {
        log.info("Удаление из друзей: пользователь {} удаляет {}", userId, friendId);

        try {
            userStorage.removeFriend(userId, friendId);
            log.info("Пользователь {} удалил из друзей пользователя {}", userId, friendId);

        } catch (Exception e) {
            log.error("Ошибка при удалении из друзей: {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<User> getFriends(Long userId) {
        log.info("Получение списка друзей для пользователя: {}", userId);

        try {
            userStorage.findById(userId);
            List<User> friends = userStorage.getFriends(userId);
            log.info("Найдено {} друзей для пользователя {}", friends.size(), userId);
            return friends;

        } catch (Exception e) {
            log.error("Ошибка при получении списка друзей: {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<User> getCommonFriends(Long userId1, Long userId2) {
        log.info("Поиск общих друзей для пользователей {} и {}", userId1, userId2);

        try {
            userStorage.findById(userId1);
            userStorage.findById(userId2);

            List<User> commonFriends = userStorage.getCommonFriends(userId1, userId2);
            log.info("Найдено {} общих друзей для пользователей {} и {}",
                    commonFriends.size(), userId1, userId2);
            return commonFriends;

        } catch (Exception e) {
            log.error("Ошибка при поиске общих друзей: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void validateUser(User user) {
        log.info("Валидация пользователя: email={}", user.getEmail());

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.error("Пустой email");
            throw new ValidationException("Email не может быть пустым");
        }

        if (!user.getEmail().contains("@")) {
            log.error("Некорректный email: отсутствует символ @ - '{}'", user.getEmail());
            throw new ValidationException("Email должен содержать символ @");
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.error("Пустой логин");
            throw new ValidationException("Логин не может быть пустым");
        }

        if (user.getLogin().contains(" ")) {
            log.error("Некорректный логин: содержит пробелы - '{}'", user.getLogin());
            throw new ValidationException("Логин не может содержать пробелы");
        }

        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.error("Некорректная дата рождения: {} (дата в будущем)", user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }

        log.info("Валидация пользователя пройдена успешно");
    }

    public boolean existsById(Long id) {
        try {
            findById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long count() {
        return findAll().size();
    }
}