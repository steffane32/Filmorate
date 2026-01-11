package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import(GenreDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class GenreDbStorageTest {

    private final GenreDbStorage genreStorage;

    @Test
    void findAll_ShouldReturnAllGenres() {
        // When
        List<Genre> genres = genreStorage.findAll();

        // Then
        assertThat(genres).hasSize(6); // Должно быть 6 жанров из data.sql
        assertThat(genres).extracting("name")
                .contains("Комедия", "Драма", "Мультфильм", "Триллер", "Документальный", "Боевик");
    }

    @Test
    void findById_WhenExists_ShouldReturnGenre() {
        // When
        Optional<Genre> genreOptional = genreStorage.findById(1L);

        // Then
        assertThat(genreOptional)
                .isPresent()
                .hasValueSatisfying(genre -> {
                    assertThat(genre.getId()).isEqualTo(1L);
                    assertThat(genre.getName()).isEqualTo("Комедия");
                });
    }

    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        // When
        Optional<Genre> genreOptional = genreStorage.findById(999L);

        // Then
        assertThat(genreOptional).isEmpty();
    }

    @Test
    void findByName_WhenExists_ShouldReturnGenre() {
        // When
        Optional<Genre> genreOptional = genreStorage.findByName("Драма");

        // Then
        assertThat(genreOptional)
                .isPresent()
                .hasValueSatisfying(genre -> {
                    assertThat(genre.getId()).isEqualTo(2L);
                    assertThat(genre.getName()).isEqualTo("Драма");
                });
    }
}