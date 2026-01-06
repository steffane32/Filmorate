package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserControllerTest {

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController();
    }

    @Test
    void createUser_ValidData_Success() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User createdUser = userController.create(user);

        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals("test@example.com", createdUser.getEmail());
        assertEquals("testlogin", createdUser.getLogin());
        assertEquals("Test User", createdUser.getName());
    }

    @Test
    void createUser_EmptyName_UsesLogin() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testlogin");
        user.setName("");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User createdUser = userController.create(user);

        assertNotNull(createdUser);
        assertEquals("testlogin", createdUser.getName());
    }

    @Test
    void createUser_NullName_UsesLogin() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testlogin");
        user.setName(null);
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User createdUser = userController.create(user);

        assertNotNull(createdUser);
        assertEquals("testlogin", createdUser.getName());
    }

    @Test
    void createUser_EmptyEmail_ThrowsValidationException() {
        User user = new User();
        user.setEmail("");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void createUser_NullEmail_ThrowsValidationException() {
        User user = new User();
        user.setEmail(null);
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void createUser_EmailWithoutAtSymbol_ThrowsValidationException() {
        User user = new User();
        user.setEmail("invalid-email");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void createUser_EmptyLogin_ThrowsValidationException() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void createUser_NullLogin_ThrowsValidationException() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin(null);
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void createUser_LoginWithSpaces_ThrowsValidationException() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("login with spaces");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void createUser_FutureBirthday_ThrowsValidationException() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.now().plusDays(1)); // завтра

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void createUser_TodayBirthday_Success() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.now()); // сегодня

        User createdUser = userController.create(user);

        assertNotNull(createdUser);
        assertEquals(LocalDate.now(), createdUser.getBirthday());
    }

    @Test
    void createUser_PastBirthday_Success() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User createdUser = userController.create(user);

        assertNotNull(createdUser);
        assertEquals(LocalDate.of(1990, 1, 1), createdUser.getBirthday());
    }

    @Test
    void createUser_NullBirthday_Success() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(null); // допустимо по ТЗ

        User createdUser = userController.create(user);

        assertNotNull(createdUser);
        assertNull(createdUser.getBirthday());
    }

    @Test
    void updateUser_WithoutId_ThrowsValidationException() {
        User user = new User();
        user.setEmail("updated@example.com");
        user.setLogin("updatedlogin");
        user.setName("Updated User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        // id не установлен

        assertThrows(ValidationException.class, () -> userController.update(user));
    }

    @Test
    void updateUser_NonExistentId_ThrowsValidationException() {
        User user = new User();
        user.setId(999L);
        user.setEmail("updated@example.com");
        user.setLogin("updatedlogin");
        user.setName("Updated User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertThrows(ValidationException.class, () -> userController.update(user));
    }

    @Test
    void updateUser_ValidData_Success() {
        // Сначала создаем пользователя
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        User createdUser = userController.create(user);
        Long userId = createdUser.getId();

        // Обновляем пользователя
        User updatedUserData = new User();
        updatedUserData.setId(userId);
        updatedUserData.setEmail("updated@example.com");
        updatedUserData.setLogin("updatedlogin");
        updatedUserData.setName("Updated User");
        updatedUserData.setBirthday(LocalDate.of(1995, 1, 1));

        User updatedUser = userController.update(updatedUserData);

        assertNotNull(updatedUser);
        assertEquals(userId, updatedUser.getId());
        assertEquals("updated@example.com", updatedUser.getEmail());
        assertEquals("updatedlogin", updatedUser.getLogin());
        assertEquals("Updated User", updatedUser.getName());
        assertEquals(LocalDate.of(1995, 1, 1), updatedUser.getBirthday());
    }
}