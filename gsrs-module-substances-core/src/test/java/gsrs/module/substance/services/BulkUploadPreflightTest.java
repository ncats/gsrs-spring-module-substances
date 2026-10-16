package gsrs.module.substance.services;

import gsrs.payload.LegacyPayloadConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BulkUploadPreflightTest {
    @Test
    void configuredLimitAppliesToEveryDatabaseWithoutOpeningAConnection() {
        DataSource source = mock(DataSource.class);
        BulkUploadPreflight preflight = new BulkUploadPreflight(
                source, new LegacyPayloadConfiguration(), DataSize.ofMegabytes(100));
        assertTrue(preflight.rejectionFor(DataSize.ofMegabytes(101).toBytes()).orElseThrow()
                .contains("ix.ginas.batch.maxUploadSize"));
        verifyNoInteractions(source);
    }

    @Test
    void fileStorageDoesNotRequireDatabasePacketCapacity() {
        DataSource source = mock(DataSource.class);
        LegacyPayloadConfiguration config = mock(LegacyPayloadConfiguration.class);
        BulkUploadPreflight preflight = new BulkUploadPreflight(source, config, DataSize.ofMegabytes(100));
        assertTrue(preflight.rejectionFor(58_708_950).isEmpty());
        verifyNoInteractions(source);
    }

    @Test
    void mariaDbAndMySqlUseTheSmallerSessionOrGlobalLimitAndEncodingHeadroom() throws Exception {
        for (String product : new String[]{"MariaDB", "MySQL"}) {
            Fixture f = new Fixture(product);
            when(f.limits.next()).thenReturn(true);
            when(f.limits.getLong(1)).thenReturn(16_777_216L, 268_435_456L, 268_435_456L);
            when(f.limits.getLong(2)).thenReturn(268_435_456L, 16_777_216L, 268_435_456L);
            String first = f.preflight.rejectionFor(58_708_950).orElseThrow();
            assertTrue(first.contains("max_allowed_packet"));
            assertTrue(first.contains("restart GSRS"));
            assertTrue(first.contains("16777216"));
            assertTrue(f.preflight.rejectionFor(58_708_950).isPresent());
            assertTrue(f.preflight.rejectionFor(58_708_950).isEmpty());
            verify(f.connection, times(3)).close();
            verify(f.statement, times(3)).close();
            verify(f.limits, times(3)).close();
        }
    }

    @Test
    void packetBoundaryIsConservativeIncludingBinaryEscaping() throws Exception {
        Fixture f = new Fixture("MariaDB");
        when(f.limits.next()).thenReturn(true);
        long limit = 16_777_216L;
        when(f.limits.getLong(1)).thenReturn(limit);
        when(f.limits.getLong(2)).thenReturn(limit);
        long boundary = (limit - 64 * 1024) / 2;
        assertTrue(f.preflight.rejectionFor(boundary - 1).isEmpty());
        assertTrue(f.preflight.rejectionFor(boundary).isPresent());
    }

    @Test
    void otherDatabasesDoNotExecuteMySqlVariables() throws Exception {
        for (String product : new String[]{"PostgreSQL", "Oracle", "Microsoft SQL Server", "H2"}) {
            Fixture f = new Fixture(product);
            assertTrue(f.preflight.rejectionFor(58_708_950).isEmpty(), product);
            verify(f.connection, never()).createStatement();
        }
    }

    @Test
    void postgresByteaLimitCannotBeOverriddenByTheApplicationCap() throws Exception {
        Fixture f = new Fixture("PostgreSQL", DataSize.ofMegabytes(1500));
        assertTrue(f.preflight.rejectionFor(1L << 30).orElseThrow().contains("bytea"));
    }

    @Test
    void databaseErrorsAndInvalidLimitsAreSurfaced() throws Exception {
        Fixture f = new Fixture("MariaDB");
        when(f.limits.next()).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> f.preflight.rejectionFor(100));
        when(f.source.getConnection()).thenThrow(new SQLException("offline"));
        assertThrows(IllegalStateException.class, () -> f.preflight.rejectionFor(100));
    }

    @Test
    void unsafeConfiguredCapsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BulkUploadPreflight(
                mock(DataSource.class), new LegacyPayloadConfiguration(), DataSize.ofBytes(0)));
        assertThrows(IllegalArgumentException.class, () -> new BulkUploadPreflight(
                mock(DataSource.class), new LegacyPayloadConfiguration(), DataSize.ofGigabytes(2)));
    }

    private static class Fixture {
        final DataSource source = mock(DataSource.class);
        final Connection connection = mock(Connection.class);
        final Statement statement = mock(Statement.class);
        final ResultSet limits = mock(ResultSet.class);
        final BulkUploadPreflight preflight;

        Fixture(String product) throws Exception {
            this(product, DataSize.ofMegabytes(100));
        }

        Fixture(String product, DataSize cap) throws Exception {
            DatabaseMetaData metadata = mock(DatabaseMetaData.class);
            when(source.getConnection()).thenReturn(connection);
            when(connection.getMetaData()).thenReturn(metadata);
            when(metadata.getDatabaseProductName()).thenReturn(product);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(anyString())).thenReturn(limits);
            LegacyPayloadConfiguration config = mock(LegacyPayloadConfiguration.class);
            when(config.shouldPersistInDb()).thenReturn(true);
            preflight = new BulkUploadPreflight(source, config, cap);
        }
    }
}
