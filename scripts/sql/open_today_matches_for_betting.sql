-- Opens today's not-started matches for betting.
-- The application stores match_time as a timestamp without timezone in America/Sao_Paulo.
--
-- Example:
-- docker compose -f compose.docker.yaml exec -T postgres sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' < scripts/sql/open_today_matches_for_betting.sql

WITH brasilia_now AS (
    SELECT (now() AT TIME ZONE 'America/Sao_Paulo') AS value
),
updated_matches AS (
    UPDATE "match" match_ref
       SET result = 'OPEN_FOR_BETTING'
      FROM brasilia_now
     WHERE match_ref.result = 'NOT_STARTED'
       AND match_ref.match_time::date = brasilia_now.value::date
       AND match_ref.match_time > brasilia_now.value
 RETURNING match_ref.id, match_ref.match_time, match_ref.result
)
SELECT id,
       to_char(match_time, 'DD/MM/YYYY HH24:MI') AS match_time,
       result
  FROM updated_matches
 ORDER BY match_time, id;
