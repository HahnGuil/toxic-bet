CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     name VARCHAR(255) NOT NULL,
                                     email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS teams (
                                     id BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS match (
                                     id BIGSERIAL PRIMARY KEY,
                                     home_team_id BIGINT NOT NULL,
                                     visiting_team_id BIGINT NOT NULL,
                                     result VARCHAR(50),
                                     match_time TIMESTAMP NOT NULL,
                                     CONSTRAINT fk_home_team FOREIGN KEY (home_team_id) REFERENCES teams(id),
                                     CONSTRAINT fk_visiting_team FOREIGN KEY (visiting_team_id) REFERENCES teams(id),
                                     CONSTRAINT chk_different_teams CHECK (home_team_id != visiting_team_id)
);

CREATE TABLE IF NOT EXISTS bet (
                                   id BIGSERIAL PRIMARY KEY,
                                   user_id UUID NOT NULL,
                                   match_id BIGINT NOT NULL,
                                   result VARCHAR(50) NOT NULL,
                                   CONSTRAINT fk_bet_user FOREIGN KEY (user_id) REFERENCES users(id),
                                   CONSTRAINT fk_bet_match FOREIGN KEY (match_id) REFERENCES match(id),
                                   CONSTRAINT uq_user_match_bet UNIQUE (user_id, match_id)
);

CREATE TABLE IF NOT EXISTS betting_pool (
                                            id BIGSERIAL PRIMARY KEY,
                                            betting_pool_name VARCHAR(255) NOT NULL,
                                            betting_pool_key VARCHAR(255) NOT NULL UNIQUE,
                                            betting_pool_owner_id UUID NOT NULL,
                                            users UUID[],
                                            CONSTRAINT fk_betting_pool_owner FOREIGN KEY (betting_pool_owner_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS standings (
                                         id BIGSERIAL PRIMARY KEY,
                                         user_id UUID NOT NULL UNIQUE,
                                         points INTEGER DEFAULT 0,
                                         CONSTRAINT fk_standings_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_bet_user_id ON bet(user_id);
CREATE INDEX idx_bet_match_id ON bet(match_id);
CREATE INDEX idx_match_time ON match(match_time);
CREATE INDEX idx_standings_user_id ON standings(user_id);
