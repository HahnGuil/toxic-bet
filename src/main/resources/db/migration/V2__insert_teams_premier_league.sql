BEGIN;

INSERT INTO teams (name)
VALUES
    -- Premier League 2025/26
    ('Arsenal'),
    ('Aston Villa'),
    ('AFC Bournemouth'),
    ('Brentford'),
    ('Brighton & Hove Albion'),
    ('Burnley'),
    ('Chelsea'),
    ('Crystal Palace'),
    ('Everton'),
    ('Fulham'),
    ('Leeds United'),
    ('Liverpool'),
    ('Manchester City'),
    ('Manchester United'),
    ('Newcastle United'),
    ('Nottingham Forest'),
    ('Sheffield United'),
    ('Tottenham Hotspur'),
    ('West Ham United'),
    ('Wolverhampton Wanderers'),

    -- UEFA Champions League 2025/26 (fase de liga)
    -- Espanha
    ('Barcelona'),
    ('Real Madrid'),
    ('Atlético de Madrid'),
    ('Athletic Club'),
    ('Villarreal'),

    -- Itália
    ('Napoli'),
    ('Inter'),
    ('Atalanta'),
    ('Juventus'),

    -- Alemanha
    ('Bayern München'),
    ('Bayer Leverkusen'),
    ('Eintracht Frankfurt'),
    ('Borussia Dortmund'),

    -- França
    ('Paris Saint-Germain'),
    ('Marseille'),
    ('Monaco'),

    -- Países Baixos
    ('PSV'),
    ('Ajax'),

    -- Portugal
    ('Sporting CP'),
    ('Benfica'),

    -- Bélgica
    ('Union Saint-Gilloise'),
    ('Club Brugge'),

    -- Outros países
    ('Galatasaray'),
    ('Slavia Praha'),
    ('Olympiacos'),
    ('Copenhagen'),
    ('Bodø/Glimt'),
    ('Pafos'),
    ('Qarabağ')

ON CONFLICT (name) DO NOTHING;

COMMIT;