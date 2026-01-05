package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmControllerTest {

    private FilmController filmController;

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
    }

    @Test
    void createFilm_ValidData_Success() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Film createdFilm = filmController.create(film);

        assertNotNull(createdFilm);
        assertNotNull(createdFilm.getId());
        assertEquals("Test Film", createdFilm.getName());
        assertEquals(120, createdFilm.getDuration());
    }

    @Test
    void createFilm_EmptyName_ThrowsValidationException() {
        Film film = new Film();
        film.setName("");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void createFilm_NullName_ThrowsValidationException() {
        Film film = new Film();
        film.setName(null);
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void createFilm_DescriptionTooLong_ThrowsValidationException() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("A".repeat(201)); // 201 символов
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void createFilm_DescriptionExactly200Chars_Success() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("A".repeat(200)); // 200 символов - допустимо
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Film createdFilm = filmController.create(film);

        assertNotNull(createdFilm);
        assertEquals(200, createdFilm.getDescription().length());
    }

    @Test
    void createFilm_ReleaseDateBeforeMinDate_ThrowsValidationException() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(1895, 12, 27)); // на день раньше
        film.setDuration(120);

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void createFilm_ReleaseDateMinDate_Success() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(1895, 12, 28)); // минимальная дата
        film.setDuration(120);

        Film createdFilm = filmController.create(film);

        assertNotNull(createdFilm);
        assertEquals(LocalDate.of(1895, 12, 28), createdFilm.getReleaseDate());
    }

    @Test
    void createFilm_NegativeDuration_ThrowsValidationException() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(-10);

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void createFilm_ZeroDuration_ThrowsValidationException() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(0);

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void createFilm_NullDuration_ThrowsValidationException() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(null);

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void createFilm_ReleaseDateNull_ThrowsValidationException() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(null);
        film.setDuration(120);

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void updateFilm_WithoutId_ThrowsValidationException() {
        Film film = new Film();
        film.setName("Updated Film");
        film.setDescription("Updated Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        // id не установлен

        assertThrows(ValidationException.class, () -> filmController.update(film));
    }

    @Test
    void updateFilm_NonExistentId_ThrowsValidationException() {
        Film film = new Film();
        film.setId(999L);
        film.setName("Updated Film");
        film.setDescription("Updated Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        assertThrows(ValidationException.class, () -> filmController.update(film));
    }
}