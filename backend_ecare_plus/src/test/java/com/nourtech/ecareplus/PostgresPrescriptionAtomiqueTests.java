package com.nourtech.ecareplus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.*;
import java.sql.DriverManager;
import java.util.UUID;

@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("postgres-test")
@EnabledIfEnvironmentVariable(named="ECARE_TEST_POSTGRES_URL",matches="jdbc:postgresql:.*")
class PostgresPrescriptionAtomiqueTests extends PrescriptionAtomiqueTests {
    static final String SCHEMA="ecare_test_"+UUID.randomUUID().toString().replace("-","");
    @DynamicPropertySource static void schema(DynamicPropertyRegistry r){r.add("spring.flyway.schemas",()->SCHEMA);r.add("spring.flyway.default-schema",()->SCHEMA);r.add("spring.jpa.properties.hibernate.default_schema",()->SCHEMA);r.add("spring.datasource.hikari.connection-init-sql",()->"SET search_path TO "+SCHEMA);}
    @AfterAll static void nettoyer()throws Exception{try(var c=DriverManager.getConnection(System.getenv("ECARE_TEST_POSTGRES_URL"),System.getenv("ECARE_TEST_POSTGRES_USER"),System.getenv("ECARE_TEST_POSTGRES_PASSWORD"));var s=c.createStatement()){s.execute("DROP SCHEMA IF EXISTS "+SCHEMA+" CASCADE");}}
}
