-- Oracle
-- Run with the application stopped, after backing up the database.
-- Uses the current user's schema and uppercase table/column names.

DECLARE
    target_column VARCHAR2(30);
    object_name VARCHAR2(128);
    index_count NUMBER;
BEGIN
    FOR col IN (SELECT 'OWNER_ID' AS name FROM dual
                UNION ALL
                SELECT 'PAYLOAD_ID' AS name FROM dual) LOOP
        target_column := col.name;

        SELECT COUNT(*)
          INTO index_count
          FROM user_indexes i
          JOIN user_ind_columns c ON c.index_name = i.index_name
         WHERE i.table_name = 'IX_CORE_PROCJOB'
           AND i.uniqueness = 'NONUNIQUE'
           AND c.column_position = 1
           AND c.column_name = target_column;

        IF index_count = 0 THEN
            object_name := CASE target_column
                WHEN 'OWNER_ID' THEN 'IX_PROCJOB_OWNER_NU'
                ELSE 'IX_PROCJOB_PAYLOAD_NU'
            END;
            EXECUTE IMMEDIATE 'CREATE INDEX ' || object_name ||
                ' ON IX_CORE_PROCJOB (' || target_column || ')';
        END IF;

        FOR con IN (
            SELECT c.constraint_name
              FROM user_constraints c
              JOIN user_cons_columns cc
                ON cc.constraint_name = c.constraint_name
               AND cc.table_name = c.table_name
             WHERE c.table_name = 'IX_CORE_PROCJOB'
               AND c.constraint_type = 'U'
               AND cc.column_name = target_column
               AND (SELECT COUNT(*) FROM user_cons_columns x
                     WHERE x.constraint_name = c.constraint_name
                       AND x.table_name = c.table_name) = 1
        ) LOOP
            EXECUTE IMMEDIATE 'ALTER TABLE IX_CORE_PROCJOB DROP CONSTRAINT ' ||
                DBMS_ASSERT.SIMPLE_SQL_NAME(con.constraint_name);
        END LOOP;

        FOR idx IN (
            SELECT i.index_name
              FROM user_indexes i
              JOIN user_ind_columns ic ON ic.index_name = i.index_name
             WHERE i.table_name = 'IX_CORE_PROCJOB'
               AND i.uniqueness = 'UNIQUE'
               AND ic.column_name = target_column
               AND ic.column_position = 1
               AND (SELECT COUNT(*) FROM user_ind_columns x
                     WHERE x.index_name = i.index_name
                       AND x.table_name = i.table_name) = 1
               AND NOT EXISTS (
                   SELECT 1 FROM user_constraints c
                    WHERE c.index_name = i.index_name
                      AND c.table_name = i.table_name
               )
        ) LOOP
            EXECUTE IMMEDIATE 'DROP INDEX ' ||
                DBMS_ASSERT.SIMPLE_SQL_NAME(idx.index_name);
        END LOOP;
    END LOOP;
END;
/

SELECT index_name, column_name, uniqueness
  FROM user_indexes i
  JOIN user_ind_columns c USING (index_name, table_name)
 WHERE table_name = 'IX_CORE_PROCJOB'
 ORDER BY index_name, column_position;
