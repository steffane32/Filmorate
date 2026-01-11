package ru.yandex.practicum.filmorate.storage.film;
import ru.yandex.practicum.filmorate.model.Mpa;
import java.util.List;
import java.util.Optional;

public interface MpaStorage {
    List<Mpa> findAll();
    Optional<Mpa> findById(Long id);
    Optional<Mpa> findByName(String name);
    Mpa create(Mpa mpa);
    Mpa update(Mpa mpa);
    void delete(Long id);
    boolean existsById(Long id);
}