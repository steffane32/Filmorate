package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Slf4j
@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private long nextId = 1;

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public User create(User user) {
        user.setId(nextId++);

        // Создаем копию пользователя с новым сетом друзей
        User userToSave = copyUser(user);
        users.put(userToSave.getId(), userToSave);

        log.info("Создан пользователь с ID: {}", userToSave.getId());
        return userToSave;
    }

    @Override
    public User update(User user) {
        Long id = user.getId();

        if (id == null) {
            throw new IllegalArgumentException("ID пользователя должен быть указан");
        }

        if (!users.containsKey(id)) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }

        // Создаем копию
        User userToSave = copyUser(user);
        users.put(id, userToSave);

        log.info("Обновлен пользователь с ID: {}", id);
        return userToSave;
    }

    @Override
    public User findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID пользователя не может быть null");
        }

        User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }

        // Возвращаем копию
        return copyUser(user);
    }

    @Override
    public void delete(Long id) {
        if (!users.containsKey(id)) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
        users.remove(id);
    }

    // Метод для глубокого копирования пользователя
    private User copyUser(User original) {
        User copy = new User();
        copy.setId(original.getId());
        copy.setEmail(original.getEmail());
        copy.setLogin(original.getLogin());
        copy.setName(original.getName());
        copy.setBirthday(original.getBirthday());

        // Копируем сет друзей
        Set<Long> friendsCopy = new HashSet<>(original.getFriends());
        copy.setFriends(friendsCopy);

        return copy;
    }
}