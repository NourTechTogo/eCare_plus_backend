# Backend — préparer une consultation (synthèse JSON)

Lot local du 27 octobre 2026, sans changement mobile/web ni publication GitHub. Cette étape produit une synthèse JSON réelle et enregistrée. Elle ne génère pas de PDF et ne crée aucun fichier fictif.

## Accès et contrat

POST /api/v1/mobile/me/rapports-consultation : sourceRef UUID conservée lors d’un réessai, du et au (dates inclusives, 1 à 90 jours), fuseau IANA. La fin ne peut pas dépasser aujourd’hui dans ce fuseau. GET collection renvoie les métadonnées paginées (50 maximum) ; GET /{id} renvoie le contenu enregistré.

Le propriétaire vient exclusivement du JWT ; compte et consentement SUIVI_MEDICAL sont contrôlés à la génération et à la lecture. Aucun médecin affecté n’est requis. Le rapport est rattaché au dossier personnel ; une affectation clinique ne partage pas automatiquement ce rapport avec un professionnel ou un autre patient. Les autorisations des anciennes routes restent inchangées.

Même référence et même période/fuseau : même reçu et même contenu. Référence réutilisée avec une autre demande : 409. Une nouvelle référence crée une nouvelle photographie des données. Le rapport conserve les données collectées lors de sa génération, même si de nouvelles mesures arrivent ensuite.

## Données

- Identité minimale du patient, période, fuseau, date de génération et versionSchema=1.
- Mesures chronologiques dans les unités canoniques du serveur. Une série par type/composante/unité, avec nombre/minimum/maximum/moyenne arrondie à deux décimales et dernière valeur. La tension a des séries principale et diastolique distinctes ; les unités incompatibles ne sont pas mélangées.
- Jours sans saisie, pour l’ensemble et pour chaque série. Cela décrit une absence d’enregistrement et non un manquement médical.
- Traitements dont les dates recouvrent la période, avec configuration et statut courants au moment de la génération, distinction déclaration personnelle/prescription institutionnelle et horaires enregistrés. Les anciennes modifications de configuration ne sont pas reconstituées dans cette rubrique.
- Réponses aux prises déjà enregistrées (PRISE/OUBLIEE/REPORTEE), selon la date initialement prévue dans la période, y compris une réponse anticipée déjà saisie aujourd’hui. Aucune occurrence de prise n’est créée pour fabriquer un historique, et aucun taux d’observance n’est déduit des absences.
- Alertes avec la capture de règle disponible : date de la mesure associée, ou date de création pour une alerte sans mesure. Une capture ancienne inconnue reste null.
- Agenda personnel de la période avec son statut, y compris les annulations. Il ne vaut pas confirmation de réservation médicale.

Les avertissements figurent dans le JSON. Le rapport est descriptif : aucun diagnostic, nouveau seuil, interprétation clinique automatique ou recommandation thérapeutique.

## Persistance et bornes

V36 ajoute quatre colonnes facultatives à RapportSuivi : contenuSynthese, sourceRefSynthese, empreinteSynthese, fuseauSynthese ; contrainte de référence unique par dossier. Le contenu est inséré complet et n’est pas réécrit à la relecture. Les anciens rapports ne sont ni convertis ni fabriqués rétroactivement. JSON/GENERE signifie que cette synthèse existe ; cheminFichier reste null puisqu’aucun PDF n’est produit.

La collecte est limitée à 5000 lignes par ensemble et à 500 traitements. Un dépassement est rejeté avec invitation à réduire la période, sans troncature silencieuse. Le verrou patient sérialise les réessais et les écritures qui utilisent le même verrou ; il ne garantit pas un gel global de toutes les écritures professionnelles indépendantes pendant la collecte. Une saisie simultanée peut n’apparaître qu’au rapport suivant. Le contenu enregistré est ensuite figé.

## Validation

Vingt-deux tests ciblés H2/PostgreSQL réussis ; revue indépendante et vérification des corrections sans P1/P2 restant. Recette API réelle réussie : génération autonome, moyenne/unité canonique, contenu figé après nouvelle saisie, référence conflictuelle refusée, nouvelle synthèse actualisée et relecture identique après reconnexion. Validation complète finale : 294 tests réussis, aucun échec/erreur/ignoré ; `mvnw -o verify`, compilation Spring Boot et migration de préservation V35→V36 réussis le 27/10/2026 à 22:12:36 UTC. Résultats dans docs/VALIDATION.md. Le raccordement aux interfaces et l’export PDF relèvent de lots suivants.

Complément ultérieur : l'export PDF privé est maintenant implémenté dans la [phase 7](PHASE_7_BACKEND_EXPORT_PDF.md), depuis la synthèse enregistrée et sans modifier son contenu.
