UPDATE account SET chat_price = 25.00 WHERE chat_price < 25.00;
ALTER TABLE account ALTER COLUMN chat_price SET DEFAULT 25.00;
ALTER TABLE account DROP CONSTRAINT IF EXISTS account_chat_price_check;
ALTER TABLE account ADD CONSTRAINT account_chat_price_check CHECK (chat_price >= 25.00);
