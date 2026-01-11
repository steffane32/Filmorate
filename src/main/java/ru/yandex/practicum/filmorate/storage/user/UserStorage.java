package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;
import java.util.List;
import java.util.Optional;

public interface UserStorage {
    List<User> findAll();
    User create(User user);
    User update(User user);
    User findById(Long id);
    void delete(Long id);

    // Методы для работы с друзьями (односторонняя дружба)
    void addFriend(Long userId, Long friendId);
    void removeFriend(Long userId, Long friendId);
    List<User> getFriends(Long userId);
    List<User> getCommonFriends(Long userId1, Long userId2);

    // Дополнительные методы
    boolean existsById(Long id);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    long count();
    List<User> findUsersByIds(List<Long> userIds);
    void confirmFriendship(Long userId, Long friendId);
}