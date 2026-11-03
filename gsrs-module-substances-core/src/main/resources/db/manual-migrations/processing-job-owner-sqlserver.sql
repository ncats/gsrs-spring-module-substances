-- Microsoft SQL Server
-- Run with the application stopped, after backing up the database.
-- Uses the default schema. Change @schema_name if needed.

DECLARE @schema_name sysname = SCHEMA_NAME();
DECLARE @table_id int = OBJECT_ID(QUOTENAME(@schema_name) + N'.[ix_core_procjob]');
DECLARE @column_name sysname;
DECLARE @object_name sysname;
DECLARE @sql nvarchar(max);
DECLARE @position int = 1;
DECLARE @target_columns TABLE (position int PRIMARY KEY, column_name sysname);

IF @table_id IS NULL
    THROW 50001, 'ix_core_procjob was not found in the current schema.', 1;

INSERT INTO @target_columns (position, column_name)
VALUES (1, N'owner_id'), (2, N'payload_id');

WHILE @position <= 2
BEGIN
    SELECT @column_name = column_name
      FROM @target_columns
     WHERE position = @position;

    -- Add a regular index if no non-unique index starts with this column.
    IF NOT EXISTS (
        SELECT 1
          FROM sys.indexes i
          JOIN sys.index_columns ic
            ON ic.object_id = i.object_id AND ic.index_id = i.index_id
          JOIN sys.columns c
            ON c.object_id = ic.object_id AND c.column_id = ic.column_id
         WHERE i.object_id = @table_id
           AND i.is_unique = 0
           AND ic.key_ordinal = 1
           AND c.name = @column_name
    )
    BEGIN
        SET @object_name = N'ix_procjob_' + @column_name + N'_nonunique';
        SET @sql = N'CREATE INDEX ' + QUOTENAME(@object_name) + N' ON ' +
                   QUOTENAME(@schema_name) + N'.[ix_core_procjob] (' +
                   QUOTENAME(@column_name) + N')';
        EXEC sys.sp_executesql @sql;
    END;

    -- Drop one-column unique constraints on this column.
    WHILE EXISTS (
        SELECT 1
          FROM sys.key_constraints kc
          JOIN sys.indexes i ON i.object_id = kc.parent_object_id
                             AND i.index_id = kc.unique_index_id
          JOIN sys.index_columns ic ON ic.object_id = i.object_id
                                   AND ic.index_id = i.index_id
          JOIN sys.columns c ON c.object_id = ic.object_id
                            AND c.column_id = ic.column_id
         WHERE kc.parent_object_id = @table_id
           AND kc.type = 'UQ'
           AND c.name = @column_name
           AND ic.key_ordinal = 1
           AND (SELECT COUNT(*) FROM sys.index_columns x
                 WHERE x.object_id = i.object_id AND x.index_id = i.index_id
                   AND x.key_ordinal > 0) = 1
    )
    BEGIN
        SELECT TOP (1) @object_name = kc.name
          FROM sys.key_constraints kc
          JOIN sys.indexes i ON i.object_id = kc.parent_object_id
                             AND i.index_id = kc.unique_index_id
          JOIN sys.index_columns ic ON ic.object_id = i.object_id
                                   AND ic.index_id = i.index_id
          JOIN sys.columns c ON c.object_id = ic.object_id
                            AND c.column_id = ic.column_id
         WHERE kc.parent_object_id = @table_id
           AND kc.type = 'UQ'
           AND c.name = @column_name
           AND ic.key_ordinal = 1
           AND (SELECT COUNT(*) FROM sys.index_columns x
                 WHERE x.object_id = i.object_id AND x.index_id = i.index_id
                   AND x.key_ordinal > 0) = 1;
        SET @sql = N'ALTER TABLE ' + QUOTENAME(@schema_name) +
                   N'.[ix_core_procjob] DROP CONSTRAINT ' + QUOTENAME(@object_name);
        EXEC sys.sp_executesql @sql;
    END;

    -- Drop one-column standalone unique indexes on this column.
    WHILE EXISTS (
        SELECT 1
          FROM sys.indexes i
          JOIN sys.index_columns ic ON ic.object_id = i.object_id
                                   AND ic.index_id = i.index_id
          JOIN sys.columns c ON c.object_id = ic.object_id
                            AND c.column_id = ic.column_id
         WHERE i.object_id = @table_id
           AND i.is_unique = 1
           AND i.is_primary_key = 0
           AND i.is_unique_constraint = 0
           AND c.name = @column_name
           AND ic.key_ordinal = 1
           AND (SELECT COUNT(*) FROM sys.index_columns x
                 WHERE x.object_id = i.object_id AND x.index_id = i.index_id
                   AND x.key_ordinal > 0) = 1
    )
    BEGIN
        SELECT TOP (1) @object_name = i.name
          FROM sys.indexes i
          JOIN sys.index_columns ic ON ic.object_id = i.object_id
                                   AND ic.index_id = i.index_id
          JOIN sys.columns c ON c.object_id = ic.object_id
                            AND c.column_id = ic.column_id
         WHERE i.object_id = @table_id
           AND i.is_unique = 1
           AND i.is_primary_key = 0
           AND i.is_unique_constraint = 0
           AND c.name = @column_name
           AND ic.key_ordinal = 1
           AND (SELECT COUNT(*) FROM sys.index_columns x
                 WHERE x.object_id = i.object_id AND x.index_id = i.index_id
                   AND x.key_ordinal > 0) = 1;
        SET @sql = N'DROP INDEX ' + QUOTENAME(@object_name) + N' ON ' +
                   QUOTENAME(@schema_name) + N'.[ix_core_procjob]';
        EXEC sys.sp_executesql @sql;
    END;

    SET @position += 1;
END;

SELECT i.name, i.is_unique, c.name AS column_name
  FROM sys.indexes i
  JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
  JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
 WHERE i.object_id = @table_id
 ORDER BY i.name, ic.key_ordinal;
