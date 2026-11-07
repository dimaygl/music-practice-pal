package org.musicpracticepal.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class SongTableMigrationTests {

    @Test
    void renamesLegacyTableAndPreservesRows() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:song-table-migration");
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE practice_song (id BIGINT PRIMARY KEY, name VARCHAR(255))");
            statement.execute("INSERT INTO practice_song (id, name) VALUES (1, 'Existing song')");

            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));

            try (ResultSet result = statement.executeQuery("SELECT name FROM song WHERE id = 1")) {
                result.next();
                assertEquals("Existing song", result.getString("name"));
            }
        }
    }
}
