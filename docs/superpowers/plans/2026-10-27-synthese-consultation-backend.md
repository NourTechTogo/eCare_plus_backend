# Synthèse de consultation — plan backend local

Objectif : générer et relire un rapport personnel JSON enregistré, utilisable sans médecin affecté. Aucun PDF, interface cliente, commit ou push dans ce lot.

Réutiliser RapportSuivi et le dossier personnel avec quatre colonnes facultatives V36 (contenu, sourceRef, empreinte, fuseau). Le rapport JSON est réellement produit, statut GENERE, sans chemin de fichier inventé. Ne pas modifier les rapports historiques.

POST /api/v1/mobile/me/rapports-consultation : sourceRef UUID, du/au LocalDate inclusifs (1 à 90 jours, fin au plus aujourd’hui dans le fuseau IANA). GET collection : métadonnées paginées ; GET /{id} : synthèse figée, propriétaire et consentement vérifiés. Même référence/contenu : même rapport ; contenu différent : 409. Aucun partage professionnel implicite.

Contenu : identité minimale, période/fuseau/date de génération, statistiques descriptives par type/composante/unité canonique (nombre/min/max/moyenne/dernière valeur), points chronologiques, jours sans saisie, traitements enregistrés et configuration courante, réponses aux prises déjà enregistrées, alertes associées à la période avec capture de règle, agenda personnel avec statuts. Aucun diagnostic, seuil nouveau, pourcentage d’observance déduit ou création d’occurrences de prise pour fabriquer un historique. Avertissements explicites sur couverture et données manquantes.

Bornes : 5000 lignes par ensemble et 500 traitements, dépassement rejeté sans troncature silencieuse. Transactions et verrou patient pour les réessais ; pas de garantie de gel global des écritures professionnelles indépendantes pendant la collecte. Le contenu enregistré devient ensuite immuable.

- [x] Tests RED : statistiques/unités/composantes, fuseaux/bornes, absence de données, historique figé, réessais/conflits/concurrence, permissions/consentement, autres rubriques et limites de volume.
- [x] Modèle JSON/DTO, service personnel, contrôleur et V36 additive ; conserver les anciennes routes.
- [x] Revue indépendante, tests H2/PostgreSQL, préservation V35→V36, suite complète.
- [x] Recette API locale et documentation des limites ; garder les clients et HEAD inchangés.

- RED initial : routes absentes (404). Premier ciblé : statistiques/fuseaux/permissions passent, mais 6 échecs de persistance/reçu ont révélé un flush avant remplissage des colonnes insert-only. Correction : préparer tous les DTO, obtenir l’UUID sans flush, attacher le JSON complet, puis insérer/flush dans la même transaction.

- Complément de diagnostic : même sans flush explicite, Hibernate préparait l’état d’insertion au persist. Correction définitive : remplir les colonnes avant persist ; l’UUID de la ligne est ajouté à la vue API lors de la lecture, sans réécrire le contenu. Revue P2 : réponse anticipée omise, test RED (1 attendu, 0 observé), filtre temporel retiré uniquement pour les réponses déjà enregistrées de la période.

- GREEN ciblé : 22 tests H2/PostgreSQL, aucun échec/erreur/ignoré. Rubriques complètes, aucune création d’occurrences, limite 5001 mesures rejetée sans rapport partiel, réponse anticipée incluse. Revue indépendante P2 corrigée ; contrôle de préservation V35→V36 ajouté.

- Recette API réelle : 4 contrôles réussis, aucun PDF généré ni envoi externe ; API redémarrée après sauvegarde vérifiée une fois la compilation terminée. Revue des corrections sans P1/P2 restant.

- GREEN final : 294 tests, zéro échec/erreur/ignoré ; compilation/paquet Spring Boot produits, V36 et sauvegarde/restauration PostgreSQL validées, 27/10/2026 22:12:36 UTC. Recette API 4 contrôles réussis. Clients propres, HEAD inchangé, aucun commit/push.
