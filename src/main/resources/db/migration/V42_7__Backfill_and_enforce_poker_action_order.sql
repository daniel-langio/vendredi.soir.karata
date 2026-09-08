-- action_order was declared in Java as a DB-generated identity column, but the actual column was
-- never created as one (plain nullable integer, no default/identity) - every row has always had
-- action_order = NULL, so every replay of these actions (the entire event-sourced domain model)
-- has never had a guaranteed-correct order. Backfill a correct order from each row's timestamp
-- (a secondary tiebreak on id makes this deterministic even for same-timestamp rows), then make
-- the column authoritative going forward - the application now assigns it explicitly on every
-- insert (see GameService.saveAction).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'poker_action') THEN
        UPDATE poker_action pa
        SET action_order = sub.rn
        FROM (
            SELECT id, ROW_NUMBER() OVER (PARTITION BY game_id ORDER BY timestamp, id) - 1 AS rn
            FROM poker_action
        ) sub
        WHERE pa.id = sub.id AND pa.action_order IS NULL;

        ALTER TABLE poker_action ALTER COLUMN action_order SET NOT NULL;
    END IF;
END $$;
