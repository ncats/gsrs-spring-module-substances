package gsrs.module.substance.services;

import gsrs.payload.LegacyPayloadConfiguration;
import org.springframework.util.unit.DataSize;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Optional;

/**
 * Checks upload limits before allocating the byte array or opening the payload transaction.
 */
public class BulkUploadPreflight {
    private static final long PACKET_OVERHEAD_BYTES = 64 * 1024;
    private static final long POSTGRES_MAX_BYTES = (1L << 30) - PACKET_OVERHEAD_BYTES;
    private final DataSource dataSource;
    private final LegacyPayloadConfiguration payloadConfiguration;
    private final long maxUploadBytes;

    public BulkUploadPreflight(DataSource dataSource, LegacyPayloadConfiguration payloadConfiguration,
                               DataSize maxUploadSize) {
        this.dataSource = dataSource;
        this.payloadConfiguration = payloadConfiguration;
        maxUploadBytes = maxUploadSize.toBytes();
        if (maxUploadBytes <= 0 || maxUploadBytes > Integer.MAX_VALUE - PACKET_OVERHEAD_BYTES) {
            throw new IllegalArgumentException("ix.ginas.batch.maxUploadSize must be positive and below 2 GiB");
        }
    }

    public Optional<String> rejectionFor(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Upload size cannot be negative");
        }
        if (size > maxUploadBytes) {
            return Optional.of("Bulk import file is " + size + " bytes; the configured limit is "
                    + maxUploadBytes + " bytes. Split the import file or increase "
                    + "ix.ginas.batch.maxUploadSize and the Spring multipart limits after checking "
                    + "database and application memory capacity.");
        }
        if (!payloadConfiguration.shouldPersistInDb()) {
            return Optional.empty();
        }
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            String database = product.toLowerCase(Locale.ROOT);
            if (database.contains("mariadb") || database.contains("mysql")) {
                return packetRejection(connection, size, product);
            }
            if (database.contains("postgresql") && size > POSTGRES_MAX_BYTES) {
                return Optional.of("Bulk import file exceeds PostgreSQL's approximately 1 GiB bytea limit. "
                        + "Split the import file; increasing the multipart limit cannot lift this database limit.");
            }
            // Oracle BLOB, SQL Server varbinary(max) and H2 BLOB accept the default 100 MiB cap.
            // Deployment-specific storage, proxy and memory limits still need administrator review.
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to check database upload limits; no import payload was saved", e);
        }
    }

    private Optional<String> packetRejection(Connection connection, long size, String product) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet limits = statement.executeQuery(
                     "SELECT @@SESSION.max_allowed_packet, @@GLOBAL.max_allowed_packet")) {
            if (!limits.next()) {
                throw new SQLException("The database returned no max_allowed_packet values");
            }
            long sessionLimit = limits.getLong(1);
            long globalLimit = limits.getLong(2);
            if (sessionLimit <= 0 || globalLimit <= 0) {
                throw new SQLException("The database returned an invalid max_allowed_packet value");
            }
            long packetLimit = Math.min(sessionLimit, globalLimit);
            // Text-protocol binary escaping can double the payload size. Include SQL/metadata headroom.
            long requiredPacket = 2 * size + PACKET_OVERHEAD_BYTES;
            if (requiredPacket < packetLimit) {
                return Optional.empty();
            }
            return Optional.of("Bulk import file is " + size + " bytes and may require "
                    + requiredPacket + " bytes with JDBC binary encoding. " + product
                    + " max_allowed_packet is " + packetLimit + " bytes (session=" + sessionLimit
                    + ", global=" + globalLimit + "). Split the import file, or ask the DBA to raise "
                    + "max_allowed_packet above " + requiredPacket
                    + " bytes, persist it in the database server configuration, and restart GSRS to "
                    + "refresh pooled connections. No payload was saved.");
        }
    }
}
