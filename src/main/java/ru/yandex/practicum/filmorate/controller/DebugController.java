package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/debug")
public class DebugController {

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;

    @Autowired
    public DebugController(JdbcTemplate jdbcTemplate, UserService userService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
    }

    @GetMapping("/check-db")
    public String checkDb() {
        try {
            // Проверяем подключение к БД
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "✅ Database connection OK! Result: " + result;
        } catch (Exception e) {
            return "❌ Database connection FAILED: " + e.getMessage();
        }
    }

    @GetMapping("/tables")
    public List<String> listTables() {
        try {
            return jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'",
                    String.class
            );
        } catch (Exception e) {
            return List.of("Error: " + e.getMessage());
        }
    }

    @GetMapping("/storage-type")
    public String getStorageType() {
        return "Current UserStorage type: " + userService.getClass().getSimpleName();
    }

    @GetMapping("/test-insert")
    public String testInsert() {
        try {
            String sql = "INSERT INTO users (email, login, user_name, birthday) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sql, "test@test.com", "testuser", "Test User", java.sql.Date.valueOf("1990-01-01"));
            return "✅ Test insert successful!";
        } catch (Exception e) {
            return "❌ Test insert failed: " + e.getMessage();
        }
    }

    @GetMapping("/users-count")
    public String getUsersCount() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            return "Users in database: " + count;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}