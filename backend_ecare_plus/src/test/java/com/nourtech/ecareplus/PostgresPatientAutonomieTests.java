package com.nourtech.ecareplus;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Exercises V32 on real legacy rows in a disposable, randomly named PostgreSQL schema. */
@EnabledIfEnvironmentVariable(named = "ECARE_TEST_POSTGRES_URL", matches = "jdbc:postgresql:.*")
class PostgresPatientAutonomieTests {
    @Test
    void migrationV32AjouteUnEspacePersonnelSansModifierLesDossiersNiLesAffectations() throws Exception {
        String url = System.getenv("ECARE_TEST_POSTGRES_URL");
        String user = System.getenv("ECARE_TEST_POSTGRES_USER");
        String password = System.getenv("ECARE_TEST_POSTGRES_PASSWORD");
        String schema = "ecare_test_" + UUID.randomUUID().toString().replace("-", "");
        try {
            Flyway.configure().dataSource(url, user, password).schemas(schema).defaultSchema(schema)
                    .locations("classpath:db/migration").target("14").load().migrate();
            try (var connection = DriverManager.getConnection(url, user, password);
                 var statement = connection.createStatement()) {
                statement.execute("SET search_path TO " + schema);
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("postgres/v14-fixture.sql"));
            }
            Flyway.configure().dataSource(url, user, password).schemas(schema).defaultSchema(schema)
                    .locations("classpath:db/migration").target("31").load().migrate();
            try (var connection = DriverManager.getConnection(url, user, password);
                 var statement = connection.createStatement()) {
                statement.execute("SET search_path TO " + schema);
                String institutional = scalar(statement,
                        "SELECT jsonb_agg(to_jsonb(d) ORDER BY d.id)::text FROM dossier_suivi d WHERE structure_id IS NOT NULL");
                String assignments = scalar(statement,
                        "SELECT jsonb_agg(to_jsonb(a) ORDER BY a.id)::text FROM affectation_patient a");
                assertThat(institutional).contains("00000000-0000-0000-0000-000000000005",
                        "00000000-0000-0000-0000-000000000002", "00000000-0000-0000-0000-000000000001");

                var latest = Flyway.configure().dataSource(url, user, password).schemas(schema).defaultSchema(schema)
                        .locations("classpath:db/migration").load();
                latest.migrate();
                latest.validate();
                assertThat(latest.info().pending()).isEmpty();
                assertThat(scalar(statement,
                        "SELECT jsonb_agg(to_jsonb(d) ORDER BY d.id)::text FROM dossier_suivi d WHERE structure_id IS NOT NULL"))
                        .isEqualTo(institutional);
                assertThat(scalar(statement,
                        "SELECT jsonb_agg(to_jsonb(a) ORDER BY a.id)::text FROM affectation_patient a"))
                        .isEqualTo(assignments);
                assertThat(scalar(statement, "SELECT count(*) FROM patient p WHERE (SELECT count(*) FROM dossier_suivi d "
                        + "WHERE d.patient_id=p.id AND d.structure_id IS NULL AND d.statut='ACTIF') <> 1")).isEqualTo("0");
                assertThat(scalar(statement, "SELECT count(*) FROM dossier_suivi WHERE structure_id IS NULL AND statut='ACTIF'"))
                        .isEqualTo(scalar(statement, "SELECT count(*) FROM patient"));

                SQLException duplicate = assertThrows(SQLException.class, () -> statement.executeUpdate("""
                        INSERT INTO dossier_suivi
                            (id,version,date_creation,date_modification,patient_id,structure_id,date_ouverture,statut,date_derniere_mise_ajour)
                        SELECT gen_random_uuid(),0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,patient_id,NULL,CURRENT_DATE,'ACTIF',CURRENT_TIMESTAMP
                        FROM dossier_suivi WHERE structure_id IS NULL AND statut='ACTIF' LIMIT 1
                        """));
                assertThat(duplicate.getSQLState()).isEqualTo("23505");
                assertThat(duplicate.getMessage()).contains("uk_dossier_personnel_actif");
            }
        } finally {
            try (var connection = DriverManager.getConnection(url, user, password);
                 var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }

    private static String scalar(Statement statement, String query) throws SQLException {
        try (var result = statement.executeQuery(query)) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }
}
