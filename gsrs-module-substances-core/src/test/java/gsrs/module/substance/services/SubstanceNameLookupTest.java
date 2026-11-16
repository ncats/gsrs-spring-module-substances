package gsrs.module.substance.services;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubstanceNameLookupTest {
    @Test
    void unsupportedDatabaseUsesTheOriginalLookupWithoutChangingTheTable() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        try (Connection connection = source.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE ix_ginas_name (name VARCHAR(1024), owner_uuid UUID)");
                statement.execute("CREATE INDEX name_index ON ix_ginas_name(name)");
                statement.execute("INSERT INTO ix_ginas_name SELECT 'unrelated ' || X, RANDOM_UUID() FROM SYSTEM_RANGE(1, 4000)");
            }
            UUID owner = UUID.randomUUID();
            String longName = "a".repeat(128) + "target";
            insert(connection, "MiXeD Case", owner);
            insert(connection, longName, owner);
            insert(connection, "a".repeat(128) + "not target", UUID.randomUUID());

            SubstanceNameLookup lookup = new SubstanceNameLookup(source);
            assertFalse(lookup.isReady());
            lookup.initialize();
            lookup.initialize();
            assertFalse(lookup.isReady());

            assertEquals(List.of(owner), matches(connection, "mixed CASE"));
            assertEquals(List.of(owner), matches(connection, longName.toUpperCase()));
            assertTrue(matches(connection, "missing name").isEmpty());
            assertFalse(hasColumn(connection, "name_upper_prefix"));
            assertFalse(hasIndex(connection, SubstanceNameLookup.INDEX));

            try (SessionFactory factory = new Configuration()
                    .addAnnotatedClass(NameLookupRow.class)
                    .setProperty("hibernate.connection.url", source.getURL())
                    .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                    .buildSessionFactory();
                 EntityManager manager = factory.createEntityManager()) {
                ReflectionTestUtils.setField(lookup, "entityManager", manager);
                ReflectionTestUtils.setField(lookup, "lookupSql", SubstanceNameLookup.lookupSql());
                assertEquals(List.of(owner), lookup.findOwnerIds("mixed CASE"));
                manager.getTransaction().begin();
                UUID newOwner = UUID.randomUUID();
                NameLookupRow row = new NameLookupRow();
                row.ownerUuid = newOwner;
                row.name = "not yet flushed";
                manager.persist(row);
                assertEquals(List.of(newOwner), lookup.findOwnerIds("NOT YET FLUSHED"));
                manager.getTransaction().rollback();
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO ix_ginas_name(name, owner_uuid) SELECT 'later ' || X, RANDOM_UUID() FROM SYSTEM_RANGE(1, 20000)");
                statement.execute("UPDATE ix_ginas_name SET name = 'Renamed' WHERE name = 'MiXeD Case'");
            }
            assertTrue(matches(connection, "mixed case").isEmpty());
            assertEquals(List.of(owner), matches(connection, "RENAMED"));

            SubstanceNameLookup afterRestart = new SubstanceNameLookup(source);
            afterRestart.initialize();
            assertFalse(afterRestart.isReady());
            assertFalse(hasColumn(connection, "name_upper_prefix"));
            assertFalse(hasIndex(connection, SubstanceNameLookup.INDEX));
        }
    }

    @Test
    void functionBasedIndexIsLimitedToOracleAndPostgreSQL() {
        for (String product : List.of("PostgreSQL", "Oracle")) {
            assertTrue(SubstanceNameLookup.supportsIndex(product), product);
            assertTrue(SubstanceNameLookup.lookupSql().contains("UPPER(name) = UPPER(?1)"), product);
        }
        for (String product : List.of("MySQL", "MariaDB", "Microsoft SQL Server", "H2", "unsupported")) {
            assertFalse(SubstanceNameLookup.supportsIndex(product), product);
        }
    }

    @Test
    void supportedDatabasesInspectIndexWithoutExecutingDdl() throws Exception {
        for (String product : List.of("Oracle", "PostgreSQL")) {
            for (boolean indexExists : List.of(false, true)) {
                DataSource source = mock(DataSource.class);
                Connection connection = mock(Connection.class);
                DatabaseMetaData metadata = mock(DatabaseMetaData.class);
                ResultSet tables = mock(ResultSet.class);
                ResultSet indexes = mock(ResultSet.class);
                when(source.getConnection()).thenReturn(connection);
                when(connection.getMetaData()).thenReturn(metadata);
                when(metadata.getDatabaseProductName()).thenReturn(product);
                when(metadata.getTables(null, null, SubstanceNameLookup.TABLE, new String[]{"TABLE"}))
                        .thenReturn(tables);
                when(tables.next()).thenReturn(true, false);
                when(tables.getString("TABLE_NAME")).thenReturn(SubstanceNameLookup.TABLE);
                when(metadata.getIndexInfo(null, null, SubstanceNameLookup.TABLE, false, false))
                        .thenReturn(indexes);
                when(indexes.next()).thenReturn(indexExists, false);
                when(indexes.getString("INDEX_NAME")).thenReturn(SubstanceNameLookup.INDEX);

                SubstanceNameLookup lookup = new SubstanceNameLookup(source);
                lookup.initialize();

                assertEquals(indexExists, lookup.isReady(), product);
                verify(connection, never()).createStatement();
                verify(connection, never()).prepareStatement(anyString());
                verify(connection, never()).commit();
            }
        }
    }

    @Test
    void metadataErrorsAreReported() throws Exception {
        DataSource source = mock(DataSource.class);
        when(source.getConnection()).thenThrow(new SQLException("connection unavailable"));
        SubstanceNameLookup lookup = new SubstanceNameLookup(source);
        assertThrows(IllegalStateException.class, lookup::initialize);
        assertFalse(lookup.isReady());
    }

    @Test
    void unsupportedDatabaseLeavesLookupUninitialized() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        SubstanceNameLookup lookup = new SubstanceNameLookup(source);
        assertDoesNotThrow(lookup::initialize);
        assertFalse(lookup.isReady());
        assertThrows(IllegalStateException.class, () -> lookup.findOwnerIds("test"));
    }

    private static void insert(Connection connection, String name, UUID owner) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO ix_ginas_name(name, owner_uuid) VALUES (?, ?)")) {
            statement.setString(1, name);
            statement.setObject(2, owner);
            statement.executeUpdate();
        }
    }

    private static List<UUID> matches(Connection connection, String name) throws Exception {
        String sql = SubstanceNameLookup.lookupSql().replace("?1", "?");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            List<UUID> matches = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    matches.add(rows.getObject(1, UUID.class));
                }
            }
            return matches;
        }
    }

    private static boolean hasColumn(Connection connection, String columnName) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, "IX_GINAS_NAME", null)) {
            while (columns.next()) {
                if (columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasIndex(Connection connection, String indexName) throws Exception {
        try (ResultSet indexes = connection.getMetaData()
                .getIndexInfo(null, null, "IX_GINAS_NAME", false, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Entity
    @Table(name = "ix_ginas_name")
    static class NameLookupRow {
        @Id
        @Column(name = "owner_uuid")
        UUID ownerUuid;

        String name;
    }
}
