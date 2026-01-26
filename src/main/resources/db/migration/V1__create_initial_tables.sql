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
                                           name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS championship_teams(
                                                 id BIGSERIAL PRIMARY KEY,
                                                 championship_id BIGINT NOT NULL,
                                                 team_id BIGINT NOT NULL,
                                                 CONSTRAINT fk_championship_teams_championship FOREIGN KEY (championship_id) REFERENCES championship(id),
                                                 CONSTRAINT fk_championship_teams_team FOREIGN KEY (team_id) REFERENCES teams(id),
                                                 CONSTRAINT uq_championship_team UNIQUE (championship_id, team_id)
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
                                     total_bet_match INTEGER DEFAULT 0,
                                     total_bet_home_team INTEGER DEFAULT 0,
                                     total_bet_draw INTEGER DEFAULT 0,
                                     total_bet_visiting_team INTEGER DEFAULT 0,
                                     CONSTRAINT fk_match_championship FOREIGN KEY (championship_id) REFERENCES championship(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_home_team FOREIGN KEY (home_team_id) REFERENCES teams(id) ON DELETE RESTRICT,
                                     CONSTRAINT fk_visiting_team FOREIGN KEY (visiting_team_id) REFERENCES teams(id) ON DELETE RESTRICT,
                                     CONSTRAINT chk_different_teams CHECK (home_team_id != visiting_team_id),
                                     CONSTRAINT chk_match_result CHECK (result IN ('HOME_WIN', 'VISITING_WIN', 'DRAW', 'NOT_STARTED', 'IN_PROGRESS', 'OPEN_FOR_BETTING'))

);

CREATE TABLE IF NOT EXISTS bet (
                                   id BIGSERIAL PRIMARY KEY,
                                   user_id UUID NOT NULL,
                                   match_id BIGINT NOT NULL,
                                   result VARCHAR(50) NOT NULL,
                                   bet_time TIMESTAMP DEFAULT NOW(),
                                   CONSTRAINT fk_bet_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                   CONSTRAINT fk_bet_match FOREIGN KEY (match_id) REFERENCES match(id) ON DELETE CASCADE,
                                   CONSTRAINT uq_user_match_bet UNIQUE (user_id, match_id),
                                   CONSTRAINT chk_bet_result CHECK (result IN ('HOME_WIN', 'VISITING_WIN', 'DRAW'))
);

CREATE TABLE IF NOT EXISTS betting_pool (
                                            id BIGSERIAL PRIMARY KEY,
                                            betting_pool_name VARCHAR(255) NOT NULL,
                                            betting_pool_key VARCHAR(255) NOT NULL UNIQUE,
                                            betting_pool_owner_id UUID NOT NULL,
                                            CONSTRAINT fk_betting_pool_owner FOREIGN KEY (betting_pool_owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS betting_pool_users (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  betting_pool_id BIGINT NOT NULL,
                                                  user_id UUID NOT NULL,
                                                  joined_at TIMESTAMP DEFAULT NOW(),
                                                  CONSTRAINT fk_pool FOREIGN KEY (betting_pool_id) REFERENCES betting_pool(id) ON DELETE CASCADE,
                                                  CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                                  CONSTRAINT uq_pool_user UNIQUE (betting_pool_id, user_id)
);

CREATE TABLE IF NOT EXISTS betting_pool_standings (
                                                      id BIGSERIAL PRIMARY KEY,
                                                      betting_pool_id BIGINT NOT NULL,
                                                      user_id UUID NOT NULL,
                                                      points INTEGER DEFAULT 0,
                                                      CONSTRAINT fk_standings_pool FOREIGN KEY (betting_pool_id) REFERENCES betting_pool(id) ON DELETE CASCADE,
                                                      CONSTRAINT fk_standings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                                      CONSTRAINT uq_pool_user_standings UNIQUE (betting_pool_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_bet_user_id ON bet(user_id);
CREATE INDEX IF NOT EXISTS idx_bet_match_id ON bet(match_id);
CREATE INDEX IF NOT EXISTS idx_pool_users_user_id ON betting_pool_users(user_id);
CREATE INDEX IF NOT EXISTS idx_pool_users_pool_id ON betting_pool_users(betting_pool_id);
CREATE INDEX IF NOT EXISTS idx_match_championship_id ON match(championship_id);
CREATE INDEX IF NOT EXISTS idx_match_time ON match(match_time);
CREATE INDEX IF NOT EXISTS idx_pool_standings_pool_id ON betting_pool_standings(betting_pool_id);
CREATE INDEX IF NOT EXISTS idx_pool_standings_user_id ON betting_pool_standings(user_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_teams_name ON teams(name);
CREATE INDEX IF NOT EXISTS idx_championship_name ON championship(name);
CREATE INDEX IF NOT EXISTS idx_championship_teams_team_id ON championship_teams(team_id);
CREATE INDEX IF NOT EXISTS idx_championship_teams_championship_id ON championship_teams(championship_id);
CREATE INDEX IF NOT EXISTS idx_betting_pool_owner_id ON betting_pool(betting_pool_owner_id);
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
