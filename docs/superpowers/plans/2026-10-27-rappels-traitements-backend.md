# Rappels de traitements — backend local

Périmètre : réutiliser le planning versionné existant, générer les occurrences sans ouverture du client, publier une seule notification privée par occurrence/échéance. Aucun changement mobile/web, commit ou push.

1. Tests de rappels autonomes, fuseaux, concurrence, réponses et arrêt du traitement.
2. Extraction de la matérialisation commune et tâche planifiée bornée par patient.
3. Notification privée PRISE, contrôle de validité avant distribution, invalidation après réponse/modification.
4. Vérifications ciblées puis suite complète H2/PostgreSQL ; documentation des limites.

Règles : seulement échéances du jour local, statut ATTENTE/REPORTEE, compte et consentement actifs, traitement/prise/dossier actifs ; aucune fréquence libre interprétée, aucun statut médical automatique. L’outbox est annulée après réponse mais un transport déjà parti ne peut être rappelé.

État : tâches 1 à 4 terminées et vérifiées. Base locale existante conservée, branche ecare_backend_autonomie_patient.

## Journal

- RED : six scénarios constatent l’absence du service de déclenchement automatique.
- Extraction de la matérialisation commune, sans accès HTTP supplémentaire à cette méthode interne.
- Les premières fixtures simulaient une échéance antérieure au consentement : correction de la date du consentement fictif pour tester réellement le rappel historique, sans assouplir les règles de production.
- RED : la lecture privée d’une occurrence retourne 404 avant création de la route.
- Deux erreurs de sérialisation Instant dans les fixtures HTTP corrigées avec des dates ISO, conformément aux requêtes clientes.
- Vérification H2/PostgreSQL en cours ; aucune modification des clients, aucun commit/push.

- Revue indépendante : deux P2 confirmés par tests RED (rappel clinique ancien encore visible ; restauration utilisateur réactivant un rappel obsolète).
- Correction : verrou/invalidation partagés sur les anciennes réponses cliniques, marqueur permanent date_invalidation distinct de la suppression utilisateur, également appliqué aux rappels d’agenda.

- Cas supplémentaire RED : suppression utilisateur avant réponse permettait encore la restauration (204 au lieu de 409). Correction du filtre d’invalidation : inclure les notifications déjà supprimées. Première validation complète interrompue volontairement pour intégrer ce cas ; seule la nouvelle validation complète finale fera référence.

- Recette API réelle réussie : création autonome, rappel automatique sans GET planning, réponse PRIS, restauration refusée et historique relu. Base sauvegardée avant redémarrage ; aucun fournisseur externe appelé.

- GREEN final : `mvnw -o verify`, 239 tests, zéro échec/erreur/ignoré, paquet Spring Boot produit, migrations PostgreSQL V1 à V34 et sauvegarde/restauration validées. Les deux clients restent propres, HEAD backend inchangé, aucun commit/push.
