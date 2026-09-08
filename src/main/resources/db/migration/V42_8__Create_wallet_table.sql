create table if not exists wallet
(
    id uuid
        constraint wallet_pk primary key,
    username varchar not null
        constraint wallet_username_unique unique,
    chips bigint not null
);

-- Existing accounts predate the wallet system - give each one the same starting balance a brand
-- new registration gets (see BankingService.STARTING_CHIPS), so nobody who already has an account
-- is locked out of joining a table once this ships. Guarded like the other migrations here: on a
-- fresh DB, poker_account doesn't exist yet at this point (Flyway runs before Hibernate's
-- ddl-auto creates it), so skip the backfill entirely in that case.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'poker_account') THEN
        INSERT INTO wallet (id, username, chips)
        SELECT gen_random_uuid(), pa.username, 1000
        FROM poker_account pa
        WHERE NOT EXISTS (SELECT 1 FROM wallet w WHERE w.username = pa.username);
    END IF;
END $$;
