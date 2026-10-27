package com.nourtech.ecareplus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.*;
import com.nourtech.ecareplus.care.domain.entity.*;
import java.time.*;
import java.sql.DriverManager;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@ActiveProfiles("postgres-test")
@EnabledIfEnvironmentVariable(named="ECARE_TEST_POSTGRES_URL",matches="jdbc:postgresql:.*")
class PostgresPrecisionTests extends BackendFixture {
    static final String SCHEMA="ecare_test_"+UUID.randomUUID().toString().replace("-","");
    @DynamicPropertySource static void schema(DynamicPropertyRegistry r){r.add("spring.flyway.schemas",()->SCHEMA);r.add("spring.flyway.default-schema",()->SCHEMA);r.add("spring.jpa.properties.hibernate.default_schema",()->SCHEMA);r.add("spring.datasource.hikari.connection-init-sql",()->"SET search_path TO "+SCHEMA);}
    @AfterAll static void nettoyer()throws Exception{try(var c=DriverManager.getConnection(System.getenv("ECARE_TEST_POSTGRES_URL"),System.getenv("ECARE_TEST_POSTGRES_USER"),System.getenv("ECARE_TEST_POSTGRES_PASSWORD"));var s=c.createStatement()){s.execute("DROP SCHEMA IF EXISTS "+SCHEMA+" CASCADE");}}
    @Test void horaireFractionnaireResteRejouableApresPersistance()throws Exception{
        var patient=patientId();LocalDate jour=LocalDate.now(ZoneOffset.UTC);UUID[] ids=tx.execute(s->{var d=em.createQuery("select d from DossierSuivi d where d.patient.id=:id and d.structure.id=:structure",DossierSuivi.class).setParameter("id",patient).setParameter("structure",structure).getSingleResult();var t=new Traitement(d,"Précision",null,jour,jour,"Médecin",null);em.persist(t);var m=new Medicament("Exemple","Exemple","comprimé","10","mg");em.persist(m);return new UUID[]{t.getId(),m.getId()};});
        mvc.perform(post("/api/v1/traitements/{id}/prises",ids[0]).header("Authorization",bearer(doctorToken)).contentType("application/json").content("{\"medicamentId\":\""+ids[1]+"\",\"quantite\":1,\"unite\":\"comprimé\",\"frequence\":\"QUOTIDIEN\",\"heurePrevue\":\"00:00:00.123456789\"}" )).andExpect(status().isCreated());
        var token=login();for(int i=0;i<2;i++)mvc.perform(get("/api/v1/mobile/me/planning-traitements").param("du",jour.toString()).param("au",jour.toString()).header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.occurrences.length()").value(1));
    }
}
