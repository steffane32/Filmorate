package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserControllerTest {

    private UserController userController;

    @BeforeEach
    void setUp() {
        InMemoryUserStorage userStorage = new InMemoryUserStorage();
        UserService userService = new UserService(userStorage);
        userController = new UserController(userService);
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
    void createUser_EmptyEmail_ThrowsException() {
        User user = new User();
        user.setEmail("");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        // Spring валидация через аннотации выбрасывает MethodArgumentNotValidException
        assertThrows(Exception.class, () -> userController.create(user));
    }

    @Test
    void createUser_EmailWithoutAtSymbol_ThrowsException() {
        User user = new User();
        user.setEmail("invalid-email");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertThrows(Exception.class, () -> userController.create(user));
    }

    @Test
    void createUser_LoginWithSpaces_ThrowsValidationException() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("login with spaces");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        // Эта проверка в UserService.validateUser() выбрасывает ValidationException
        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    void createUser_FutureBirthday_ThrowsException() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.now().plusDays(1));

        assertThrows(Exception.class, () -> userController.create(user));
    }
}