package gsrs.module.substance.services;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessingJobRelationshipSchemaInitializerTest {

    @Test
    void removesLegacyUniqueRulesAndAllowsJobsToShareOwnerAndPayload() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:processing_job_relationships;DB_CLOSE_DELAY=-1");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE ix_core_procjob ("
                    + "id BIGINT PRIMARY KEY, owner_id BIGINT, payload_id VARCHAR(36), "
                    + "CONSTRAINT ukn40jpberx7pnh9qaxao93qrxr UNIQUE (owner_id), "
                    + "CONSTRAINT ukiqr7nxep1eeknhaaw7aoopm9x UNIQUE (payload_id))");
            statement.execute("INSERT INTO ix_core_procjob VALUES (1, 7, 'shared-file')");
        }

        ProcessingJobRelationshipSchemaInitializer initializer =
                new ProcessingJobRelationshipSchemaInitializer(dataSource);
        initializer.ensureCompatibility();
        assertDoesNotThrow(initializer::ensureCompatibility);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO ix_core_procjob VALUES (2, 7, 'shared-file')");
            try (ResultSet result = statement.executeQuery(
                    "SELECT COUNT(*) FROM ix_core_procjob WHERE owner_id = 7 AND payload_id = 'shared-file'")) {
                result.next();
                assertEquals(2, result.getInt(1));
            }
        }
    }
}
