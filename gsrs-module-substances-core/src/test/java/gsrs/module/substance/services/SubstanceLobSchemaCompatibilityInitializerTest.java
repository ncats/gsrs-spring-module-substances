package gsrs.module.substance.services;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubstanceLobSchemaCompatibilityInitializerTest {

    @Test
    void alterStatementsShouldMatchSupportedDatabaseSyntax() {
        assertEquals("ALTER TABLE ix_ginas_site_lob MODIFY COLUMN sites_json LONGTEXT",
                SubstanceLobSchemaCompatibilityInitializer.buildAlterStatement(
                        "MariaDB", "ix_ginas_site_lob", "sites_json"));
        assertEquals("ALTER TABLE ix_ginas_site_lob MODIFY COLUMN sites_json LONGTEXT",
                SubstanceLobSchemaCompatibilityInitializer.buildAlterStatement(
                        "MySQL", "ix_ginas_site_lob", "sites_json"));
        assertEquals("ALTER TABLE ix_ginas_site_lob ALTER COLUMN sites_json TYPE TEXT",
                SubstanceLobSchemaCompatibilityInitializer.buildAlterStatement(
                        "PostgreSQL", "ix_ginas_site_lob", "sites_json"));
        assertEquals("ALTER TABLE ix_ginas_site_lob MODIFY (sites_json CLOB)",
                SubstanceLobSchemaCompatibilityInitializer.buildAlterStatement(
                        "Oracle", "ix_ginas_site_lob", "sites_json"));
    }

    @Test
    void mariaDbShouldRequireLongTextRatherThanAnUndersizedTextType() {
        assertTrue(SubstanceLobSchemaCompatibilityInitializer.isCompatibleColumnType(
                "MariaDB",
                new SubstanceLobSchemaCompatibilityInitializer.ColumnType("LONGTEXT", Types.LONGVARCHAR)));
        assertFalse(SubstanceLobSchemaCompatibilityInitializer.isCompatibleColumnType(
                "MariaDB",
                new SubstanceLobSchemaCompatibilityInitializer.ColumnType("TEXT", Types.LONGVARCHAR)));
    }

    @Test
    void ensureCompatibilityShouldAcceptExistingMariaDbLongTextColumns() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.getCatalog()).thenReturn("gsrs");
        when(metaData.getDatabaseProductName()).thenReturn("MariaDB");
        when(metaData.getColumns(any(), any(), any(), any())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("TYPE_NAME")).thenReturn("LONGTEXT");
        when(resultSet.getInt("DATA_TYPE")).thenReturn(Types.LONGVARCHAR);

        SubstanceLobSchemaCompatibilityInitializer initializer =
                new SubstanceLobSchemaCompatibilityInitializer(dataSource);

        assertDoesNotThrow(initializer::ensureCompatibility);
        verify(connection, never()).createStatement();
    }

    @Test
    void ensureCompatibilityShouldSurfaceDatabaseConnectionFailures() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("unavailable"));

        SubstanceLobSchemaCompatibilityInitializer initializer =
                new SubstanceLobSchemaCompatibilityInitializer(dataSource);

        assertThrows(IllegalStateException.class, initializer::ensureCompatibility);
    }
}
