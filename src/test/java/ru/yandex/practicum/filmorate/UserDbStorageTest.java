package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import(UserDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserDbStorageTest {

    private final UserDbStorage userStorage;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .login("testuser")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Test
    void createUser_ShouldReturnUserWithId() {
        // When
        User createdUser = userStorage.create(testUser);

        // Then
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getEmail()).isEqualTo("test@example.com");
        assertThat(createdUser.getLogin()).isEqualTo("testuser");
        assertThat(createdUser.getName()).isEqualTo("Test User");
    }

    @Test
    void findById_WhenUserExists_ShouldReturnUser() {
        // Given
        User createdUser = userStorage.create(testUser);
        Long userId = createdUser.getId();

        // When
        User foundUser = userStorage.findById(userId);

        // Then
        assertThat(foundUser).isEqualTo(createdUser);
    }

    @Test
    void findById_WhenUserNotExists_ShouldThrowException() {
        // When & Then
        assertThrows(ru.yandex.practicum.filmorate.exception.NotFoundException.class,
                () -> userStorage.findById(999L));
    }

    @Test
    void findAll_ShouldReturnAllUsers() {
        // Given
        User user1 = userStorage.create(testUser);

        User user2 = User.builder()
                .email("test2@example.com")
                .login("testuser2")
                .name("Test User 2")
                .birthday(LocalDate.of(1992, 2, 2))
                .build();
        userStorage.create(user2);

        // When
        List<User> users = userStorage.findAll();

        // Then
        assertThat(users).hasSize(2);
        assertThat(users).contains(user1, user2);
    }

    @Test
    void update_ShouldUpdateUser() {
        // Given
        User createdUser = userStorage.create(testUser);

        createdUser.setName("Updated Name");
        createdUser.setEmail("updated@example.com");

        // When
        User updatedUser = userStorage.update(createdUser);

        // Then
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");

        // Verify in database
        User foundUser = userStorage.findById(createdUser.getId());
        assertThat(foundUser.getName()).isEqualTo("Updated Name");
    }

    @Test
    void delete_ShouldRemoveUser() {
        // Given
        User createdUser = userStorage.create(testUser);
        Long userId = createdUser.getId();

        // When
        userStorage.delete(userId);

        // Then
        assertThrows(ru.yandex.practicum.filmorate.exception.NotFoundException.class,
                () -> userStorage.findById(userId));
    }

    @Test
    void addFriend_ShouldAddFriend() {
        // Given
        User user1 = userStorage.create(testUser);

        User user2 = new User();
        user2.setEmail("friend@example.com");
        user2.setLogin("friend");
        user2.setName("Friend User");
        user2.setBirthday(LocalDate.of(1995, 5, 5));
        user2 = userStorage.create(user2);

        // When
        userStorage.addFriend(user1.getId(), user2.getId());

        // Then
        List<User> friends = userStorage.getFriends(user1.getId());
        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).getId()).isEqualTo(user2.getId());
    }

    @Test
    void removeFriend_ShouldRemoveFriend() {
        // Given
        User user1 = userStorage.create(testUser);

        User user2 = new User();
        user2.setEmail("friend@example.com");
        user2.setLogin("friend");
        user2.setName("Friend User");
        user2.setBirthday(LocalDate.of(1995, 5, 5));
        user2 = userStorage.create(user2);

        userStorage.addFriend(user1.getId(), user2.getId());

        // When
        userStorage.removeFriend(user1.getId(), user2.getId());

        // Then
        List<User> friends = userStorage.getFriends(user1.getId());
        assertThat(friends).isEmpty();
    }

    @Test
    void getCommonFriends_ShouldReturnCommonFriends() {
        // Given
        User user1 = userStorage.create(testUser);

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setLogin("user2");
        user2.setName("User Two");
        user2.setBirthday(LocalDate.of(1992, 2, 2));
        user2 = userStorage.create(user2);

        User commonFriend = new User();
        commonFriend.setEmail("common@example.com");
        commonFriend.setLogin("common");
        commonFriend.setName("Common Friend");
        commonFriend.setBirthday(LocalDate.of(1993, 3, 3));
        commonFriend = userStorage.create(commonFriend);

        // user1 добавляет commonFriend в друзья
        userStorage.addFriend(user1.getId(), commonFriend.getId());
        // user2 добавляет commonFriend в друзья
        userStorage.addFriend(user2.getId(), commonFriend.getId());

        // When
        List<User> commonFriends = userStorage.getCommonFriends(user1.getId(), user2.getId());

        // Then
        assertThat(commonFriends).hasSize(1);
        assertThat(commonFriends.get(0).getId()).isEqualTo(commonFriend.getId());
    }
}