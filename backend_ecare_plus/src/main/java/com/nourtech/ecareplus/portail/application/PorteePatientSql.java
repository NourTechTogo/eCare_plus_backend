package com.nourtech.ecareplus.portail.application;
import com.nourtech.ecareplus.organization.domain.entity.AffectationPatient;
import com.nourtech.ecareplus.organization.domain.enumeration.StatutAffectation;
import com.nourtech.ecareplus.consent.domain.entity.Consentement;
import com.nourtech.ecareplus.consent.domain.enumeration.StatutConsentement;
import jakarta.persistence.criteria.*;
import java.time.*;
import java.util.*;
/** SQL equivalent of assignment/consent visibility, applied before pagination. */
public final class PorteePatientSql {
    private PorteePatientSql(){}
    public static Predicate accessible(Expression<UUID> patient,CriteriaQuery<?> query,CriteriaBuilder cb,UUID structure,UUID professionnel,boolean clinique){
        LocalDate jour=LocalDate.now(ZoneOffset.UTC);Instant maintenant=Instant.now();
        var liens=query.subquery(UUID.class);var a=liens.from(AffectationPatient.class);
        var conditions=new ArrayList<Predicate>();conditions.add(cb.equal(a.get("patient").get("id"),patient));conditions.add(cb.equal(a.get("structure").get("id"),structure));conditions.add(cb.equal(a.get("statut"),StatutAffectation.ACTIVE));conditions.add(cb.lessThanOrEqualTo(a.get("dateDebut"),jour));conditions.add(cb.or(cb.isNull(a.get("dateFin")),cb.greaterThanOrEqualTo(a.get("dateFin"),jour)));
        if(professionnel!=null)conditions.add(cb.equal(a.get("professionnel").get("id"),professionnel));
        liens.select(a.get("id")).where(conditions.toArray(Predicate[]::new));Predicate resultat=cb.exists(liens);
        if(clinique){var accords=query.subquery(UUID.class);var c=accords.from(Consentement.class);accords.select(c.get("id")).where(cb.equal(c.get("patient").get("id"),patient),cb.equal(c.get("statut"),StatutConsentement.ACCORDE),cb.lessThanOrEqualTo(c.get("dateAcceptation"),maintenant),cb.or(cb.isNull(c.get("dateRetrait")),cb.greaterThan(c.get("dateRetrait"),maintenant)));resultat=cb.and(resultat,cb.exists(accords));}
        return resultat;
    }
}
