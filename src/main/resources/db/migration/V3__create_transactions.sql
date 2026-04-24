CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    amount NUMERIC(19,2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    category VARCHAR(255) NOT NULL,
    description TEXT,
    date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    wallet_id BIGINT NOT NULL,
    CONSTRAINT fk_transaction_wallet
        FOREIGN KEY (wallet_id)
        REFERENCES wallets(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_transaction_type
        CHECK (type IN ('INGRESO', 'GASTO')),
    CONSTRAINT chk_transaction_amount_positive
        CHECK (amount > 0),
    CONSTRAINT chk_transaction_amount_scale
        CHECK (amount = ROUND(amount, 2))
);

CREATE INDEX IF NOT EXISTS idx_transactions_wallet_id ON transactions(wallet_id);
CREATE INDEX IF NOT EXISTS idx_transactions_wallet_date ON transactions(wallet_id, date);
CREATE INDEX IF NOT EXISTS idx_transactions_wallet_category ON transactions(wallet_id, category);
