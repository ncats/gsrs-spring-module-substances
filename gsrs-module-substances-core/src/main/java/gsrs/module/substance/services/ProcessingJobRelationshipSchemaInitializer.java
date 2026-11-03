package gsrs.module.substance.services;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Removes legacy one-to-one uniqueness from processing-job owner and payload
 * relationships before the first bulk import on an existing database.
 */
@Slf4j
public class ProcessingJobRelationshipSchemaInitializer {
    private static final String TABLE_NAME = "ix_core_procjob";
    private static final List<String> TARGET_COLUMNS = List.of("owner_id", "payload_id");

    private final DataSource dataSource;
    private volatile boolean initialized;

    public ProcessingJobRelationshipSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public synchronized void ensureCompatibility() {
        if (initialized) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            TableRef table = findTable(connection, metadata);
            List<IndexRef> indexes = findIndexes(metadata, table);
            for (String column : TARGET_COLUMNS) {
                removeSingleColumnUniqueIndexes(connection, metadata, table, indexes, column);
                indexes = findIndexes(metadata, table);
            }
            initialized = true;
            log.info("Processing-job owner/payload relationships verified for {}",
                    metadata.getDatabaseProductName());
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Unable to update legacy unique indexes on ix_core_procjob owner_id/payload_id", e);
        }
    }

    private TableRef findTable(Connection connection, DatabaseMetaData metadata) throws SQLException {
        String catalog = connection.getCatalog();
        List<String> schemas = new ArrayList<>();
        String currentSchema = connection.getSchema();
        if (currentSchema != null && !currentSchema.isBlank()) {
            schemas.add(currentSchema);
        }
        schemas.add(null);

        for (String schema : schemas) {
            for (String candidate : nameCandidates(TABLE_NAME)) {
                try (ResultSet tables = metadata.getTables(catalog, schema, candidate, new String[]{"TABLE"})) {
                    if (tables.next()) {
                        return new TableRef(tables.getString("TABLE_CAT"),
                                tables.getString("TABLE_SCHEM"), tables.getString("TABLE_NAME"));
                    }
                }
            }
        }
        throw new SQLException("Table " + TABLE_NAME + " was not found in the current database schema");
    }

    private List<IndexRef> findIndexes(DatabaseMetaData metadata, TableRef table) throws SQLException {
        Map<String, MutableIndex> indexes = new LinkedHashMap<>();
        try (ResultSet rows = metadata.getIndexInfo(table.catalog(), table.schema(), table.name(), true, false)) {
            while (rows.next()) {
                String name = rows.getString("INDEX_NAME");
                String column = rows.getString("COLUMN_NAME");
                short type = rows.getShort("TYPE");
                if (name == null || column == null || type == DatabaseMetaData.tableIndexStatistic) {
                    continue;
                }
                boolean unique = !rows.getBoolean("NON_UNIQUE");
                MutableIndex index = indexes.computeIfAbsent(name,
                        ignored -> new MutableIndex(name, unique));
                index.columns.add(new IndexColumn(rows.getShort("ORDINAL_POSITION"), column));
            }
        }
        List<IndexRef> result = new ArrayList<>();
        for (MutableIndex index : indexes.values()) {
            index.columns.sort(Comparator.comparingInt(IndexColumn::ordinal));
            result.add(new IndexRef(index.name, index.unique,
                    index.columns.stream().map(IndexColumn::name).toList()));
        }
        return result;
    }

    private void removeSingleColumnUniqueIndexes(Connection connection,
                                                 DatabaseMetaData metadata,
                                                 TableRef table,
                                                 List<IndexRef> indexes,
                                                 String targetColumn) throws SQLException {
        for (IndexRef index : indexes) {
            if (!index.unique()
                    || index.columns().size() != 1
                    || !targetColumn.equalsIgnoreCase(index.columns().get(0))) {
                continue;
            }

            if (!hasNonUniqueLeadingIndex(indexes, targetColumn)) {
                String supportingName = targetColumn.equals("owner_id")
                        ? "ix_procjob_owner_nu" : "ix_procjob_payload_nu";
                ensureIndexNameAvailable(indexes, supportingName, targetColumn);
                execute(connection, createIndexSql(metadata, table, supportingName, index.columns().get(0)));
                indexes = findIndexes(metadata, table);
            }

            String product = metadata.getDatabaseProductName().toLowerCase(Locale.ROOT);
            String constraintName = findUniqueConstraint(connection, product, table, index.name());
            execute(connection, dropUniqueSql(metadata, product, table, index.name(), constraintName));
            log.info("Removed legacy unique index {} on {}.{}", index.name(), TABLE_NAME, targetColumn);
        }
    }

    private boolean hasNonUniqueLeadingIndex(List<IndexRef> indexes, String column) {
        return indexes.stream().anyMatch(index ->
                !index.unique() && !index.columns().isEmpty()
                        && column.equalsIgnoreCase(index.columns().get(0)));
    }

    private void ensureIndexNameAvailable(List<IndexRef> indexes, String name, String column) throws SQLException {
        for (IndexRef index : indexes) {
            if (index.name().equalsIgnoreCase(name)
                    && (index.unique() || index.columns().isEmpty()
                    || !column.equalsIgnoreCase(index.columns().get(0)))) {
                throw new SQLException("Cannot create supporting index " + name
                        + ": an index with that name already exists on different columns");
            }
        }
    }

    private String findUniqueConstraint(Connection connection, String product,
                                        TableRef table, String indexName) throws SQLException {
        String sql;
        if (product.contains("postgresql")) {
            sql = "SELECT c.conname FROM pg_constraint c "
                    + "JOIN pg_class t ON t.oid = c.conrelid "
                    + "JOIN pg_namespace n ON n.oid = t.relnamespace "
                    + "JOIN pg_class i ON i.oid = c.conindid "
                    + "WHERE UPPER(n.nspname) = UPPER(?) AND UPPER(t.relname) = UPPER(?) "
                    + "AND UPPER(i.relname) = UPPER(?) AND c.contype = 'u'";
        } else if (product.contains("oracle")) {
            sql = "SELECT constraint_name FROM all_constraints "
                    + "WHERE UPPER(owner) = UPPER(?) AND UPPER(table_name) = UPPER(?) "
                    + "AND UPPER(index_name) = UPPER(?) AND constraint_type = 'U'";
        } else if (product.contains("microsoft")) {
            sql = "SELECT kc.name FROM sys.key_constraints kc "
                    + "JOIN sys.tables t ON t.object_id = kc.parent_object_id "
                    + "JOIN sys.schemas s ON s.schema_id = t.schema_id "
                    + "JOIN sys.indexes i ON i.object_id = t.object_id AND i.index_id = kc.unique_index_id "
                    + "WHERE UPPER(s.name) = UPPER(?) AND UPPER(t.name) = UPPER(?) AND UPPER(i.name) = UPPER(?)";
        } else if (product.contains("h2")) {
            sql = "SELECT constraint_name FROM information_schema.table_constraints "
                    + "WHERE UPPER(table_schema) = UPPER(?) AND UPPER(table_name) = UPPER(?) "
                    + "AND UPPER(index_name) = UPPER(?) "
                    + "AND constraint_type = 'UNIQUE'";
        } else if (product.contains("mysql") || product.contains("mariadb")) {
            return null;
        } else {
            throw new SQLException("Automatic processing-job index migration is not supported for "
                    + connection.getMetaData().getDatabaseProductName());
        }

        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, schemaOrDefault(table.schema()));
            statement.setString(2, table.name());
            statement.setString(3, indexName);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private String dropUniqueSql(DatabaseMetaData metadata, String product,
                                TableRef table, String indexName, String constraintName) throws SQLException {
        String qualifiedTable = qualifiedTable(metadata, table);
        String quotedIndex = quote(metadata, indexName);
        String quotedConstraint = constraintName == null ? null : quote(metadata, constraintName);
        if (product.contains("postgresql")) {
            return quotedConstraint != null
                    ? "ALTER TABLE " + qualifiedTable + " DROP CONSTRAINT " + quotedConstraint
                    : "DROP INDEX " + qualifiedName(metadata, table.schema(), indexName);
        }
        if (product.contains("oracle") || product.contains("h2")) {
            return quotedConstraint != null
                    ? "ALTER TABLE " + qualifiedTable + " DROP CONSTRAINT " + quotedConstraint
                    : "DROP INDEX " + quotedIndex;
        }
        if (product.contains("microsoft")) {
            return quotedConstraint != null
                    ? "ALTER TABLE " + qualifiedTable + " DROP CONSTRAINT " + quotedConstraint
                    : "DROP INDEX " + quotedIndex + " ON " + qualifiedTable;
        }
        if (product.contains("mysql") || product.contains("mariadb")) {
            return "ALTER TABLE " + qualifiedTable + " DROP INDEX " + quotedIndex;
        }
        throw new SQLException("Automatic processing-job index migration is not supported for " + product);
    }

    private String createIndexSql(DatabaseMetaData metadata, TableRef table,
                                  String indexName, String column) throws SQLException {
        return "CREATE INDEX " + quote(metadata, indexName) + " ON "
                + qualifiedTable(metadata, table) + " (" + quote(metadata, column) + ")";
    }

    private String qualifiedTable(DatabaseMetaData metadata, TableRef table) throws SQLException {
        return qualifiedName(metadata, table.schema(), table.name());
    }

    private String qualifiedName(DatabaseMetaData metadata, String schema, String name) throws SQLException {
        return (schema == null || schema.isBlank() ? "" : quote(metadata, schema) + ".")
                + quote(metadata, name);
    }

    private String quote(DatabaseMetaData metadata, String identifier) throws SQLException {
        String quote = metadata.getIdentifierQuoteString();
        if (quote == null || quote.isBlank()) {
            return identifier;
        }
        return quote + identifier.replace(quote, quote + quote) + quote;
    }

    private String schemaOrDefault(String schema) {
        return schema == null ? "" : schema;
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
        if (!connection.getAutoCommit()) {
            connection.commit();
        }
    }

    private List<String> nameCandidates(String name) {
        return new ArrayList<>(new LinkedHashSet<>(
                List.of(name, name.toUpperCase(Locale.ROOT), name.toLowerCase(Locale.ROOT))));
    }

    private record TableRef(String catalog, String schema, String name) {
    }

    private record IndexRef(String name, boolean unique, List<String> columns) {
    }

    private record IndexColumn(int ordinal, String name) {
    }

    private static final class MutableIndex {
        private final String name;
        private final boolean unique;
        private final List<IndexColumn> columns = new ArrayList<>();

        private MutableIndex(String name, boolean unique) {
            this.name = name;
            this.unique = unique;
        }
    }
}
