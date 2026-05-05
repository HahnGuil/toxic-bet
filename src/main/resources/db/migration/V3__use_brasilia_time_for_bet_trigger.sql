CREATE OR REPLACE FUNCTION check_bet_before_match()
    RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT match_time FROM match WHERE id = NEW.match_id) <= (NOW() AT TIME ZONE 'America/Sao_Paulo') THEN
        RAISE EXCEPTION 'Cannot bet on a match that has already started';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
