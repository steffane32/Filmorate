package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public List<User> findAll() {
        return userStorage.findAll();
    }

    public User create(User user) {
        validateUser(user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        return userStorage.create(user);
    }

    public User update(User user) {
        validateUser(user);

        if (user.getId() == null) {
            throw new ValidationException("ID пользователя должен быть указан");
        }

        userStorage.findById(user.getId()); // Проверяем существование
        return userStorage.update(user);
    }

    public User findById(Long id) {
        return userStorage.findById(id);
    }

    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Пользователь не может добавить сам себя в друзья");
        }

        User user = userStorage.findById(userId);
        User friend = userStorage.findById(friendId);

        if (user.getFriends().contains(friendId)) {
            throw new ValidationException("Пользователь уже добавлен в друзья");
        }

        user.getFriends().add(friendId); // Работаем с существующими объектами
        friend.getFriends().add(userId);
    }

    public void removeFriend(Long userId, Long friendId) {
        User user = userStorage.findById(userId);
        User friend = userStorage.findById(friendId);

        if (!user.getFriends().remove(friendId)) {
            throw new ValidationException("Пользователь не является другом");
        }

        friend.getFriends().remove(userId);
    }

    public List<User> getFriends(Long userId) {
        User user = userStorage.findById(userId);
        List<User> friends = new ArrayList<>();

        for (Long friendId : user.getFriends()) {
            friends.add(userStorage.findById(friendId));
        }

        return friends;
    }

    public List<User> getCommonFriends(Long userId1, Long userId2) {
        User user1 = userStorage.findById(userId1);
        User user2 = userStorage.findById(userId2);

        Set<Long> commonIds = new HashSet<>(user1.getFriends());
        commonIds.retainAll(user2.getFriends());

        List<User> commonFriends = new ArrayList<>();
        for (Long friendId : commonIds) {
            commonFriends.add(userStorage.findById(friendId));
        }

        return commonFriends;
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            throw new ValidationException("Некорректный email");
        }

        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            throw new ValidationException("Логин не может быть пустым или содержать пробелы");
        }

        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }
}