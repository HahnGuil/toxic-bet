-- Add individual bet counters to odds table
ALTER TABLE odds
    ADD COLUMN total_bets_home_win INTEGER DEFAULT 0,
    ADD COLUMN total_bets_visiting_win INTEGER DEFAULT 0,
    ADD COLUMN total_bets_draw INTEGER DEFAULT 0;
