package com.nourtech.ecareplus.audit.infrastructure.aop;

import com.nourtech.ecareplus.audit.application.service.JournalisationService;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Principal;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Journalise les cas d'usage applicatifs sans polluer chaque service avec du
 * code technique. Les paramètres métier ne sont jamais sérialisés dans le
 * journal afin d'éviter d'y copier des données médicales sensibles.
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class JournalisationAspect {

    private static final Pattern FRONTIERE_MAJUSCULE = Pattern.compile("([a-z0-9])([A-Z])");
    private static final int TAILLE_APPAREIL = 300;
    private static final Map<String, String> RESSOURCES_PAR_SERVICE = Map.ofEntries(
            Map.entry("GestionAbonnementService", "ABONNEMENT_STRUCTURE"),
            Map.entry("SynchronisationService", "ELEMENT_SYNCHRONISATION"),
            Map.entry("GestionCompteService", "UTILISATEUR"),
            Map.entry("GestionConsentementService", "CONSENTEMENT"),
            Map.entry("GestionStructureService", "STRUCTURE_SANTE"),
            Map.entry("GestionProfessionnelSanteService", "PROFESSIONNEL_SANTE")
    );

    private final JournalisationService journalisationService;

    public JournalisationAspect(JournalisationService journalisationService) {
        this.journalisationService = journalisationService;
    }

    @Around("(execution(public * com.nourtech.ecareplus..application.service..*(..))"
            + " && !within(com.nourtech.ecareplus.audit..*)"
            + " && !within(com.nourtech.ecareplus.identity.application.service.MotDePasseService)"
            + " && !within(com.nourtech.ecareplus.identity.application.service.VerificationUniciteCompteService))"
            + " || @annotation(com.nourtech.ecareplus.audit.infrastructure.aop.JournaliserPortail)")
    public Object journaliser(ProceedingJoinPoint point) throws Throwable {
        ContexteRequete contexte = contexteRequete();
        JournaliserPortail facade = ((MethodSignature) point.getSignature()).getMethod().getAnnotation(JournaliserPortail.class);
        String ressourceParDefaut = facade == null ? ressourceDepuisService(point.getTarget().getClass()) : facade.value();
        UUID identifiantParDefaut = premierIdentifiant(point.getArgs());

        Object resultat;
        try {
            resultat = point.proceed();
        } catch (Throwable erreur) {
            String action = action(point, ressourceParDefaut);
            journalisationService.enregistrerEchec(
                    contexte.principal(), action, ressourceParDefaut, identifiantParDefaut,
                    contexte.adresseIp(), contexte.appareil()
            );
            throw erreur;
        }

        String ressource = ressourceDepuisResultat(resultat, ressourceParDefaut);
        UUID ressourceId = identifiantDepuisResultat(resultat, identifiantParDefaut);
        String action = action(point, ressource);
        enregistrerSucces(contexte, action, ressource, ressourceId);
        return resultat;
    }

    private void enregistrerSucces(
            ContexteRequete contexte,
            String action,
            String ressource,
            UUID ressourceId
    ) {
        Runnable ecriture = () -> journalisationService.enregistrerPourPrincipal(
                contexte.principal(), action, ressource, ressourceId,
                contexte.adresseIp(), contexte.appareil(), "SUCCES"
        );
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ecriture.run();
                }
            });
            return;
        }
        ecriture.run();
    }

    private static String action(ProceedingJoinPoint point, String ressource) {
        String methode = ((MethodSignature) point.getSignature()).getMethod().getName();
        String verbe = versConstante(methode);
        if ("ConfirmationPriseService".equals(ClassUtils.getUserClass(point.getTarget()).getSimpleName())) {
            if ("CONFIRMER".equals(verbe)) {
                return "CONFIRMER_PRISE_MEDICAMENT";
            }
            if ("SIGNALER_OUBLI".equals(verbe)) {
                return "SIGNALER_OUBLI_PRISE_MEDICAMENT";
            }
            if ("REPORTER".equals(verbe)) {
                return "REPORTER_PRISE_MEDICAMENT";
            }
        }
        return verbe + "_" + ressource;
    }

    private static String ressourceDepuisResultat(Object resultat, String valeurParDefaut) {
        if (resultat instanceof AbstractEntity) {
            return versConstante(ClassUtils.getUserClass(resultat).getSimpleName());
        }
        return valeurParDefaut;
    }

    private static UUID identifiantDepuisResultat(Object resultat, UUID valeurParDefaut) {
        if (resultat instanceof Map<?,?> valeurs && valeurs.get("id") instanceof UUID id) {
            return id;
        }
        return resultat instanceof AbstractEntity entite && entite.getId() != null
                ? entite.getId()
                : valeurParDefaut;
    }

    private static UUID premierIdentifiant(Object[] arguments) {
        for (Object argument : arguments) {
            if (argument instanceof UUID identifiant) {
                return identifiant;
            }
        }
        return null;
    }

    private static String ressourceDepuisService(Class<?> type) {
        String nom = ClassUtils.getUserClass(type).getSimpleName();
        String ressource = RESSOURCES_PAR_SERVICE.get(nom);
        if (ressource != null) {
            return ressource;
        }
        return versConstante(nom.replaceFirst("^Gestion", "").replaceFirst("Service$", ""));
    }

    private static String versConstante(String valeur) {
        return FRONTIERE_MAJUSCULE.matcher(valeur)
                .replaceAll("$1_$2")
                .toUpperCase(Locale.ROOT);
    }

    private static ContexteRequete contexteRequete() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributs)) {
            return ContexteRequete.vide();
        }
        HttpServletRequest requete = attributs.getRequest();
        Principal principal = requete.getUserPrincipal();
        return new ContexteRequete(
                principal == null ? null : principal.getName(),
                requete.getRemoteAddr(),
                tronquer(requete.getHeader("User-Agent"), TAILLE_APPAREIL)
        );
    }

    private static String tronquer(String valeur, int longueurMaximale) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        String nettoyee = valeur.trim();
        return nettoyee.length() <= longueurMaximale
                ? nettoyee
                : nettoyee.substring(0, longueurMaximale);
    }

    private record ContexteRequete(String principal, String adresseIp, String appareil) {

        private static ContexteRequete vide() {
            return new ContexteRequete(null, null, null);
        }
    }
}
