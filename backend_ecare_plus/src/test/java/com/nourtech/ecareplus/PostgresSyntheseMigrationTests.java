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
class PostgresSyntheseMigrationTests {
    @Test void v36NeTransformeNiNeReecritLesRapportsHistoriques()throws Exception{
        String url=System.getenv("ECARE_TEST_POSTGRES_URL"),user=System.getenv("ECARE_TEST_POSTGRES_USER"),password=System.getenv("ECARE_TEST_POSTGRES_PASSWORD");
        String schema="ecare_test_"+UUID.randomUUID().toString().replace("-","");
        try{
            Flyway.configure().dataSource(url,user,password).schemas(schema).defaultSchema(schema).locations("classpath:db/migration").target("14").load().migrate();
            try(var c=DriverManager.getConnection(url,user,password);var st=c.createStatement()){
                st.execute("SET search_path TO "+schema);ScriptUtils.executeSqlScript(c,new ClassPathResource("postgres/v14-fixture.sql"));
            }
            Flyway.configure().dataSource(url,user,password).schemas(schema).defaultSchema(schema).locations("classpath:db/migration").target("35").load().migrate();
            try(var c=DriverManager.getConnection(url,user,password);var st=c.createStatement()){
                st.execute("SET search_path TO "+schema);
                st.executeUpdate("INSERT INTO rapport_suivi(id,version,date_creation,date_modification,dossier_id,titre,date_debut,date_fin,resume,date_generation,format,statut) VALUES ('00000000-0000-0000-0000-000000000037',7,'2026-01-01T00:00:00Z','2026-01-02T00:00:00Z','00000000-0000-0000-0000-000000000005','Rapport historique','2026-01-01','2026-01-02','Contenu historique conserve','2026-01-02T08:00:00Z','PDF','EN_COURS')");
                String avant=scalaire(st,"SELECT jsonb_agg(to_jsonb(r) ORDER BY id)::text FROM rapport_suivi r");
                var flyway=Flyway.configure().dataSource(url,user,password).schemas(schema).defaultSchema(schema).locations("classpath:db/migration").load();flyway.migrate();flyway.validate();assertThat(flyway.info().pending()).isEmpty();
                assertThat(scalaire(st,"SELECT jsonb_agg(to_jsonb(r)-ARRAY['contenu_synthese','source_ref_synthese','empreinte_synthese','fuseau_synthese'] ORDER BY id)::text FROM rapport_suivi r")).isEqualTo(avant);
                assertThat(scalaire(st,"SELECT count(*) FROM rapport_suivi WHERE contenu_synthese IS NOT NULL OR source_ref_synthese IS NOT NULL OR empreinte_synthese IS NOT NULL OR fuseau_synthese IS NOT NULL")).isEqualTo("0");
            }
        }finally{try(var c=DriverManager.getConnection(url,user,password);var st=c.createStatement()){st.execute("DROP SCHEMA IF EXISTS "+schema+" CASCADE");}}
    }
    static String scalaire(Statement st,String sql)throws Exception{try(var rs=st.executeQuery(sql)){rs.next();return rs.getString(1);}}
}
