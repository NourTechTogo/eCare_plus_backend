package com.nourtech.ecareplus.auth.api.controller;
import com.nourtech.ecareplus.auth.infrastructure.security.AutorisationRessources;
import com.nourtech.ecareplus.auth.application.service.SessionService;
import com.nourtech.ecareplus.identity.application.service.MotDePasseService;
import com.nourtech.ecareplus.identity.domain.entity.*;
import com.nourtech.ecareplus.portail.application.PortailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
@RestController @RequestMapping("/api/v1") @Transactional
public class CompteController  {
    private final AutorisationRessources auth;
    private final SessionService sessions;
    private final MotDePasseService passwords;
    private final PortailService portail;
    public CompteController(AutorisationRessources a,SessionService s,MotDePasseService p,PortailService ps) {
        auth=a;
        sessions=s;
        passwords=p;
        portail=ps;
    }
    @GetMapping("/compte") public Object compte() {
        var u=auth.acteur();
        return u instanceof Patient p?portail.vue(p,true):vue(u);
    }
    @PatchMapping("/compte") public Object modifier(@RequestBody Map<String,Object> r) {
        var u=auth.acteur();
        if(u instanceof Patient p)return portail.modifierSoi(p.getId(),r);
        portail.identite(u,r);
        return vue(u);
    }
    @PostMapping("/compte/mot-de-passe") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void password(@Valid @RequestBody PasswordRequest r) {
        var u=auth.acteur();
        PortailService.validerPassword(r.nouveauMotDePasse());
        if(!passwords.correspond(r.motDePasseActuel(),u.getMotDePasseHash()))throw new AccessDeniedException("Mot de passe actuel incorrect");
        u.changerMotDePasse(passwords.hacher(r.nouveauMotDePasse()));
    }
    @PostMapping("/auth/deconnexion") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void logout(JwtAuthenticationToken t) {
        sessions.revoquer(t.getToken());
    }
    private Map<String,Object> vue(Utilisateur u) {
        Map<String,Object> r=new LinkedHashMap<>();
        r.put("id",u.getId());
        r.put("nom",u.getNom());
        r.put("prenoms",u.getPrenoms());
        r.put("email",u.getEmail());
        r.put("telephone",u.getTelephone());
        r.put("sexe",u.getSexe());
        r.put("dateNaissance",u.getDateNaissance());
        r.put("languePreferee",u.getLanguePreferee());
        if(u instanceof ProfessionnelSante p) {
            r.put("specialite",p.getSpecialite());
            r.put("structureId",p.getStructure().getId());
        }
        return r;
    }
    public record PasswordRequest(@NotBlank String motDePasseActuel,@NotBlank @Size(min=12,max=72) @Pattern(regexp="(?s)(?=.*[A-Za-z])(?=.*[0-9]).+") String nouveauMotDePasse) {
    }
}

