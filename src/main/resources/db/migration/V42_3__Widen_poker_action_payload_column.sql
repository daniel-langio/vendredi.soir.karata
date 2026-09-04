-- The poker_action table is created by Hibernate ddl-auto (no prior Flyway migration for it),
-- so it will not exist yet on a brand new database at this point in the migration chain; guard
-- the ALTER so this migration is a no-op there and Hibernate creates the column as TEXT directly.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'poker_action') THEN
        ALTER TABLE poker_action ALTER COLUMN payload TYPE TEXT;
    END IF;
END $$;
