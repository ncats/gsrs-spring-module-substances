package gsrs.module.substance.services;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class SubstanceLobSchemaCompatibilityInitializer {
    private static final List<ColumnRef> REQUIRED_LOB_COLUMNS = List.of(
            new ColumnRef("ix_ginas_protein", "disulf_json"),
            new ColumnRef("ix_ginas_site_lob", "sites_json"),
            new ColumnRef("ix_ginas_site_lob", "sites_short_hand")
    );

    private final DataSource dataSource;
    private final AtomicBoolean compatibilityVerified = new AtomicBoolean();

    public SubstanceLobSchemaCompatibilityInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void ensureCompatibility() {
        if (dataSource == null || compatibilityVerified.get()) {
            return;
        }
        synchronized (compatibilityVerified) {
            if (compatibilityVerified.get()) {
                return;
            }
            verifyAndUpgradeColumns();
            compatibilityVerified.set(true);
        }
    }

    private void verifyAndUpgradeColumns() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName();
            for (ColumnRef column : REQUIRED_LOB_COLUMNS) {
                ensureLargeTextColumn(connection, metaData, databaseProductName, column);
            }
            log.info("LOB compatibility confirmed for {}", databaseProductName);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to verify or upgrade substance LOB compatibility", e);
        }
    }

    private void ensureLargeTextColumn(Connection connection,
                                       DatabaseMetaData metaData,
                                       String databaseProductName,
                                       ColumnRef column) throws SQLException {
        Optional<ColumnType> currentType = findColumnType(connection, metaData, column);
        if (currentType.isPresent() && isCompatibleColumnType(databaseProductName, currentType.get())) {
            return;
        }

        String ddl = buildAlterStatement(databaseProductName, column.tableName(), column.columnName());
        if (ddl == null) {
            if (currentType.isEmpty()) {
                log.debug("Column {}.{} was not found; skipping compatibility check for {}",
                        column.tableName(), column.columnName(), databaseProductName);
            }
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed applying LOB compatibility DDL: " + ddl, e);
        }

        Optional<ColumnType> upgradedType = findColumnType(connection, metaData, column);
        if (upgradedType.isEmpty() || !isCompatibleColumnType(databaseProductName, upgradedType.get())) {
            throw new IllegalStateException("LOB compatibility verification failed for "
                    + column.tableName() + "." + column.columnName() + " after DDL " + ddl);
        }
        log.info("Applied LOB compatibility DDL: {}", ddl);
    }

    private Optional<ColumnType> findColumnType(Connection connection,
                                                DatabaseMetaData metaData,
                                                ColumnRef column) throws SQLException {
        List<String> schemas = new ArrayList<>();
        String schema = connection.getSchema();
        if (schema != null && !schema.isBlank()) {
            schemas.add(schema);
        }
        schemas.add(null);
        String catalog = connection.getCatalog();
        for (String candidateSchema : schemas) {
            for (String table : nameCandidates(column.tableName())) {
                for (String columnName : nameCandidates(column.columnName())) {
                    try (ResultSet resultSet = metaData.getColumns(catalog, candidateSchema, table, columnName)) {
                        if (resultSet.next()) {
                            return Optional.of(new ColumnType(
                                    resultSet.getString("TYPE_NAME"),
                                    resultSet.getInt("DATA_TYPE")));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private List<String> nameCandidates(String name) {
        return List.of(name, name.toUpperCase(Locale.ROOT), name.toLowerCase(Locale.ROOT));
    }

    static String buildAlterStatement(String databaseProductName, String tableName, String columnName) {
        String database = normalize(databaseProductName);
        if (database.contains("mariadb") || database.contains("mysql")) {
            return "ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " LONGTEXT";
        }
        if (database.contains("postgresql")) {
            return "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " TYPE TEXT";
        }
        if (database.contains("oracle")) {
            return "ALTER TABLE " + tableName + " MODIFY (" + columnName + " CLOB)";
        }
        return null;
    }

    static boolean isCompatibleColumnType(String databaseProductName, ColumnType columnType) {
        if (columnType == null) {
            return false;
        }
        String database = normalize(databaseProductName);
        String typeName = normalize(columnType.typeName());
        if (database.contains("mariadb") || database.contains("mysql")) {
            return "longtext".equals(typeName);
        }
        if (database.contains("postgresql")) {
            return "text".equals(typeName);
        }
        if (database.contains("oracle")) {
            return typeName.contains("clob");
        }
        return columnType.jdbcType() == Types.CLOB
                || columnType.jdbcType() == Types.NCLOB
                || columnType.jdbcType() == Types.LONGVARCHAR
                || columnType.jdbcType() == Types.LONGNVARCHAR;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private record ColumnRef(String tableName, String columnName) {
    }

    record ColumnType(String typeName, int jdbcType) {
    }
}
