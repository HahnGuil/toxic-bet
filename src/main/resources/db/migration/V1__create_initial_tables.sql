-- SQL migration: initial tables and oddie table
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users table: stores application users with UUID primary key
CREATE TABLE IF NOT EXISTS users (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     name VARCHAR(255) NOT NULL,
                                     email VARCHAR(255) NOT NULL UNIQUE
);

-- Teams table: stores teams with BIG SERIAL id
CREATE TABLE IF NOT EXISTS teams (
                                     id BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL UNIQUE
);

-- Match table: stores scheduled matches between two teams and the result
CREATE TABLE IF NOT EXISTS match (
                                     id BIGSERIAL PRIMARY KEY,
                                     home_team_id BIGINT NOT NULL,
                                     visiting_team_id BIGINT NOT NULL,
                                     home_team_score INTEGER,
                                     visiting_team_score INTEGER,
                                     result VARCHAR(50),
                                     match_time TIMESTAMP NOT NULL,
                                     CONSTRAINT fk_home_team FOREIGN KEY (home_team_id) REFERENCES teams(id),
                                     CONSTRAINT fk_visiting_team FOREIGN KEY (visiting_team_id) REFERENCES teams(id),
                                     CONSTRAINT chk_different_teams CHECK (home_team_id != visiting_team_id)
);

-- Bet table: stores bets placed by users on matches; one bet per user per match
CREATE TABLE IF NOT EXISTS bet (
                                   id BIGSERIAL PRIMARY KEY,
                                   user_id UUID NOT NULL,
                                   match_id BIGINT NOT NULL,
                                   result VARCHAR(50) NOT NULL,
                                    bet_odds DOUBLE PRECISION,
                                   CONSTRAINT fk_bet_user FOREIGN KEY (user_id) REFERENCES users(id),
                                   CONSTRAINT fk_bet_match FOREIGN KEY (match_id) REFERENCES match(id),
                                   CONSTRAINT uq_user_match_bet UNIQUE (user_id, match_id)
);

-- Betting pool table: groups of users with an owner reference
CREATE TABLE IF NOT EXISTS betting_pool (
                                            id BIGSERIAL PRIMARY KEY,
                                            betting_pool_name VARCHAR(255) NOT NULL,
                                            betting_pool_key VARCHAR(255) NOT NULL UNIQUE,
                                            betting_pool_owner_id UUID NOT NULL,
                                            users UUID[],
                                            CONSTRAINT fk_betting_pool_owner FOREIGN KEY (betting_pool_owner_id) REFERENCES users(id)
);

-- Standings table: stores points per user (unique per user)
CREATE TABLE IF NOT EXISTS standings (
                                         id BIGSERIAL PRIMARY KEY,
                                         user_id UUID NOT NULL UNIQUE,
                                         points INTEGER DEFAULT 0,
                                         CONSTRAINT fk_standings_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Odds table: stores odds for a match, one record per match (match_id must be unique)
-- id: BIG SERIAL primary key
-- match_id: references match(id) and must be unique (no duplicate match entries)
-- total_bets_for_match: integer count of bets aggregated for the match
-- odd_home_team: double precision odd for home team
-- odd_visiting_team: double precision odd for visiting team
-- odd_draw: double precision odd for draw outcome
CREATE TABLE IF NOT EXISTS odds (
                                     id BIGSERIAL PRIMARY KEY,
                                     match_id BIGINT NOT NULL,
                                     total_bets_for_match INTEGER,
                                     odd_home_team DOUBLE PRECISION,
                                     odd_visiting_team DOUBLE PRECISION,
                                     odd_draw DOUBLE PRECISION,
                                     CONSTRAINT fk_odds_match FOREIGN KEY (match_id) REFERENCES match(id),
                                     CONSTRAINT uq_odds_match UNIQUE (match_id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_bet_user_id ON bet(user_id);
CREATE INDEX IF NOT EXISTS idx_bet_match_id ON bet(match_id);
CREATE INDEX IF NOT EXISTS idx_match_time ON match(match_time);
CREATE INDEX IF NOT EXISTS idx_standings_user_id ON standings(user_id);

-- Index specifically for odds.match_id for efficient lookups
CREATE INDEX IF NOT EXISTS idx_odds_match_id ON odds(match_id);
