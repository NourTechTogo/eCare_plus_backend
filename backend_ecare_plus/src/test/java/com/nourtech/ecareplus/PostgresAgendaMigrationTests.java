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

/** Migrates committed legacy appointments and reminders in a disposable PostgreSQL schema. */
@EnabledIfEnvironmentVariable(named = "ECARE_TEST_POSTGRES_URL", matches = "jdbc:postgresql:.*")
class PostgresAgendaMigrationTests {
    @Test
    void migrationV33PreserveRendezVousEtRappelsHistoriquesSansCreerDAgendaPersonnel() throws Exception {
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
                    .locations("classpath:db/migration").target("32").load().migrate();
            try (var connection = DriverManager.getConnection(url, user, password);
                 var statement = connection.createStatement()) {
                statement.execute("SET search_path TO " + schema);
                statement.executeUpdate("""
                        INSERT INTO rendez_vous
                            (id,version,date_creation,date_modification,patient_id,motif,date_heure,lieu,type_rendez_vous,statut,commentaire)
                        VALUES ('00000000-0000-0000-0000-000000000033',9,'2026-01-02T03:04:05Z','2026-02-03T04:05:06Z',
                            '00000000-0000-0000-0000-000000000002','Consultation historique','2026-06-12T10:00:00Z',
                            'Centre historique','CONSULTATION','CONFIRME','Commentaire conserve')
                        """);
                statement.executeUpdate("""
                        INSERT INTO rappel
                            (id,version,date_creation,date_modification,patient_id,prise_medicament_id,type_rappel,titre,message,
                             date_heure_prevue,frequence,statut,canal,date_fin,derniere_execution,derniere_lecture)
                        VALUES ('00000000-0000-0000-0000-000000000034',7,'2026-01-02T03:04:05Z','2026-02-03T04:05:06Z',
                            '00000000-0000-0000-0000-000000000002',NULL,'RENDEZ_VOUS','Rappel historique','Message historique',
                            '2026-06-12T09:00:00Z','AUCUNE','PLANIFIE','PUSH',NULL,NULL,NULL)
                        """);
                String appointments = scalar(statement,
                        "SELECT jsonb_agg(to_jsonb(r) ORDER BY r.id)::text FROM rendez_vous r");
                String reminders = scalar(statement,
                        "SELECT jsonb_agg(to_jsonb(r) ORDER BY r.id)::text FROM rappel r");
                String appointmentCount = scalar(statement, "SELECT count(*) FROM rendez_vous");
                String reminderCount = scalar(statement, "SELECT count(*) FROM rappel");

                Flyway.configure().dataSource(url, user, password).schemas(schema).defaultSchema(schema)
                        .locations("classpath:db/migration").target("33").load().migrate();
                var latest = Flyway.configure().dataSource(url, user, password).schemas(schema).defaultSchema(schema)
                        .locations("classpath:db/migration").load();
                latest.migrate();
                latest.validate();
                assertThat(latest.info().pending()).isEmpty();

                // Compare every historical column, including IDs, versions, dates, content and status.
                assertThat(scalar(statement, """
                        SELECT jsonb_agg(to_jsonb(r) - ARRAY['personnel','fuseau','professionnel_nom','rappel_minutes','rappel_id']
                            ORDER BY r.id)::text FROM rendez_vous r
                        """)).isEqualTo(appointments);
                assertThat(scalar(statement, """
                        SELECT jsonb_agg(to_jsonb(r) - 'agenda_personnel_id' ORDER BY r.id)::text FROM rappel r
                        """)).isEqualTo(reminders);
                assertThat(scalar(statement, "SELECT count(*) FROM rendez_vous")).isEqualTo(appointmentCount);
                assertThat(scalar(statement, "SELECT count(*) FROM rappel")).isEqualTo(reminderCount);
                assertThat(scalar(statement, "SELECT count(*) FROM rendez_vous WHERE personnel IS DISTINCT FROM FALSE "
                        + "OR fuseau IS NOT NULL OR professionnel_nom IS NOT NULL OR rappel_minutes IS NOT NULL OR rappel_id IS NOT NULL"))
                        .isEqualTo("0");
                assertThat(scalar(statement, "SELECT count(*) FROM rappel WHERE agenda_personnel_id IS NOT NULL")).isEqualTo("0");
                assertThat(scalar(statement, "SELECT count(*) FROM operation_agenda_patient")).isEqualTo("0");
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
