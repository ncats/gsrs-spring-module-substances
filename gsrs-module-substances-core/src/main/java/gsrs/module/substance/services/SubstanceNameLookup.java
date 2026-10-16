package gsrs.module.substance.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * An indexed, database-normalized candidate lookup for case-insensitive name searches.
 * The full comparison still uses UPPER(name); the prefix only narrows the candidates.
 */
@Slf4j
public class SubstanceNameLookup {
    static final String TABLE = "ix_ginas_name";
    static final String COLUMN = "name_upper_prefix";
    static final String INDEX = "ix_ginas_name_upper_prefix";
    private static final int PREFIX_LENGTH = 128;

    private final DataSource dataSource;
    private volatile String lookupSql;

    @PersistenceContext
    private EntityManager entityManager;

    public SubstanceNameLookup(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isReady() {
        return lookupSql != null;
    }

    /**
     * Called before the import opens any worker transactions, not during validation.
     * Generated columns also cover existing rows and stay current after ordinary edits.
     */
    public synchronized void ensureIndex() {
        if (isReady()) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String product = metadata.getDatabaseProductName();
            String columnDdl = columnDdl(product);
            if (columnDdl == null) {
                throw new IllegalStateException("Indexed name lookup is not supported for " + product
                        + "; set ix.ginas.batch.indexNameLookups=false to use the original lookup");
            }
            String schema = connection.getSchema();
            String catalog = connection.getCatalog();
            String table = existingTable(metadata, catalog, schema);
            if (table == null) {
                throw new IllegalStateException("Cannot prepare indexed name lookup: " + TABLE + " was not found");
            }
            if (!hasColumn(metadata, catalog, schema, table)) {
                execute(connection, columnDdl);
            }
            if (!hasIndex(metadata, catalog, schema, table)) {
                execute(connection, "CREATE INDEX " + INDEX + " ON " + TABLE + " (" + COLUMN + ")");
            }
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
            lookupSql = lookupSql(product);
            log.info("Case-insensitive substance name lookups now use {} on {}", INDEX, product);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to prepare indexed substance name lookup. "
                    + "The database account needs schema-alter permissions, or an administrator must "
                    + "create the generated column and index before importing.", e);
        }
    }

    public List<UUID> findOwnerIds(String name) {
        String sql = lookupSql;
        if (sql == null) {
            throw new IllegalStateException("Indexed substance name lookup has not been initialized");
        }
        // A typed scalar lets Hibernate handle UUID, binary UUID and uniqueidentifier storage.
        return entityManager.unwrap(Session.class).createNativeQuery(sql, UUID.class)
                .addSynchronizedQuerySpace(TABLE)
                .setParameter(1, name)
                .setParameter(2, name)
                .getResultList();
    }

    static String lookupSql(String product) {
        String prefix = prefixExpression(product, "?1");
        return "SELECT DISTINCT owner_uuid FROM " + TABLE
                + " WHERE " + COLUMN + " = " + prefix
                + " AND UPPER(name) = UPPER(?2) AND owner_uuid IS NOT NULL";
    }

    static String columnDdl(String product) {
        String database = product.toLowerCase(Locale.ROOT);
        String expression = prefixExpression(product, "name");
        if (database.contains("oracle")) {
            return "ALTER TABLE " + TABLE + " ADD (" + COLUMN
                    + " GENERATED ALWAYS AS (" + expression + ") VIRTUAL)";
        }
        if (database.contains("microsoft") || database.contains("sql server")) {
            return "ALTER TABLE " + TABLE + " ADD " + COLUMN
                    + " AS (CAST(" + expression + " AS NVARCHAR(" + PREFIX_LENGTH + "))) PERSISTED";
        }
        if (database.contains("postgresql") || database.contains("mysql") || database.contains("mariadb")) {
            return "ALTER TABLE " + TABLE + " ADD COLUMN " + COLUMN + " VARCHAR(" + PREFIX_LENGTH
                    + ") GENERATED ALWAYS AS (" + expression + ") STORED";
        }
        if (database.equals("h2")) {
            return "ALTER TABLE " + TABLE + " ADD COLUMN " + COLUMN + " VARCHAR(" + PREFIX_LENGTH
                    + ") GENERATED ALWAYS AS (" + expression + ")";
        }
        return null;
    }

    private static String prefixExpression(String product, String value) {
        String function = product.toLowerCase(Locale.ROOT).contains("oracle") ? "SUBSTR" : "SUBSTRING";
        return function + "(UPPER(" + value + "), 1, " + PREFIX_LENGTH + ")";
    }

    private static String existingTable(DatabaseMetaData metadata, String catalog, String schema) throws SQLException {
        for (String candidate : List.of(TABLE, TABLE.toUpperCase(Locale.ROOT))) {
            try (ResultSet tables = metadata.getTables(catalog, schema, candidate, new String[]{"TABLE"})) {
                while (tables.next()) {
                    if (candidate.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                        return tables.getString("TABLE_NAME");
                    }
                }
            }
        }
        return null;
    }

    private static boolean hasColumn(DatabaseMetaData metadata, String catalog, String schema,
                                     String table) throws SQLException {
        try (ResultSet columns = metadata.getColumns(catalog, schema, table, null)) {
            while (columns.next()) {
                if (COLUMN.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasIndex(DatabaseMetaData metadata, String catalog, String schema,
                                    String table) throws SQLException {
        try (ResultSet indexes = metadata.getIndexInfo(catalog, schema, table, false, false)) {
            while (indexes.next()) {
                if (INDEX.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void execute(Connection connection, String ddl) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
        }
    }
}
