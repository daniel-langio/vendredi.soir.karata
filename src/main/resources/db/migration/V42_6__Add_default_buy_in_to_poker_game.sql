DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'poker_game') THEN
        ALTER TABLE poker_game ADD COLUMN IF NOT EXISTS default_buy_in BIGINT;
    END IF;
END $$;
