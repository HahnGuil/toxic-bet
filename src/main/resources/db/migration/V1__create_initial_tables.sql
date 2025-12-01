-- V1__create_initial_tables.sql

CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS teams (
                                     id BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL
    );

CREATE TABLE IF NOT EXISTS match (
                                     id BIGSERIAL PRIMARY KEY,
                                     home_team_id BIGINT NOT NULL,
                                     visiting_team_id BIGINT NOT NULL,
                                     result VARCHAR(50),
    match_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_home_team FOREIGN KEY (home_team_id) REFERENCES teams(id),
    CONSTRAINT fk_visiting_team FOREIGN KEY (visiting_team_id) REFERENCES teams(id)
    );

CREATE TABLE IF NOT EXISTS bet (
                                   id BIGSERIAL PRIMARY KEY,
                                   user_id BIGINT NOT NULL,
                                   match_id BIGINT NOT NULL,
                                   result VARCHAR(50) NOT NULL,
    CONSTRAINT fk_bet_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_bet_match FOREIGN KEY (match_id) REFERENCES match(id)
    );

CREATE TABLE IF NOT EXISTS betting_pool (
                                            id BIGSERIAL PRIMARY KEY,
                                            betting_pool_name VARCHAR(255) NOT NULL,
    betting_pool_key VARCHAR(255) NOT NULL UNIQUE,
    betting_pool_owner_id BIGINT NOT NULL,
    users BIGINT[],
    CONSTRAINT fk_betting_pool_owner FOREIGN KEY (betting_pool_owner_id) REFERENCES users(id)
    );

CREATE TABLE IF NOT EXISTS standings (
                                         id BIGSERIAL PRIMARY KEY,
                                         user_id BIGINT NOT NULL,
                                         points INTEGER DEFAULT 0,
                                         CONSTRAINT fk_standings_user FOREIGN KEY (user_id) REFERENCES users(id)
    );

CREATE INDEX idx_bet_user_id ON bet(user_id);
CREATE INDEX idx_bet_match_id ON bet(match_id);
CREATE INDEX idx_match_time ON match(match_time);
CREATE INDEX idx_standings_user_id ON standings(user_id);
