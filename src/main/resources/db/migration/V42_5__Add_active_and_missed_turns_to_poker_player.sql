DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'poker_player') THEN
        ALTER TABLE poker_player ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
        ALTER TABLE poker_player ADD COLUMN IF NOT EXISTS missed_turns INTEGER NOT NULL DEFAULT 0;
    END IF;
END $$;
