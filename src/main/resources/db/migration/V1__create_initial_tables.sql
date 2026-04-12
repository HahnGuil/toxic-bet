CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     name VARCHAR(255) NOT NULL,
                                     email VARCHAR(255) NOT NULL UNIQUE,
                                     user_points DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS teams (
                                     id BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS championship(
                                           id BIGSERIAL PRIMARY KEY,
                                           name VARCHAR(255) NOT NULL,
                                            teams_id TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[]

);

CREATE TABLE IF NOT EXISTS match (
                                     id BIGSERIAL PRIMARY KEY,
                                     championship_id BIGINT NOT NULL,
                                     home_team_id BIGINT NOT NULL,
                                     visiting_team_id BIGINT NOT NULL,
                                     home_team_score INTEGER,
                                     visiting_team_score INTEGER,
                                     odds_home_team DOUBLE PRECISION,
                                     odds_visiting_team DOUBLE PRECISION,
                                     odds_draw DOUBLE PRECISION,
                                     result VARCHAR(50),
                                     match_time TIMESTAMP NOT NULL,
                                     version INTEGER DEFAULT 0,
                                     total_bet_match INTEGER DEFAULT 0,
                                     total_bet_home_team INTEGER DEFAULT 0,
                                     total_bet_draw INTEGER DEFAULT 0,
                                     total_bet_visiting_team INTEGER DEFAULT 0,
                                     CONSTRAINT fk_match_championship FOREIGN KEY (championship_id) REFERENCES championship(id),
                                     CONSTRAINT fk_home_team FOREIGN KEY (home_team_id) REFERENCES teams(id),
                                     CONSTRAINT fk_visiting_team FOREIGN KEY (visiting_team_id) REFERENCES teams(id),
                                     CONSTRAINT chk_different_teams CHECK (home_team_id != visiting_team_id),
                                     CONSTRAINT chk_match_result CHECK (result IN ('HOME_WIN', 'VISITING_WIN', 'DRAW', 'NOT_STARTED', 'IN_PROGRESS', 'OPEN_FOR_BETTING', 'ENDED', 'CLOSE_FOR_BETTING'))
);

CREATE TABLE IF NOT EXISTS bet (
                                   id BIGSERIAL PRIMARY KEY,
                                   user_id UUID NOT NULL,
                                   match_id BIGINT NOT NULL,
                                   result VARCHAR(50) NOT NULL,
                                   bet_time TIMESTAMP DEFAULT NOW(),
                                   user_points DOUBLE PRECISION,
                                   bet_odds DOUBLE PRECISION,
                                   CONSTRAINT fk_bet_user FOREIGN KEY (user_id) REFERENCES users(id),
                                   CONSTRAINT fk_bet_match FOREIGN KEY (match_id) REFERENCES match(id),
                                   CONSTRAINT uq_user_match_bet UNIQUE (user_id, match_id),
                                   CONSTRAINT chk_bet_result CHECK (result IN ('HOME_WIN', 'VISITING_WIN', 'DRAW'))
);

CREATE TABLE betting_pool (
                              id BIGSERIAL PRIMARY KEY,
                              betting_pool_name VARCHAR(255) NOT NULL,
                              betting_pool_key VARCHAR(255) NOT NULL UNIQUE,
                              betting_pool_owner_id UUID NOT NULL,
                              user_ids TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
                              CONSTRAINT fk_betting_pool_owner FOREIGN KEY (betting_pool_owner_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_bet_user_id ON bet(user_id);
CREATE INDEX IF NOT EXISTS idx_bet_match_id ON bet(match_id);
CREATE INDEX IF NOT EXISTS idx_match_championship_id ON match(championship_id);
CREATE INDEX IF NOT EXISTS idx_match_time ON match(match_time);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_teams_name ON teams(name);
CREATE INDEX IF NOT EXISTS idx_championship_name ON championship(name);
CREATE INDEX IF NOT EXISTS idx_championship_teams_team_id ON championship_teams(team_id);
CREATE INDEX IF NOT EXISTS idx_championship_teams_championship_id ON championship_teams(championship_id);
CREATE INDEX IF NOT EXISTS idx_betting_pool_owner_id ON betting_pool(betting_pool_owner_id);
CREATE INDEX IF NOT EXISTS idx_betting_pool_user_ids ON betting_pool USING GIN(user_ids);
CREATE INDEX IF NOT EXISTS idx_bet_user_match ON bet(user_id, match_id);
CREATE INDEX IF NOT EXISTS idx_match_time_championship ON match(championship_id, match_time);

CREATE OR REPLACE FUNCTION check_bet_before_match()
    RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT match_time FROM match WHERE id = NEW.match_id) <= NOW() THEN
        RAISE EXCEPTION 'Cannot bet on a match that has already started';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_check_bet_before_match
    BEFORE INSERT ON bet
    FOR EACH ROW
EXECUTE FUNCTION check_bet_before_match();