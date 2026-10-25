package com.nourtech.ecareplus;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;

/** Logical JDBC data + private-file archive, restored into a separate migrated schema.
 * This exercises recovery of a frozen fixture, not a production pg_dump/PITR procedure. */
@EnabledIfEnvironmentVariable(named="ECARE_TEST_POSTGRES_URL",matches="jdbc:postgresql:.*")
class PostgresBackupRestoreTests {
    record Colonne(String nom,String type){}
    record TableCopie(String nom,List<Colonne> colonnes,List<List<String>> lignes,Set<String> dependances){}
    final ObjectMapper json=new ObjectMapper();
    @Test void restaureDonneesHistoriquesEtDocumentPriveDepuisArchives()throws Exception{
        String source="ecare_backup_src_"+UUID.randomUUID().toString().replace("-","");String destination="ecare_backup_dst_"+UUID.randomUUID().toString().replace("-","");
        String url=System.getenv("ECARE_TEST_POSTGRES_URL"),user=System.getenv("ECARE_TEST_POSTGRES_USER"),password=System.getenv("ECARE_TEST_POSTGRES_PASSWORD");
        Path dossier=Files.createTempDirectory(Path.of("target"),"restauration-");String cle=UUID.randomUUID()+".bin";
        try{
            flyway(url,user,password,source,"14").migrate();
            try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement()){s.execute("SET search_path TO "+source);ScriptUtils.executeSqlScript(c,new ClassPathResource("postgres/v14-fixture.sql"));}
            flyway(url,user,password,source,null).migrate();
            try(var c=DriverManager.getConnection(url,user,password);var s=c.prepareStatement("INSERT INTO "+source+".document_medical(id,version,date_creation,date_modification,patient_id,auteur_id,type_document,titre,date_document,nom_original,type_mime,taille_octets,cle_stockage,source_ref,empreinte,fichier_supprime) VALUES (?,0,now(),now(),'00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000002','AUTRE','Fixture','2026-01-01','fixture.pdf','application/pdf',?,?,'backup-fixture',?,false)")){
                s.setObject(1,UUID.randomUUID());s.setLong(2,DocumentMedicalIntegrationTests.PDF.length);s.setString(3,cle);s.setString(4,com.nourtech.ecareplus.shared.application.Empreintes.sha256("fixture"));s.executeUpdate();
            }
            List<TableCopie> sauvegarde;
            try(var c=DriverManager.getConnection(url,user,password)){c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);sauvegarde=lire(c,source);c.commit();}
            Path donnees=dossier.resolve("donnees.json");json.writeValue(donnees.toFile(),sauvegarde);
            Path fichiers=dossier.resolve("fichiers.zip");try(var zip=new ZipOutputStream(Files.newOutputStream(fichiers))){zip.putNextEntry(new ZipEntry(cle));zip.write(DocumentMedicalIntegrationTests.PDF);zip.closeEntry();}
            var archives=Arrays.asList(json.readValue(donnees.toFile(),TableCopie[].class));flyway(url,user,password,destination,null).migrate();
            try(var c=DriverManager.getConnection(url,user,password)){c.setAutoCommit(false);Set<String> effectuees=new HashSet<>();var restantes=new ArrayList<>(archives);
                while(!restantes.isEmpty()){
                    var admissibles=restantes.stream().filter(t->effectuees.containsAll(t.dependances())).toList();assertThat(admissibles).as("Tables restaurables selon leurs clés étrangères").isNotEmpty();
                    for(var table:admissibles){if(!table.lignes().isEmpty()){
                        String colonnes=table.colonnes().stream().map(v->identifiant(v.nom())).collect(java.util.stream.Collectors.joining(","));
                        String valeurs=table.colonnes().stream().map(v->{if(!v.type().matches("[a-zA-Z0-9_ ]+"))throw new IllegalStateException("Type SQL inattendu");return "CAST(? AS "+v.type()+")";}).collect(java.util.stream.Collectors.joining(","));
                        try(var insertion=c.prepareStatement("INSERT INTO "+identifiant(destination)+"."+identifiant(table.nom())+" ("+colonnes+") VALUES ("+valeurs+")")){for(var ligne:table.lignes()){for(int i=0;i<ligne.size();i++)insertion.setString(i+1,ligne.get(i));insertion.addBatch();}insertion.executeBatch();}
                    }effectuees.add(table.nom());restantes.remove(table);}
                }c.commit();
                var restaurees=lire(c,destination);for(var table:archives){var resultat=restaurees.stream().filter(t->t.nom().equals(table.nom())).findFirst().orElseThrow();assertThat(canonique(resultat.lignes())).as(table.nom()).isEqualTo(canonique(table.lignes()));}
            }
            Path fichiersRestaures=Files.createDirectory(dossier.resolve("prive-restaure"));try(var zip=new ZipFile(fichiers.toFile())){try(var flux=zip.getInputStream(zip.getEntry(cle))){Files.copy(flux,fichiersRestaures.resolve(cle));}}
            assertThat(Files.readAllBytes(fichiersRestaures.resolve(cle))).isEqualTo(DocumentMedicalIntegrationTests.PDF);
            try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement();var resultat=s.executeQuery("SELECT cle_stockage,taille_octets FROM "+destination+".document_medical")){assertThat(resultat.next()).isTrue();assertThat(resultat.getString(1)).isEqualTo(cle);assertThat(resultat.getLong(2)).isEqualTo(Files.size(fichiersRestaures.resolve(cle)));}
        }finally{try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement()){s.execute("DROP SCHEMA IF EXISTS "+source+" CASCADE");s.execute("DROP SCHEMA IF EXISTS "+destination+" CASCADE");}}
    }
    Flyway flyway(String url,String user,String password,String schema,String target){var config=Flyway.configure().dataSource(url,user,password).schemas(schema).defaultSchema(schema).locations("classpath:db/migration");if(target!=null)config.target(target);return config.load();}
    List<TableCopie> lire(Connection c,String schema)throws Exception{
        var tables=new ArrayList<TableCopie>();try(var meta=c.getMetaData().getTables(null,schema,"%",new String[]{"TABLE"})){while(meta.next()){
            String nom=meta.getString("TABLE_NAME");if(nom.equals("flyway_schema_history"))continue;Set<String> dependances=new HashSet<>();
            try(var fk=c.getMetaData().getImportedKeys(null,schema,nom)){while(fk.next())if(schema.equals(fk.getString("PKTABLE_SCHEM"))&&!nom.equals(fk.getString("PKTABLE_NAME")))dependances.add(fk.getString("PKTABLE_NAME"));}
            var colonnes=new ArrayList<Colonne>();var lignes=new ArrayList<List<String>>();try(var s=c.createStatement();var r=s.executeQuery("SELECT * FROM "+identifiant(schema)+"."+identifiant(nom))){var champs=r.getMetaData();for(int i=1;i<=champs.getColumnCount();i++)colonnes.add(new Colonne(champs.getColumnName(i),champs.getColumnTypeName(i)));while(r.next()){var ligne=new ArrayList<String>();for(int i=1;i<=champs.getColumnCount();i++)ligne.add(r.getString(i));lignes.add(ligne);}}
            tables.add(new TableCopie(nom,colonnes,lignes,dependances));
        }}return tables;
    }
    List<String> canonique(List<List<String>> lignes){return lignes.stream().map(l->{try{return json.writeValueAsString(l);}catch(Exception e){throw new IllegalStateException(e);}}).sorted().toList();}
    String identifiant(String nom){return "\""+nom.replace("\"","\"\"")+"\"";}
}
