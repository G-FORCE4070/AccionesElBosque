CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS trading;
CREATE SCHEMA IF NOT EXISTS markets;
CREATE SCHEMA IF NOT EXISTS notification;


CREATE TABLE IF NOT EXISTS identity.users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    birth_date DATE NOT NULL,
    doc_type VARCHAR(10) NOT NULL,
    doc_number VARCHAR(50) NOT NULL,
    nationality VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    income_source VARCHAR(50) NOT NULL,
    annual_income VARCHAR(20) NOT NULL,
    net_worth VARCHAR(20) NOT NULL,
    risk_tolerance VARCHAR(20) NOT NULL,
    trading_experience VARCHAR(20) NOT NULL,
    investment_goal VARCHAR(20) NOT NULL,
    is_pep BOOLEAN NOT NULL,
    is_finra_affiliated BOOLEAN NOT NULL,
    accepts_terms BOOLEAN NOT NULL,
    accepts_privacy BOOLEAN NOT NULL,
    authorize_data_use BOOLEAN NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'INVESTOR',
    plan VARCHAR(10) NOT NULL DEFAULT 'BASIC',
    alpaca_account_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS identity.user_preferences (
    user_id UUID PRIMARY KEY REFERENCES identity.users(id),
    market_open BOOLEAN DEFAULT false,
    price_alerts BOOLEAN DEFAULT false,
    daily_summary BOOLEAN DEFAULT false,
    order_confirm BOOLEAN DEFAULT true,
    notif_email BOOLEAN DEFAULT true,
    notif_sms BOOLEAN DEFAULT false,
    notif_whatsapp BOOLEAN DEFAULT false,
    monthly_report BOOLEAN DEFAULT false,
    view_type VARCHAR(20) DEFAULT 'list',
    default_order VARCHAR(20) DEFAULT 'totalValue',
    show_low_weight BOOLEAN DEFAULT true,
    default_order_type VARCHAR(20) DEFAULT 'market',
    daily_limit INTEGER DEFAULT 0,
    require_double_auth BOOLEAN DEFAULT false,
    markets VARCHAR(255),
    sectors VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS identity.watchlist_items (
    user_id UUID REFERENCES identity.users(id),
    symbol VARCHAR(10) NOT NULL,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, symbol)
);

CREATE TABLE IF NOT EXISTS identity.subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES identity.users(id),
    stripe_subscription_id VARCHAR(255) UNIQUE,
    stripe_customer_id VARCHAR(255) NOT NULL,
    plan VARCHAR(10) NOT NULL,
    billing_cycle VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    current_period_end TIMESTAMP NOT NULL,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS trading.accounts (
    user_id UUID PRIMARY KEY,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS trading.holdings (
    user_id UUID,
    symbol VARCHAR(10),
    quantity INTEGER NOT NULL,
    avg_cost DECIMAL(15,4) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, symbol)
);

CREATE TABLE IF NOT EXISTS trading.orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    side VARCHAR(4) NOT NULL,
    order_type VARCHAR(10) NOT NULL,
    quantity INTEGER NOT NULL,
    estimated_price DECIMAL(15,4),
    executed_price DECIMAL(15,4),
    commission DECIMAL(15,4),
    status VARCHAR(20) NOT NULL,
    alpaca_order_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    executed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS markets.stocks (
    symbol VARCHAR(10) PRIMARY KEY,
    company_name VARCHAR(100) NOT NULL,
    market_code VARCHAR(10) NOT NULL,
    sector VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS markets.market_schedules (
    market_code VARCHAR(10) PRIMARY KEY,
    market_name VARCHAR(100) NOT NULL,
    open_time_utc TIME NOT NULL,
    close_time_utc TIME NOT NULL,
    timezone VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true
);

INSERT INTO markets.stocks (symbol, company_name, market_code, sector, active) VALUES
    ('AAPL',  'Apple Inc.',           'NASDAQ', 'tech',     true),
    ('MSFT',  'Microsoft Corp.',      'NASDAQ', 'tech',     true),
    ('GOOGL', 'Alphabet Inc.',        'NASDAQ', 'tech',     true),
    ('AMZN',  'Amazon.com Inc.',      'NASDAQ', 'consumer', true),
    ('META',  'Meta Platforms Inc.',  'NASDAQ', 'tech',     true),
    ('TSLA',  'Tesla Inc.',           'NASDAQ', 'tech',     true),
    ('NVDA',  'NVIDIA Corp.',         'NASDAQ', 'tech',     true),
    ('JNJ',   'Johnson & Johnson',    'NYSE',   'health',   true),
    ('JPM',   'JPMorgan Chase & Co.', 'NYSE',   'consumer', true),
    ('XOM',   'Exxon Mobil Corp.',    'NYSE',   'consumer', true)
ON CONFLICT (symbol) DO NOTHING;

