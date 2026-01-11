package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.film.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDbStorage.class, MpaDbStorage.class, GenreDbStorage.class, UserDbStorage.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;
    private final MpaDbStorage mpaStorage;
    private final GenreDbStorage genreStorage;
    private final UserDbStorage userStorage;

    private Film testFilm;
    private Mpa testMpa;

    @BeforeEach
    void setUp() {
        testMpa = Mpa.builder()
                .id(1L)
                .name("G")
                .description("Нет возрастных ограничений")
                .build();

        Set<Genre> genres = new HashSet<>();
        genres.add(Genre.builder().id(1L).name("Комедия").build());
        genres.add(Genre.builder().id(2L).name("Драма").build());

        testFilm = Film.builder()
                .name("Test Film")
                .description("Test Description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(testMpa)
                .genres(genres)
                .build();
    }

    @Test
    void createFilm_ShouldReturnFilmWithId() {
        // When
        Film createdFilm = filmStorage.create(testFilm);

        // Then
        assertThat(createdFilm.getId()).isNotNull();
        assertThat(createdFilm.getName()).isEqualTo("Test Film");
        assertThat(createdFilm.getDescription()).isEqualTo("Test Description");
        assertThat(createdFilm.getDuration()).isEqualTo(120);
        assertThat(createdFilm.getMpa()).isNotNull();
        assertThat(createdFilm.getMpa().getId()).isEqualTo(1L);
        assertThat(createdFilm.getGenres()).hasSize(2);
    }

    @Test
    void findById_WhenFilmExists_ShouldReturnFilm() {
        // Given
        Film createdFilm = filmStorage.create(testFilm);
        Long filmId = createdFilm.getId();

        // When
        Film foundFilm = filmStorage.findById(filmId);

        // Then
        assertThat(foundFilm.getId()).isEqualTo(filmId);
        assertThat(foundFilm.getName()).isEqualTo("Test Film");
        assertThat(foundFilm.getGenres()).hasSize(2);
    }

    @Test
    void findAll_ShouldReturnAllFilms() {
        // Given
        Film film1 = filmStorage.create(testFilm);

        Film film2 = Film.builder()
                .name("Another Film")
                .description("Another Description")
                .releaseDate(LocalDate.of(2021, 1, 1))
                .duration(90)
                .mpa(testMpa)
                .build();
        filmStorage.create(film2);

        // When
        List<Film> films = filmStorage.findAll();

        // Then
        assertThat(films).hasSize(2);
    }

    @Test
    void update_ShouldUpdateFilm() {
        // Given
        Film createdFilm = filmStorage.create(testFilm);

        createdFilm.setName("Updated Film Name");
        createdFilm.setDescription("Updated Description");

        // When
        Film updatedFilm = filmStorage.update(createdFilm);

        // Then
        assertThat(updatedFilm.getName()).isEqualTo("Updated Film Name");
        assertThat(updatedFilm.getDescription()).isEqualTo("Updated Description");

        // Verify in database
        Film foundFilm = filmStorage.findById(createdFilm.getId());
        assertThat(foundFilm.getName()).isEqualTo("Updated Film Name");
    }

    @Test
    void addLike_ShouldAddLike() {
        // Given
        Film film = filmStorage.create(testFilm);

        User user = User.builder()
                .email("user@example.com")
                .login("user")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        userStorage.create(user);

        // When
        filmStorage.addLike(film.getId(), user.getId());

        // Then
        Film foundFilm = filmStorage.findById(film.getId());
        assertThat(foundFilm.getLikes()).contains(user.getId());
    }

    @Test
    void removeLike_ShouldRemoveLike() {
        // Given
        Film film = filmStorage.create(testFilm);

        User user = User.builder()
                .email("user@example.com")
                .login("user")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        userStorage.create(user);

        filmStorage.addLike(film.getId(), user.getId());

        // When
        filmStorage.removeLike(film.getId(), user.getId());

        // Then
        Film foundFilm = filmStorage.findById(film.getId());
        assertThat(foundFilm.getLikes()).doesNotContain(user.getId());
    }

    @Test
    void getPopularFilms_ShouldReturnFilmsOrderedByLikes() {
        // Given
        Film film1 = filmStorage.create(testFilm);

        Film film2 = Film.builder()
                .name("Popular Film")
                .description("Popular Description")
                .releaseDate(LocalDate.of(2021, 1, 1))
                .duration(90)
                .mpa(testMpa)
                .build();
        filmStorage.create(film2);

        User user1 = User.builder()
                .email("user1@example.com")
                .login("user1")
                .name("User One")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        userStorage.create(user1);

        User user2 = User.builder()
                .email("user2@example.com")
                .login("user2")
                .name("User Two")
                .birthday(LocalDate.of(1992, 2, 2))
                .build();
        userStorage.create(user2);

        // film2 получает больше лайков
        filmStorage.addLike(film2.getId(), user1.getId());
        filmStorage.addLike(film2.getId(), user2.getId());
        filmStorage.addLike(film1.getId(), user1.getId());

        // When
        List<Film> popularFilms = filmStorage.getPopularFilms(2);

        // Then
        assertThat(popularFilms).hasSize(2);
        assertThat(popularFilms.get(0).getId()).isEqualTo(film2.getId()); // Больше лайков
        assertThat(popularFilms.get(1).getId()).isEqualTo(film1.getId());
    }
}
