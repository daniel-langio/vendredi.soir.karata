DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'poker_game') THEN
        ALTER TABLE poker_game ADD COLUMN IF NOT EXISTS variant VARCHAR NOT NULL DEFAULT 'TEXAS_HOLDEM';
    END IF;
END $$;
