package com.nourtech.ecareplus;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.DriverManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

/** Only creates and drops random test schemas; never cleans a supplied schema. */
@EnabledIfEnvironmentVariable(named="ECARE_TEST_POSTGRES_URL", matches="jdbc:postgresql:.*")
class PostgresMigrationTests {
    @Test void migreBaseVideEtBaseV14SansPerte() throws Exception {
        String url=System.getenv("ECARE_TEST_POSTGRES_URL"), user=System.getenv("ECARE_TEST_POSTGRES_USER"), password=System.getenv("ECARE_TEST_POSTGRES_PASSWORD");
        for (boolean upgrade: new boolean[]{false,true}) {
            String schema="ecare_test_"+UUID.randomUUID().toString().replace("-", "");
            var config=Flyway.configure().dataSource(url,user,password).schemas(schema).defaultSchema(schema).locations("classpath:db/migration");
            try {
                if(upgrade) {
                    config.target("14").load().migrate();
                    try(var connection=DriverManager.getConnection(url,user,password);var statement=connection.createStatement()) {
                        statement.execute("CREATE TABLE "+schema+".test_sentinel (id integer primary key, valeur varchar(50))");
                        statement.execute("INSERT INTO "+schema+".test_sentinel VALUES (1, 'preserver')");
                        statement.execute("SET search_path TO "+schema);
                        org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,new org.springframework.core.io.ClassPathResource("postgres/v14-fixture.sql"));
                    }
                }
                var flyway=Flyway.configure().dataSource(url,user,password).schemas(schema).defaultSchema(schema).locations("classpath:db/migration").load();
                flyway.migrate(); flyway.validate();
                assertThat(flyway.info().pending()).isEmpty();
                try(var connection=DriverManager.getConnection(url,user,password);var statement=connection.createStatement()) {
                    if(upgrade)try(var result=statement.executeQuery("SELECT valeur FROM "+schema+".test_sentinel WHERE id=1")){result.next();assertThat(result.getString(1)).isEqualTo("preserver");}
                    if(upgrade){
                        try(var resultat=statement.executeQuery("SELECT c.statut,c.version,c.quantite_prevue,c.medicament_dosage,c.fuseau_prevu,p.source_ref FROM "+schema+".confirmation_prise c CROSS JOIN "+schema+".mesure_sante p WHERE c.id='00000000-0000-0000-0000-000000000009'")){
                            assertThat(resultat.next()).isTrue();assertThat(resultat.getString(1)).isEqualTo("PRISE");assertThat(resultat.getLong(2)).isEqualTo(3);assertThat(resultat.getBigDecimal(3)).isEqualByComparingTo("1");assertThat(resultat.getString(4)).isEqualTo("10");assertThat(resultat.getString(5)).isEqualTo("UTC");assertThat(resultat.getString(6)).isEqualTo("fixture-source");}
                        try(var resultat=statement.executeQuery("SELECT count(*) FROM "+schema+".affectation_patient WHERE statut='ACTIVE' AND professionnel_principal=true")){resultat.next();assertThat(resultat.getInt(1)).isEqualTo(1);}
                        try(var resultat=statement.executeQuery("SELECT statut,date_initiale=date_prevue FROM "+schema+".confirmation_prise WHERE id='00000000-0000-0000-0000-000000000012'")){assertThat(resultat.next()).isTrue();assertThat(resultat.getString(1)).isEqualTo("REPORTEE");assertThat(resultat.getBoolean(2)).isTrue();}
                    }
                }
            } finally {
                try(var connection=DriverManager.getConnection(url,user,password);var statement=connection.createStatement()) {statement.execute("DROP SCHEMA IF EXISTS "+schema+" CASCADE");}
            }
        }
    }
}
