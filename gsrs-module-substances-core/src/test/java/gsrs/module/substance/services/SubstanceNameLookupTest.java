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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SubstanceNameLookupTest {
    @Test
    void lookupUsesAnIndexAndPreservesFullCaseInsensitiveMatchingAsTheTableGrows() throws Exception {
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
            lookup.ensureIndex();
            lookup.ensureIndex();
            assertTrue(lookup.isReady());

            assertEquals(List.of(owner), matches(connection, "mixed CASE"));
            assertEquals(List.of(owner), matches(connection, longName.toUpperCase()));
            assertTrue(matches(connection, "missing name").isEmpty());
            assertIndexUsed(connection, "mixed case");

            try (SessionFactory factory = new Configuration()
                    .addAnnotatedClass(NameLookupRow.class)
                    .setProperty("hibernate.connection.url", source.getURL())
                    .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                    .buildSessionFactory();
                 EntityManager manager = factory.createEntityManager()) {
                ReflectionTestUtils.setField(lookup, "entityManager", manager);
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
            assertIndexUsed(connection, "renamed");

            SubstanceNameLookup afterRestart = new SubstanceNameLookup(source);
            afterRestart.ensureIndex();
            assertTrue(afterRestart.isReady());
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP ALL OBJECTS");
            }
        }
    }

    @Test
    void generatedColumnSyntaxCoversSupportedDatabases() {
        for (String product : List.of("PostgreSQL", "Oracle", "MySQL", "MariaDB", "Microsoft SQL Server", "H2")) {
            String ddl = SubstanceNameLookup.columnDdl(product);
            assertNotNull(ddl, product);
            assertTrue(ddl.contains(SubstanceNameLookup.COLUMN), product);
            assertTrue(SubstanceNameLookup.lookupSql(product).contains("UPPER(name) = UPPER(?2)"), product);
        }
        assertTrue(SubstanceNameLookup.columnDdl("Oracle").contains("SUBSTR(UPPER(name), 1, 128)"));
        assertTrue(SubstanceNameLookup.columnDdl("Microsoft SQL Server").endsWith("PERSISTED"));
        assertTrue(SubstanceNameLookup.columnDdl("MySQL").endsWith("STORED"));
        assertNull(SubstanceNameLookup.columnDdl("unsupported"));
    }

    @Test
    void schemaErrorsAreNotHidden() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        SubstanceNameLookup lookup = new SubstanceNameLookup(source);
        assertThrows(IllegalStateException.class, lookup::ensureIndex);
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
        String sql = SubstanceNameLookup.lookupSql("H2").replace("?1", "?").replace("?2", "?");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, name);
            List<UUID> matches = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    matches.add(rows.getObject(1, UUID.class));
                }
            }
            return matches;
        }
    }

    private static void assertIndexUsed(Connection connection, String name) throws Exception {
        String sql = SubstanceNameLookup.lookupSql("H2").replace("?1", "?").replace("?2", "?");
        try (PreparedStatement statement = connection.prepareStatement("EXPLAIN " + sql)) {
            statement.setString(1, name);
            statement.setString(2, name);
            try (ResultSet rows = statement.executeQuery()) {
                assertTrue(rows.next());
                String plan = rows.getString(1);
                assertTrue(plan.toLowerCase().contains(SubstanceNameLookup.INDEX), plan);
            }
        }

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
