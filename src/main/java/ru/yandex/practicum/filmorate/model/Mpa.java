package ru.yandex.practicum.filmorate.model;

import lombok.Data;

@Data
public class Mpa {
    private Long id;
    private String name; // "G", "PG", "PG-13", "R", "NC-17"
    private String description;
}