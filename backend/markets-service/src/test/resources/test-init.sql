CREATE SCHEMA IF NOT EXISTS markets;

CREATE TABLE IF NOT EXISTS markets.stocks (
    symbol       VARCHAR(10)  PRIMARY KEY,
    company_name VARCHAR(100) NOT NULL,
    market_code  VARCHAR(10)  NOT NULL,
    sector       VARCHAR(20)  NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS markets.market_schedules (
    market_code    VARCHAR(10)  PRIMARY KEY,
    market_name    VARCHAR(100) NOT NULL,
    open_time_utc  TIME         NOT NULL,
    close_time_utc TIME         NOT NULL,
    timezone       VARCHAR(50)  NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT true
);

INSERT INTO markets.stocks (symbol, company_name, market_code, sector, active) VALUES
    ('AAPL',  'Apple Inc.',           'NASDAQ', 'tech',     true),
    ('MSFT',  'Microsoft Corp.',      'NASDAQ', 'tech',     true),
    ('GOOGL', 'Alphabet Inc.',        'NASDAQ', 'tech',     true),
    ('TSLA',  'Tesla Inc.',           'NASDAQ', 'tech',     true),
    ('JNJ',   'Johnson & Johnson',    'NYSE',   'health',   true),
    ('JPM',   'JPMorgan Chase & Co.', 'NYSE',   'consumer', true)
ON CONFLICT (symbol) DO NOTHING;
