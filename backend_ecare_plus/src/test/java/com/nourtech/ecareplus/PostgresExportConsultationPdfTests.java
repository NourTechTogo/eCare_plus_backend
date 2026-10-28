package com.nourtech.ecareplus;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.DriverManager;
import java.util.UUID;

@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("postgres-test")
@EnabledIfEnvironmentVariable(named = "ECARE_TEST_POSTGRES_URL", matches = "jdbc:postgresql:.*")
class PostgresExportConsultationPdfTests extends ExportConsultationPdfIntegrationTests {
    static final String SCHEMA = "ecare_test_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void schema(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        registry.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA);
    }

    @AfterAll
    static void nettoyer() throws Exception {
        try (var connection = DriverManager.getConnection(System.getenv("ECARE_TEST_POSTGRES_URL"),
                System.getenv("ECARE_TEST_POSTGRES_USER"), System.getenv("ECARE_TEST_POSTGRES_PASSWORD"));
             var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }
}
