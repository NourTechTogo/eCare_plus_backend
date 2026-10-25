package com.nourtech.ecareplus.portail.application;
import com.nourtech.ecareplus.identity.domain.entity.*;
import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import com.nourtech.ecareplus.identity.application.service.MotDePasseService;
import com.nourtech.ecareplus.identity.infrastructure.persistence.UtilisateurRepository;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import jakarta.persistence.EntityManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
/** Only enabled through explicit demo profile;
no default or logged credentials. */
@Component @Profile("demo")
public class DemoBootstrap implements ApplicationRunner  {
    private final EntityManager em;
    private final UtilisateurRepository users;
    private final MotDePasseService passwords;
    private final String password;
    public DemoBootstrap(EntityManager em,UtilisateurRepository users,MotDePasseService passwords,@Value("${app.demo.password:}") String password) {
        this.em=em;
        this.users=users;
        this.passwords=passwords;
        this.password=password;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        PortailService.validerPassword(password);
        // A restored demo may use renamed login addresses. Never recreate it.
        if(users.count()>0)return;
        var s=new StructureSante("Centre eCare Démonstration","CENTRE","Adresse de démonstration","Lomé","Maritime","+22890000000","centre@ecare.test","DEMO-ECARE-LOCAL");
        s.activer();
        em.persist(s);
        var a=new AdministrateurStructure("Administration","eCare",Sexe.NON_RENSEIGNE,null,"+22890000001","admin@ecare.test",passwords.hacher(password),"fr","Direction","STRUCTURE",s);
        a.activerCompte();
        em.persist(a);
        creerMedecin(s,"DEMO-MED-1","medecin@ecare.test","+22890000002","Référent");
        creerMedecin(s,"DEMO-MED-2","autre.medecin@ecare.test","+22890000003","Secondaire");
    }
    private void creerMedecin(StructureSante s,String numero,String email,String tel,String nom) {
        var m=new Medecin(numero,numero,"Médecin",nom,Sexe.NON_RENSEIGNE,null,tel,email,passwords.hacher(password),"fr","Médecine générale","Suivi",LocalDate.now(),s);
        m.activerCompte();
        em.persist(m);
    }
}

