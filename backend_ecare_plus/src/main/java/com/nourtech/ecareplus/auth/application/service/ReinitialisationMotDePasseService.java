package com.nourtech.ecareplus.auth.application.service;

import com.nourtech.ecareplus.auth.domain.entity.DemandeReinitialisation;
import com.nourtech.ecareplus.auth.infrastructure.persistence.DemandeReinitialisationRepository;
import com.nourtech.ecareplus.identity.application.service.MotDePasseService;
import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
import com.nourtech.ecareplus.identity.infrastructure.persistence.UtilisateurRepository;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.security.*; import java.time.*; import java.util.*;
import java.nio.charset.StandardCharsets;

@Service @Transactional
public class ReinitialisationMotDePasseService {
    private final UtilisateurRepository utilisateurs; private final DemandeReinitialisationRepository demandes;
    private final MotDePasseService motsDePasse; private final EnvoiOtp envoi; private final SecureRandom aleatoire = new SecureRandom();
    public ReinitialisationMotDePasseService(UtilisateurRepository u, DemandeReinitialisationRepository d, MotDePasseService m, EnvoiOtp e) {
        utilisateurs=u; demandes=d; motsDePasse=m; envoi=e;
    }
    public void demander(String email) {
        Optional<Utilisateur> utilisateur = utilisateurs.findByEmailIgnoreCase(email.trim());
        if (utilisateur.isEmpty()) return;
        demandes.findFirstByUtilisateurIdOrderByDateCreationDesc(utilisateur.get().getId()).ifPresent(DemandeReinitialisation::invalider);
        String code = "%04d".formatted(aleatoire.nextInt(10000));
        demandes.save(new DemandeReinitialisation(utilisateur.get(), motsDePasse.hacher(code), Instant.now().plus(Duration.ofMinutes(10))));
        // An SMTP failure rolls back creation and the invalidation of the previous request.
        envoi.envoyer(utilisateur.get().getEmail(), code);
    }
    @Transactional(noRollbackFor = CodeOtpInvalideException.class)
    public String verifier(String email, String code) {
        Utilisateur utilisateur = utilisateurs.findByEmailIgnoreCase(email.trim()).orElseThrow(this::invalide);
        DemandeReinitialisation demande = demandes.findFirstByUtilisateurIdOrderByDateCreationDesc(utilisateur.getId()).orElseThrow(this::invalide);
        if (!demande.active(Instant.now())) throw invalide();
        if (!motsDePasse.correspond(code, demande.getOtpHash())) { demande.echec(); throw invalide(); }
        byte[] secret = new byte[32];
        aleatoire.nextBytes(secret);
        String jeton = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        demande.verifier(empreinte(jeton)); return jeton;
    }
    public void reinitialiser(String jeton, String nouveau) {
        com.nourtech.ecareplus.portail.application.PortailService.validerPassword(nouveau);
        if (jeton == null || !jeton.matches("[A-Za-z0-9_-]{43}")) throw invalide();
        DemandeReinitialisation demande = demandes.findFirstByJetonHashAndUtiliseeFalse(empreinte(jeton)).orElseThrow(this::invalide);
        if (!demande.isVerifiee() || !demande.getExpireLe().isAfter(Instant.now())) throw invalide();
        demande.getUtilisateur().changerMotDePasse(motsDePasse.hacher(nouveau)); demande.utiliser();
    }
    private static String empreinte(String jeton) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(jeton.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private CodeOtpInvalideException invalide() { return new CodeOtpInvalideException(); }
    private static class CodeOtpInvalideException extends ConflitMetierException {
        CodeOtpInvalideException() { super("Code ou jeton invalide ou expiré"); }
    }
}
