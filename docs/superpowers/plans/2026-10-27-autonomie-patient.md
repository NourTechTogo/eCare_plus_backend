# Phase 1 — autonomie du patient

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Un patient inscrit sans médecin ni structure accède à son suivi personnel, saisit ses mesures et son traitement, retrouve son historique et ses alertes après reconnexion.

**Architecture:** Conserver les dossiers institutionnels et leurs affectations. Ajouter un dossier personnel sans structure, créé à l'inscription, unique par patient ; les comptes historiques obtiennent ce dossier à la demande lorsqu'aucun dossier institutionnel utilisable n'existe. L'accès personnel vient du JWT et du consentement de suivi ; il ne confère aucun droit professionnel supplémentaire.

**Tech Stack:** Spring Boot / JPA / Flyway / PostgreSQL, Flutter, API REST existante.

**Spec:** Phase 1 du texte joint par l'utilisateur le 27 octobre 2026, nouvelle orientation patient autonome. Ce plan applique uniquement cette phase : ni paiement, ni PDF, ni refonte visuelle, ni persistance hors ligne supplémentaire.

## Contraintes

- Préserver toutes les données et les routes professionnelles existantes.
- Ne pas créer de fausse structure ou de faux médecin pour contourner les contrôles.
- Les dossiers personnels ne deviennent pas lisibles aux professionnels par simple affectation ; le partage dédié reste une phase ultérieure.
- Les traitements personnels sont déclarés par le patient, sans modification d'une prescription professionnelle.
- Ne pas ajouter de seuil médical ; conserver la séparation entre règles de démonstration et validation médicale.

## Points de revue

- Accès croisé entre patients, identifiants fournis par le client et droits professionnels inchangés.
- Concurrence de création du dossier et réessais de mesures.
- Dossier personnel sans structure dans les règles, DTO, recherches et rapprochement QR ultérieur.
- Ancien patient affecté conservant son dossier, ses traitements et sa messagerie.
- Relance/reconnexion conservant les données serveur ; hors connexion durable exclu de cette phase.

## Tâches

- [x] 1. Branches dédiées, audit des dépendances et recette de référence backend/Flutter/web avant modifications.
- [x] 2. Écrire et constater l'échec des tests d'inscription autonome, dossier, mesures, historique, alertes, accès croisé et reconnexion.
- [x] 3. Modèle : `DossierSuivi`, migration V32, service de dossier personnel sous verrou patient, inscription atomique ; aucun déplacement des dossiers existants.
- [x] 4. Accès : `PortailService`, `AccesPatientService`, DTO/mappers ; consentement personnel conservé, structure facultative uniquement pour le dossier personnel ; accès professionnel fermé sur ce dossier.
- [x] 5. Sécuriser les hypothèses structurelles dans `AlerteService`, les recherches de planning et `ReferencementPatientService`. Tester les alertes personnelles sans notifier un médecin non autorisé.
- [x] 6. Déclaration de traitement personnel via route mobile dédiée, propriétaire issu du JWT, planning existant réutilisé ; Flutter ne bloque plus sur médecin absent.
- [x] 7. Tests ciblés puis suites complètes et migrations PostgreSQL ; analyse/compilation Flutter, non-régression web.
- [x] 8. Documenter fichiers, résultats réels, API et limites, vérifier les différences et livrer sur branches dédiées sans fusion dans main.

Décision de compatibilité : le dossier institutionnel actif reste prioritaire pour les patients affectés ; le dossier personnel est conservé séparément et utilisé sans affectation. Les anciennes fixtures sélectionnent désormais explicitement la structure pour leurs prescriptions. La recette complète puis la reprise ciblée HTTP sont détaillées dans docs/VALIDATION.md.
