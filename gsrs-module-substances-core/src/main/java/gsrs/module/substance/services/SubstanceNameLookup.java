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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * An indexed lookup for case-insensitive name searches on Oracle and PostgreSQL.
 */
@Slf4j
public class SubstanceNameLookup {
    static final String TABLE = "ix_ginas_name";
    static final String INDEX = "ix_ginas_name_upper";

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
     * Only inspects the schema; index creation is handled by an administrator.
     */
    public synchronized void initialize() {
        if (isReady()) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String product = metadata.getDatabaseProductName();
            if (!supportsIndex(product)) {
                log.info("Indexed case-insensitive name lookup is not enabled for {}; using the original lookup",
                        product);
                return;
            }
            String schema = connection.getSchema();
            String catalog = connection.getCatalog();
            String table = existingTable(metadata, catalog, schema);
            if (table == null) {
                throw new IllegalStateException("Cannot prepare indexed name lookup: " + TABLE + " was not found");
            }
            if (!hasIndex(metadata, catalog, schema, table)) {
                log.warn("Index {} was not found on {}; using the original lookup. "
                        + "An administrator can run the appropriate manual name-lookup index script.",
                        INDEX, TABLE);
                return;
            }
            lookupSql = lookupSql();
            log.info("Case-insensitive substance name lookup is enabled with index {} on {}", INDEX, product);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to inspect the index for substance name lookup", e);
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
                .getResultList();
    }

    static String lookupSql() {
        return "SELECT DISTINCT owner_uuid FROM " + TABLE
                + " WHERE UPPER(name) = UPPER(?1) AND owner_uuid IS NOT NULL";
    }
    static boolean supportsIndex(String product) {
        String database = product.toLowerCase(Locale.ROOT);
        return database.contains("oracle") || database.contains("postgresql");
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
}
