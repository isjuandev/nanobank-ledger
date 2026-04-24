CREATE TABLE IF NOT EXISTS wallets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(30) NOT NULL,
    balance NUMERIC(19,2) NOT NULL DEFAULT 0,
    owner_id BIGINT NOT NULL,
    CONSTRAINT fk_wallet_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_wallet_type
        CHECK (type IN ('AHORROS', 'GASTOS', 'INVERSION')),
    CONSTRAINT chk_wallet_balance_scale
        CHECK (balance = ROUND(balance, 2))
);

CREATE INDEX IF NOT EXISTS idx_wallets_owner_id ON wallets(owner_id);
CREATE INDEX IF NOT EXISTS idx_wallets_owner_type ON wallets(owner_id, type);
