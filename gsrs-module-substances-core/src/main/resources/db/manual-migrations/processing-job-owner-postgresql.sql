-- PostgreSQL 12+
-- Run with the application stopped, after backing up the database.
-- Uses the current schema. Change the schema/table references if the table is
-- not in current_schema().

DO $migration$
DECLARE
    target_column TEXT;
    constraint_name TEXT;
    index_name TEXT;
BEGIN
    FOREACH target_column IN ARRAY ARRAY['owner_id', 'payload_id'] LOOP
        -- Ensure there is a non-unique lookup index before dropping unique ones.
        IF NOT EXISTS (
            SELECT 1
              FROM pg_index i
              JOIN pg_class t ON t.oid = i.indrelid
              JOIN pg_namespace n ON n.oid = t.relnamespace
              JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = i.indkey[0]
             WHERE n.nspname = current_schema()
               AND t.relname = 'ix_core_procjob'
               AND a.attname = target_column
               AND i.indisunique = FALSE
               AND i.indnkeyatts >= 1
        ) THEN
            EXECUTE format('CREATE INDEX %I ON %I.ix_core_procjob (%I)',
                'ix_procjob_' || target_column || '_nonunique',
                current_schema(), target_column);
        END IF;

        -- Remove one-column UNIQUE constraints on the target column.
        FOR constraint_name IN
            SELECT c.conname
              FROM pg_constraint c
              JOIN pg_class t ON t.oid = c.conrelid
              JOIN pg_namespace n ON n.oid = t.relnamespace
              JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = c.conkey[1]
             WHERE n.nspname = current_schema()
               AND t.relname = 'ix_core_procjob'
               AND c.contype = 'u'
               AND array_length(c.conkey, 1) = 1
               AND a.attname = target_column
        LOOP
            EXECUTE format('ALTER TABLE %I.ix_core_procjob DROP CONSTRAINT %I',
                current_schema(), constraint_name);
        END LOOP;

        -- Also handle standalone unique indexes not owned by a constraint.
        FOR index_name IN
            SELECT idx.relname
              FROM pg_index i
              JOIN pg_class t ON t.oid = i.indrelid
              JOIN pg_namespace n ON n.oid = t.relnamespace
              JOIN pg_class idx ON idx.oid = i.indexrelid
              JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = i.indkey[0]
             WHERE n.nspname = current_schema()
               AND t.relname = 'ix_core_procjob'
               AND i.indisunique = TRUE
               AND i.indnkeyatts = 1
               AND a.attname = target_column
               AND NOT EXISTS (
                   SELECT 1 FROM pg_constraint c WHERE c.conindid = i.indexrelid
               )
        LOOP
            EXECUTE format('DROP INDEX %I.%I', current_schema(), index_name);
        END LOOP;
    END LOOP;
END
$migration$;

-- Confirm indexes and constraints.
SELECT indexname, indexdef
  FROM pg_indexes
 WHERE schemaname = current_schema()
   AND tablename = 'ix_core_procjob';
