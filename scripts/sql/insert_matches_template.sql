-- Template to insert matches in the toxic-bet database.
--
-- Run from the EC2 repository directory:
-- docker compose -f compose.aws.yaml exec -T postgres sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' < scripts/sql/insert_matches_template.sql
--
-- The application stores match_time as a timestamp without timezone.
-- Use Brasilia time (America/Sao_Paulo), format DD/MM/YYYY HH24:MI.

BEGIN;

CREATE TEMP TABLE match_seed (
    championship_name text NOT NULL,
    home_team_name text NOT NULL,
    visiting_team_name text NOT NULL,
    match_time text NOT NULL
) ON COMMIT DROP;

-- Edit only this VALUES block when adding matches.
INSERT INTO match_seed (
    championship_name,
    home_team_name,
    visiting_team_name,
    match_time
) VALUES
    -- Examples:
    -- ('Brasileirao 2026', 'Corinthians', 'Palmeiras', '20/05/2026 16:00'),
    -- ('Brasileirao 2026', 'Sao Paulo', 'Santos', '20/05/2026 18:30')
    ('Brasileirao 2026', 'Corinthians', 'Palmeiras', '20/05/2026 16:00');

DO $$
DECLARE
    invalid_count integer;
BEGIN
    SELECT count(*)
      INTO invalid_count
      FROM match_seed seed
 LEFT JOIN championship championship_ref
        ON championship_ref.name = seed.championship_name
 LEFT JOIN teams home_team
        ON home_team.name = seed.home_team_name
 LEFT JOIN teams visiting_team
        ON visiting_team.name = seed.visiting_team_name
     WHERE championship_ref.id IS NULL
        OR home_team.id IS NULL
        OR visiting_team.id IS NULL
        OR seed.home_team_name = seed.visiting_team_name
        OR to_timestamp(seed.match_time, 'DD/MM/YYYY HH24:MI')::timestamp <= (now() AT TIME ZONE 'America/Sao_Paulo');

    IF invalid_count > 0 THEN
        RAISE EXCEPTION
            'Found % invalid match seed rows. Check championship/team names, different teams, and future match_time.',
            invalid_count;
    END IF;
END $$;

INSERT INTO "match" (
    championship_id,
    home_team_id,
    visiting_team_id,
    home_team_score,
    visiting_team_score,
    odds_home_team,
    odds_visiting_team,
    odds_draw,
    result,
    match_time,
    version,
    total_bet_match,
    total_bet_home_team,
    total_bet_draw,
    total_bet_visiting_team
)
SELECT
    championship_ref.id,
    home_team.id,
    visiting_team.id,
    NULL,
    NULL,
    10,
    10,
    10,
    'NOT_STARTED',
    to_timestamp(seed.match_time, 'DD/MM/YYYY HH24:MI')::timestamp,
    0,
    0,
    0,
    0,
    0
  FROM match_seed seed
  JOIN championship championship_ref
    ON championship_ref.name = seed.championship_name
  JOIN teams home_team
    ON home_team.name = seed.home_team_name
  JOIN teams visiting_team
    ON visiting_team.name = seed.visiting_team_name
RETURNING id, championship_id, home_team_id, visiting_team_id, result, match_time;

COMMIT;

SELECT
    match_ref.id,
    championship_ref.name AS championship,
    home_team.name AS home_team,
    visiting_team.name AS visiting_team,
    match_ref.result,
    to_char(match_ref.match_time, 'DD/MM/YYYY HH24:MI') AS match_time
  FROM "match" match_ref
  JOIN championship championship_ref
    ON championship_ref.id = match_ref.championship_id
  JOIN teams home_team
    ON home_team.id = match_ref.home_team_id
  JOIN teams visiting_team
    ON visiting_team.id = match_ref.visiting_team_id
 ORDER BY match_ref.match_time, match_ref.id;
