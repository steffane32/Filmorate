package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.ArrayList;
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

        // Проверяем существование пользователя
        userStorage.findById(user.getId());

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

        Set<Long> userFriends = user.getFriends();
        Set<Long> friendFriends = friend.getFriends();

        if (userFriends.contains(friendId)) {
            throw new ValidationException("Пользователь уже добавлен в друзья");
        }

        // Создаем новых пользователей с обновленными друзьями
        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setEmail(user.getEmail());
        updatedUser.setLogin(user.getLogin());
        updatedUser.setName(user.getName());
        updatedUser.setBirthday(user.getBirthday());
        updatedUser.setFriends(new java.util.HashSet<>(userFriends));
        updatedUser.getFriends().add(friendId);

        User updatedFriend = new User();
        updatedFriend.setId(friend.getId());
        updatedFriend.setEmail(friend.getEmail());
        updatedFriend.setLogin(friend.getLogin());
        updatedFriend.setName(friend.getName());
        updatedFriend.setBirthday(friend.getBirthday());
        updatedFriend.setFriends(new java.util.HashSet<>(friendFriends));
        updatedFriend.getFriends().add(userId);

        // Сохраняем
        userStorage.update(updatedUser);
        userStorage.update(updatedFriend);
    }

    public void removeFriend(Long userId, Long friendId) {
        User user = userStorage.findById(userId);
        User friend = userStorage.findById(friendId);

        Set<Long> userFriends = user.getFriends();
        Set<Long> friendFriends = friend.getFriends();

        if (!userFriends.contains(friendId)) {
            throw new ValidationException("Пользователь не является другом");
        }

        // Создаем новых пользователей с обновленными друзьями
        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setEmail(user.getEmail());
        updatedUser.setLogin(user.getLogin());
        updatedUser.setName(user.getName());
        updatedUser.setBirthday(user.getBirthday());
        updatedUser.setFriends(new java.util.HashSet<>(userFriends));
        updatedUser.getFriends().remove(friendId);

        User updatedFriend = new User();
        updatedFriend.setId(friend.getId());
        updatedFriend.setEmail(friend.getEmail());
        updatedFriend.setLogin(friend.getLogin());
        updatedFriend.setName(friend.getName());
        updatedFriend.setBirthday(friend.getBirthday());
        updatedFriend.setFriends(new java.util.HashSet<>(friendFriends));
        updatedFriend.getFriends().remove(userId);

        // Сохраняем
        userStorage.update(updatedUser);
        userStorage.update(updatedFriend);
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

        Set<Long> commonIds = new java.util.HashSet<>(user1.getFriends());
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