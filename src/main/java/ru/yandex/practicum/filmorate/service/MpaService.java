package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.film.MpaStorage;

import java.util.List;

@Slf4j
@Service
public class MpaService {

    private final MpaStorage mpaStorage;

    @Autowired
    public MpaService(MpaStorage mpaStorage) {
        this.mpaStorage = mpaStorage;
        log.info("MpaService инициализирован");
    }

    public List<Mpa> findAll() {
        log.debug("Запрос всех рейтингов MPA");
        List<Mpa> mpaList = mpaStorage.findAll();
        log.info("Получено {} рейтингов MPA", mpaList.size());
        return mpaList;
    }

    public Mpa findById(Long id) {
        log.debug("Поиск рейтинга MPA по ID: {}", id);
        return mpaStorage.findById(id)
                .orElseThrow(() -> {
                    log.warn("Рейтинг MPA с ID {} не найден", id);
                    return new NotFoundException("Рейтинг MPA с id=" + id + " не найден");
                });
    }

    public Mpa findByName(String name) {
        log.debug("Поиск рейтинга MPA по имени: {}", name);
        return mpaStorage.findByName(name)
                .orElseThrow(() -> {
                    log.warn("Рейтинг MPA с именем '{}' не найден", name);
                    return new NotFoundException("Рейтинг MPA с именем '" + name + "' не найден");
                });
    }

    public Mpa create(Mpa mpa) {
        log.info("Создание рейтинга MPA: {}", mpa.getName());

        // Проверяем что такого рейтинга еще нет
        mpaStorage.findByName(mpa.getName()).ifPresent(existing -> {
            log.warn("Попытка создать дубликат рейтинга MPA: {}", mpa.getName());
            throw new IllegalArgumentException("Рейтинг MPA с именем '" + mpa.getName() + "' уже существует");
        });

        Mpa createdMpa = mpaStorage.create(mpa);
        log.info("✅ Рейтинг MPA создан: ID={}, name={}", createdMpa.getId(), createdMpa.getName());
        return createdMpa;
    }

    public Mpa update(Mpa mpa) {
        log.info("Обновление рейтинга MPA: ID={}, name={}", mpa.getId(), mpa.getName());

        if (mpa.getId() == null) {
            log.error("Попытка обновления рейтинга MPA без ID");
            throw new IllegalArgumentException("ID рейтинга MPA должен быть указан");
        }

        // Проверяем существование
        findById(mpa.getId());

        Mpa updatedMpa = mpaStorage.update(mpa);
        log.info("✅ Рейтинг MPA обновлён: ID={}", updatedMpa.getId());
        return updatedMpa;
    }

    public void delete(Long id) {
        log.info("Удаление рейтинга MPA: ID={}", id);
        mpaStorage.delete(id);
        log.info("✅ Рейтинг MPA удалён: ID={}", id);
    }

    public boolean existsById(Long id) {
        return mpaStorage.existsById(id);
    }

    // Методы для получения стандартных рейтингов (опционально)
    public Mpa getG() {
        return findByName("G");
    }

    public Mpa getPG() {
        return findByName("PG");
    }

    public Mpa getPG13() {
        return findByName("PG-13");
    }

    public Mpa getR() {
        return findByName("R");
    }

    public Mpa getNC17() {
        return findByName("NC-17");
    }
}