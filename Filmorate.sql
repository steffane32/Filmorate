-- Таблица пользователей
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    login VARCHAR(255) NOT NULL,
    user_name VARCHAR(255),
    birthday DATE NOT NULL
);

-- Таблица рейтингов MPA
CREATE TABLE mpa_ratings (
    mpa_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(10) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Таблица фильмов
CREATE TABLE films (
    film_id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(200),
    release_date DATE NOT NULL,
    duration INTEGER NOT NULL CHECK (duration > 0),
    mpa_id BIGINT,
    FOREIGN KEY (mpa_id) REFERENCES mpa_ratings(mpa_id) ON DELETE SET NULL
);

-- Таблица жанров
CREATE TABLE genres (
    genre_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Связь многие-ко-многим: Фильмы - Жанры
CREATE TABLE film_genres (
    film_id BIGINT,
    genre_id BIGINT,
    PRIMARY KEY (film_id, genre_id),
    FOREIGN KEY (film_id) REFERENCES films(film_id) ON DELETE CASCADE,
    FOREIGN KEY (genre_id) REFERENCES genres(genre_id) ON DELETE CASCADE
);

-- Таблица лайков
CREATE TABLE likes (
    film_id BIGINT,
    user_id BIGINT,
    PRIMARY KEY (film_id, user_id),
    FOREIGN KEY (film_id) REFERENCES films(film_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Таблица дружбы с подтверждением
CREATE TABLE friendships (
    user_id BIGINT,
    friend_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED' 
        CHECK (status IN ('REQUESTED', 'CONFIRMED')),
    PRIMARY KEY (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT check_self_friendship CHECK (user_id != friend_id)
);

-- Создаем индекс для быстрого поиска друзей
CREATE INDEX idx_friendships_user ON friendships(user_id);
CREATE INDEX idx_friendships_friend ON friendships(friend_id);
CREATE INDEX idx_friendships_status ON friendships(status);