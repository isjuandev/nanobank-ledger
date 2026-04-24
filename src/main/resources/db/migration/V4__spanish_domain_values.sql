ALTER TABLE wallets DROP CONSTRAINT IF EXISTS chk_wallet_type;
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS chk_transaction_type;

UPDATE wallets
SET type = CASE type
    WHEN 'SAVINGS' THEN 'AHORROS'
    WHEN 'EXPENSE' THEN 'GASTOS'
    WHEN 'INVESTMENT' THEN 'INVERSION'
    ELSE type
END;

UPDATE transactions
SET type = CASE type
    WHEN 'INCOME' THEN 'INGRESO'
    WHEN 'EXPENSE' THEN 'GASTO'
    ELSE type
END;

ALTER TABLE wallets
    ADD CONSTRAINT chk_wallet_type
    CHECK (type IN ('AHORROS', 'GASTOS', 'INVERSION'));

ALTER TABLE transactions
    ADD CONSTRAINT chk_transaction_type
    CHECK (type IN ('INGRESO', 'GASTO'));
