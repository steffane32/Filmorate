package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import(MpaDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class MpaDbStorageTest {

    private final MpaDbStorage mpaStorage;

    @Test
    void findAll_ShouldReturnAllMpaRatings() {
        // When
        List<Mpa> mpaList = mpaStorage.findAll();

        // Then
        assertThat(mpaList).hasSize(5); // Должно быть 5 рейтингов из data.sql
        assertThat(mpaList).extracting("name")
                .contains("G", "PG", "PG-13", "R", "NC-17");
    }

    @Test
    void findById_WhenExists_ShouldReturnMpa() {
        // When
        Optional<Mpa> mpaOptional = mpaStorage.findById(1L);

        // Then
        assertThat(mpaOptional)
                .isPresent()
                .hasValueSatisfying(mpa -> {
                    assertThat(mpa.getId()).isEqualTo(1L);
                    assertThat(mpa.getName()).isEqualTo("G");
                    assertThat(mpa.getDescription()).contains("Нет возрастных ограничений");
                });
    }

    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        // When
        Optional<Mpa> mpaOptional = mpaStorage.findById(999L);

        // Then
        assertThat(mpaOptional).isEmpty();
    }

    @Test
    void findByName_WhenExists_ShouldReturnMpa() {
        // When
        Optional<Mpa> mpaOptional = mpaStorage.findByName("PG-13");

        // Then
        assertThat(mpaOptional)
                .isPresent()
                .hasValueSatisfying(mpa -> {
                    assertThat(mpa.getId()).isEqualTo(3L);
                    assertThat(mpa.getName()).isEqualTo("PG-13");
                });
    }
}