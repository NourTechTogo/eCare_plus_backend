# Backend — rappels de traitements

Travail local du 27 octobre 2026, après l’agenda personnel. Aucun changement des applications mobile/web et aucune publication GitHub.

## Comportement

Le serveur réutilise les révisions historiques du planning pour créer les occurrences, même sans consultation de l’application. La tâche `TraitementRappelWorker` parcourt au maximum 100 patients par passage, avec un curseur entre les passages. Intervalle configurable : `app.rappels-traitements.intervalle-ms` (60 secondes par défaut), sous le commutateur existant `app.traitements-planifies`.

Une notification privée est publiée pour une occurrence ATTENTE ou REPORTEE échue aujourd’hui dans son fuseau enregistré. Le dossier, le traitement, la prise, le compte et le consentement SUIVI_MEDICAL doivent être actifs. Aucune affectation médicale n’est nécessaire. Une fréquence libre non configurée ne génère pas de calendrier déduit.

L’identifiant de source contient l’occurrence et son échéance : les réessais/concurrences ne créent pas de doublons. Un report invalide l’ancien rappel et permet un nouveau rappel à la nouvelle échéance. PRIS et IGNORER masquent les notifications précédentes et annulent les envois encore persistés en attente, à réessayer ou réservés. Une modification de planning invalide également les notifications de cette prise. L’historique des notifications reste conservé en base par suppression logique.

La distribution recontrôle le propriétaire, le statut, l’échéance et la source avant de réserver un envoi. La notification ne révèle ni nom de médicament ni dose dans le texte envoyé au fournisseur. Elle ne constitue pas une recommandation d’ajuster la prise.

## Contrat API

Les notifications ont `typeNotification=TRAITEMENT`, `typeRessource=PRISE` et `ressourceId` égal à l’occurrence (confirmation_prise). `GET /api/v1/mobile/me/prises/{occurrenceId}` fournit la vue existante OccurrenceVue exclusivement à son patient propriétaire, avec contrôle du consentement. Les anciennes routes cliniques de confirmation, oubli et report appliquent aussi le verrou patient et l’invalidation. Une restauration utilisateur d’un rappel invalidé est refusée (409). Les routes de réponse restent inchangées : `POST /api/v1/mobile/me/prises/{occurrenceId}/reponse` avec action PRIS, IGNORER ou REPORTER, version et sourceRef.

Migration V34 : index des échéances sur confirmation_prise ; marqueur permanent date_invalidation sur notification pour distinguer un rappel obsolète d’une suppression utilisateur réversible ; aucune relation circulaire ni réécriture de l’historique.

## Limites

Ce lot prépare le backend. Le routage de la nouvelle cible PRISE dans mobile/web sera à raccorder dans un lot client. Aucun fournisseur push/WhatsApp réel n’est activé par ce travail ; une notification en boîte ne prouve pas une livraison sur téléphone. Un transport ayant déjà quitté le serveur ne peut pas être rappelé. Le serveur ne marque jamais automatiquement une prise comme PRISE ou OUBLIEE. Les jours précédents ne sont pas rattrapés par une rafale de notifications. Le débit est borné à 100 patients par passage ; la latence dépend donc du volume de patients.

## Validation

La recette sur l’API réelle locale a réussi : inscription autonome, déclaration fictive, génération par la tâche planifiée sans lecture du planning, réponse PRIS, disparition du rappel et refus de restauration (409), relecture de l’historique. Aucun terminal ni fournisseur externe utilisé pour cette recette.

La revue indépendante a identifié et fait couvrir trois chemins : anciennes réponses cliniques, restauration d’un rappel obsolète, puis suppression utilisateur avant invalidation. Validation complète finale : 239 tests réussis, aucun échec, aucune erreur ni test ignoré ; `mvnw -o verify` et compilation Spring Boot réussis le 27/10/2026 à 15:05:18 UTC, avec H2/PostgreSQL et migrations V1 à V34. Résultats détaillés dans docs/VALIDATION.md.
