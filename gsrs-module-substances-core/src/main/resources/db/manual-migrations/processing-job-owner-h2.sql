-- H2 2.x
-- Run with the application stopped, after backing up the database.
-- This handles the unique constraints Hibernate creates for @OneToOne.
-- Unquoted identifiers are normalized by H2 to uppercase.

ALTER TABLE ix_core_procjob DROP CONSTRAINT IF EXISTS ukn40jpberx7pnh9qaxao93qrxr;
ALTER TABLE ix_core_procjob DROP CONSTRAINT IF EXISTS ukiqr7nxep1eeknhaaw7aoopm9x;
DROP INDEX IF EXISTS ukn40jpberx7pnh9qaxao93qrxr;
DROP INDEX IF EXISTS ukiqr7nxep1eeknhaaw7aoopm9x;

CREATE INDEX IF NOT EXISTS ix_procjob_owner_nonunique
    ON ix_core_procjob (owner_id);
CREATE INDEX IF NOT EXISTS ix_procjob_payload_nonunique
    ON ix_core_procjob (payload_id);

SELECT i.INDEX_NAME, c.COLUMN_NAME
  FROM INFORMATION_SCHEMA.INDEXES i
  JOIN INFORMATION_SCHEMA.INDEX_COLUMNS c
    ON c.INDEX_CATALOG = i.INDEX_CATALOG
   AND c.INDEX_SCHEMA = i.INDEX_SCHEMA
   AND c.INDEX_NAME = i.INDEX_NAME
 WHERE i.TABLE_NAME = 'IX_CORE_PROCJOB'
 ORDER BY i.INDEX_NAME, c.ORDINAL_POSITION;
