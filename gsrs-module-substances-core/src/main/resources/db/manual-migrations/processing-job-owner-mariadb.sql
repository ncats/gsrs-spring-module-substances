-- Run against the substances database with the application stopped, using the
-- MariaDB/MySQL command-line client. Back up the database before applying DDL.
-- Deploy the matching ProcessingJob owner/payload @ManyToOne change before restarting.
-- Removes the old one-job-per-user and one-job-per-payload unique indexes.
-- No job, user, or payload rows are changed.

DELIMITER //
CREATE PROCEDURE gsrs_drop_procjob_unique(IN idx VARCHAR(64), IN col VARCHAR(64))
BEGIN
    DECLARE index_columns INT DEFAULT 0;
    DECLARE matching_columns INT DEFAULT 0;
    DECLARE supporting_indexes INT DEFAULT 0;

    SELECT COUNT(*),
           COALESCE(SUM(CASE WHEN column_name = col AND non_unique = 0 THEN 1 ELSE 0 END), 0)
      INTO index_columns, matching_columns
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'ix_core_procjob'
       AND index_name = idx;

    IF index_columns > 0 THEN
        IF index_columns <> 1 OR matching_columns <> 1 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Migration stopped: unique index is not on the expected single column';
        END IF;

        -- Keep a non-unique index that can support the existing foreign key.
        SELECT COUNT(*)
          INTO supporting_indexes
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'ix_core_procjob'
           AND column_name = col
           AND seq_in_index = 1
           AND non_unique = 1;

        IF supporting_indexes = 0 THEN
            SET @ddl = CONCAT('ALTER TABLE ix_core_procjob ADD INDEX ix_core_procjob_', col, '_nonunique (', col, ')');
            PREPARE stmt FROM @ddl;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;

        SET @ddl = CONCAT('ALTER TABLE ix_core_procjob DROP INDEX ', idx);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL gsrs_drop_procjob_unique('ukn40jpberx7pnh9qaxao93qrxr', 'owner_id');
CALL gsrs_drop_procjob_unique('ukiqr7nxep1eeknhaaw7aoopm9x', 'payload_id');
DROP PROCEDURE gsrs_drop_procjob_unique;

SHOW CREATE TABLE ix_core_procjob;
