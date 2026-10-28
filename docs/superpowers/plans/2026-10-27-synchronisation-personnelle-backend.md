# Synchronisation personnelle — plan backend

Objectif : compléter la fiabilité du suivi côté serveur sans modifier les clients ni publier sur GitHub. Conserver les mesures et réponses aux prises existantes.

Architecture : étendre le dispatch transactionnel existant ; réutiliser AgendaPersonnelService et DeclarationTraitementService, leurs autorisations, versions et reçus. Conserver une transaction par élément et les reçus de synchronisation persistants. Aucune nouvelle table ni migration requise.

Contrat : versionSchema=1, nouveaux types CREER_RENDEZ_VOUS, MODIFIER_RENDEZ_VOUS, ANNULER_RENDEZ_VOUS, DECLARER_TRAITEMENT. Contenu identique aux routes directes, sourceRef UUID obligatoire ; rendezVousId supplémentaire pour modifier/annuler. Préserver la même sourceRef entre un envoi direct et son réessai par lot. Le patient vient toujours du JWT. versionServeur concerne la ressource renvoyée (rendez-vous ou traitement).

- [x] Tests RED : création, modification/annulation, déclaration, rejeu après évolution, concurrence, isolement patient, erreurs partielles et corps nul.
- [x] Implémentation minimale : conversion et validation explicites des DTO, appel aux services existants ; même reçu en cas de réessai, conflit si contenu changé.
- [x] Vérifications ciblées H2/PostgreSQL et revue des chemins transactionnels.
- [x] Suite Maven complète, recette API locale, documentation et conservation locale des changements.

Limites : le backend ne rend pas persistante la file actuellement en mémoire sur téléphone. Les opérations dépendant de ressources créées hors ligne doivent être envoyées après récupération de leur identifiant/version serveur ; aucune résolution automatique des identifiants locaux n’est ajoutée.

Journal : autorisation de poursuivre le backend en local confirmée par la demande « passe au prochain ». Branche existante conservée ; aucun commit/push.

- RED observé : 9 tests, 8 échecs attendus (nouvelles opérations REJETE au lieu de SUCCES), aucune erreur ; le corps nul est déjà correctement traité par Spring MVC. Aucun changement inutile sur ce chemin.

- GREEN ciblé initial : 20 tests H2/PostgreSQL, aucun échec/erreur/ignoré.
- Revue indépendante : aucun autre P1/P2 ; confirmer la gestion des DateTimeException sur fuseaux et dates extrêmes.
- RED supplémentaire : fuseau invalide produit HTTP400 pour tout le lot au lieu de REJETE par élément ; correction de la frontière de l’adaptateur et ajout du cas Instant.MAX. Suppression de la dépendance au DTO imbriqué du contrôleur.

- Recette API réelle : quatre contrôles réussis (lot partiel autonome, déduplication direct/lot, modification/annulation, reconnexion/rejeu historique). API redémarrée après sauvegarde vérifiée ; aucun envoi externe.

- Première suite complète : 259 tests, un échec dans DistributionNotificationIntegrationTests.outboxPersistanteReessayeSansAnnoncerLivraison (ACCEPTE attendu, A_REESSAYER observé au deuxième passage). Les nouveaux tests H2/PostgreSQL passent. Test existant également signalé intermittent lors du lot agenda précédent.
- Relance isolée de ce test : réussie, sans changement de code ni assertion. Cause non établie ; aucun correctif de transport non justifié. Nouvelle suite complète de contrôle lancée ; journal distinct conservé localement.

- GREEN final : suite complète de recontrôle, 259 tests sans échec/erreur/ignoré, paquet Spring Boot produit, 27/10/2026 15:55:33 UTC. Limite du test de transport intermittent explicitement documentée. Les clients restent propres et HEAD backend inchangé ; aucun commit/push.
