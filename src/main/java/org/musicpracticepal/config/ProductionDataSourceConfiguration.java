package org.musicpracticepal.config;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("production")
class ProductionDataSourceConfiguration {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    HikariDataSource dataSource(DataSourceProperties properties) throws SQLException {
        ensureDatabaseExists(properties.getUrl(), properties.getUsername(), properties.getPassword());
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    static void ensureDatabaseExists(String jdbcUrl, String username, String password) throws SQLException {
        DatabaseTarget target = DatabaseTarget.from(jdbcUrl);

        try (Connection connection = DriverManager.getConnection(target.maintenanceUrl(), username, password);
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT 1 FROM pg_database WHERE datname = ?")) {
            statement.setString(1, target.databaseName());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return;
                }
            }

            try (Statement createStatement = connection.createStatement()) {
                createStatement.execute("CREATE DATABASE " + quoteIdentifier(target.databaseName()));
            } catch (SQLException exception) {
                if (!"42P04".equals(exception.getSQLState())) {
                    throw exception;
                }
            }
        }
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    record DatabaseTarget(String databaseName, String maintenanceUrl) {

        static DatabaseTarget from(String jdbcUrl) {
            if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://")) {
                throw new IllegalArgumentException(
                        "Production database URL must use jdbc:postgresql://host:port/database");
            }

            URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
            String path = uri.getPath();
            if (uri.getRawAuthority() == null || path == null || path.length() < 2
                    || path.indexOf('/', 1) >= 0) {
                throw new IllegalArgumentException(
                        "Production database URL must include exactly one database name");
            }

            String maintenanceUrl = "jdbc:postgresql://" + uri.getRawAuthority() + "/postgres"
                    + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
            return new DatabaseTarget(path.substring(1), maintenanceUrl);
        }
    }
}
