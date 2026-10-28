package com.nourtech.ecareplus;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.sql.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="ECARE_TEST_POSTGRES_URL",matches="jdbc:postgresql:.*")
class PostgresCaptureAlerteMigrationTests {
    @Test void migrationV35PreserveToutHistoriqueSansFabriquerDeCapture()throws Exception{
        String url=System.getenv("ECARE_TEST_POSTGRES_URL"),user=System.getenv("ECARE_TEST_POSTGRES_USER"),password=System.getenv("ECARE_TEST_POSTGRES_PASSWORD");
        String schema="ecare_test_"+UUID.randomUUID().toString().replace("-","");
        try{
            Flyway.configure().dataSource(url,user,password).schemas(schema).defaultSchema(schema).locations("classpath:db/migration").target("14").load().migrate();
            try(var c=DriverManager.getConnection(url,user,password);var st=c.createStatement()){
                st.execute("SET search_path TO "+schema);ScriptUtils.executeSqlScript(c,new ClassPathResource("postgres/v14-fixture.sql"));
            }
            Flyway.configure().dataSource(url,user,password).schemas(schema).defaultSchema(schema).locations("classpath:db/migration").target("34").load().migrate();
            try(var c=DriverManager.getConnection(url,user,password);var st=c.createStatement()){
                st.execute("SET search_path TO "+schema);
                st.executeUpdate("INSERT INTO regle_alerte(id,version,date_creation,date_modification,nom,type_mesure,operateur,seuil_min,nombre_occurrences,periode_observation,niveau_priorite,actif,composante,unite) VALUES ('00000000-0000-0000-0000-000000000035',8,now(),now(),'Regle actuelle fictive','GLYCEMIE','>=',130,1,60,'ELEVE',false,'PRINCIPALE','mg/dL')");
                st.executeUpdate("INSERT INTO alerte(id,version,date_creation,date_modification,patient_id,mesure_id,regle_id,type_alerte,origine,titre,description,niveau_priorite,statut) VALUES ('00000000-0000-0000-0000-000000000036',4,now(),now(),'00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000010','00000000-0000-0000-0000-000000000035','SEUIL_MESURE','AUTOMATIQUE','Alerte historique','Parametres historiques inconnus','ELEVE','NOUVELLE')");
                String avant=scalaire(st,"SELECT jsonb_agg(to_jsonb(a) ORDER BY id)::text FROM alerte a");
                String regles=scalaire(st,"SELECT jsonb_agg(to_jsonb(r) ORDER BY id)::text FROM regle_alerte r");
                var flyway=Flyway.configure().dataSource(url,user,password).schemas(schema).defaultSchema(schema).locations("classpath:db/migration").load();flyway.migrate();flyway.validate();
                assertThat(flyway.info().pending()).isEmpty();
                assertThat(scalaire(st,"SELECT jsonb_agg(to_jsonb(a)-ARRAY['capture_regle_version','capture_regle_nom','capture_regle_type_mesure','capture_regle_operateur','capture_regle_seuil_min','capture_regle_seuil_max','capture_regle_nombre_occurrences','capture_regle_periode_observation','capture_regle_niveau_priorite','capture_regle_composante','capture_regle_unite','capture_regle_contexte_mesure','capture_regle_date_validation','capture_regle_validateur_id','capture_regle_source_validation','capture_regle_actif'] ORDER BY id)::text FROM alerte a")).isEqualTo(avant);
                assertThat(scalaire(st,"SELECT jsonb_agg(to_jsonb(r) ORDER BY id)::text FROM regle_alerte r")).isEqualTo(regles);
                assertThat(scalaire(st,"SELECT count(*) FROM alerte WHERE capture_regle_version IS NOT NULL OR capture_regle_seuil_min IS NOT NULL OR capture_regle_source_validation IS NOT NULL")).isEqualTo("0");
            }
        }finally{try(var c=DriverManager.getConnection(url,user,password);var st=c.createStatement()){st.execute("DROP SCHEMA IF EXISTS "+schema+" CASCADE");}}
    }
    static String scalaire(Statement st,String sql)throws Exception{try(var rs=st.executeQuery(sql)){rs.next();return rs.getString(1);}}
}
