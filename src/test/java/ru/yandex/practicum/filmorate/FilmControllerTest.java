package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FilmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createFilm_ValidData_Success() throws Exception {
        String filmJson = "{\"name\":\"Test Film\",\"description\":\"Test Description\"," +
                "\"releaseDate\":\"2000-01-01\",\"duration\":120}";

        mockMvc.perform(post("/films")
                        .contentType("application/json")
                        .content(filmJson))
                .andExpect(status().isCreated());
    }

    @Test
    void createFilm_EmptyName_BadRequest() throws Exception {
        String filmJson = "{\"name\":\"\",\"description\":\"Test Description\"," +
                "\"releaseDate\":\"2000-01-01\",\"duration\":120}";

        mockMvc.perform(post("/films")
                        .contentType("application/json")
                        .content(filmJson))
                .andExpect(status().isBadRequest());
    }
}