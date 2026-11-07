package org.musicpracticepal.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProductionDataSourceConfigurationTests {

    @Test
    void createsMaintenanceUrlAndDatabaseNameFromConfiguredUrl() {
        ProductionDataSourceConfiguration.DatabaseTarget target =
                ProductionDataSourceConfiguration.DatabaseTarget.from(
                        "jdbc:postgresql://db.example:5432/music-practice-pal?sslmode=require");

        assertEquals("music-practice-pal", target.databaseName());
        assertEquals("jdbc:postgresql://db.example:5432/postgres?sslmode=require", target.maintenanceUrl());
    }

    @Test
    void rejectsUrlsWithoutOneDatabaseName() {
        assertThrows(IllegalArgumentException.class,
                () -> ProductionDataSourceConfiguration.DatabaseTarget.from(
                        "jdbc:postgresql://db.example:5432/"));
        assertThrows(IllegalArgumentException.class,
                () -> ProductionDataSourceConfiguration.DatabaseTarget.from(
                        "jdbc:postgresql:music-practice-pal"));
    }
}
