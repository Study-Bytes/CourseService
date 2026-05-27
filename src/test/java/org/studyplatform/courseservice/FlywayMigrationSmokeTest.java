package org.studyplatform.courseservice;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationSmokeTest {

    @Test
    void shouldInitializeCleanDatabaseFromFlywayMigrations() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:course_service_flyway_" + System.nanoTime()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .defaultSchema("public")
                .load();

        MigrateResult result = flyway.migrate();

        assertEquals(5, result.migrationsExecuted);

        try (Connection connection = dataSource.getConnection()) {
            assertTrue(tableExists(connection, "flyway_schema_history"));
            assertTrue(tableExists(connection, "courses"));
            assertTrue(tableExists(connection, "course_modules"));
            assertTrue(tableExists(connection, "course_items"));
            assertTrue(tableExists(connection, "course_item_test_cases"));
            assertTrue(tableExists(connection, "course_item_options"));
            assertTrue(columnExists(connection, "courses", "submitted_for_review_at"));
            assertTrue(columnExists(connection, "courses", "reviewed_at"));
            assertTrue(columnExists(connection, "courses", "reviewed_by_user_id"));
            assertTrue(columnExists(connection, "courses", "review_comment"));
            assertTrue(columnExists(connection, "course_modules", "deadline_at"));
            assertTrue(columnExists(connection, "course_modules", "deadline_type"));
            assertTrue(columnExists(connection, "course_modules", "time_limit_minutes"));
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select count(*)
                from information_schema.tables
                where lower(table_schema) = 'public'
                  and lower(table_name) = ?
                """)) {
            statement.setString(1, tableName);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select count(*)
                from information_schema.columns
                where lower(table_schema) = 'public'
                  and lower(table_name) = ?
                  and lower(column_name) = ?
                """)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }
}
