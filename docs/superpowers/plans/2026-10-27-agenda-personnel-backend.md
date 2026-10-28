# Fiabilité backend — agenda personnel et notifications

> **For agentic workers:** Use superpowers:executing-plans to implement task-by-task. No GitHub publication is authorized for this increment.

**Goal:** Persister les rendez-vous personnels et leurs rappels sans médecin obligatoire, en préservant les accès et rendez-vous professionnels existants.

**Architecture:** Étendre `RendezVous` avec une origine personnelle explicite et un rappel lié. Garder les endpoints professionnels et leur filtrage ; ajouter des routes personnelles dont le propriétaire vient du JWT. Les commandes sont sérialisées par patient et possèdent un reçu idempotent ; les rappels sont exécutés par les travaux planifiés existants et passent par la boîte privée puis la distribution déjà implémentée.

**Tech Stack:** Spring Boot, JPA, Flyway/PostgreSQL, H2 de test. Aucun changement Flutter/React et aucune nouvelle dépendance.

**Spec:** Suite backend de la phase de fiabilité dans le plan de refonte patient autonome ; demande du 27 octobre : continuer uniquement en local, sans envoi GitHub.

## Contraintes et décisions

- Conserver les rendez-vous et données existants. Les nouveaux événements personnels ne sont pas des réservations médicales confirmées.
- Le patient utilise ses propres identifiants JWT ; aucune affectation n'est exigée, les contrôles de consentement restent applicables.
- Une affectation ne rend pas automatiquement les événements personnels accessibles aux professionnels.
- Préserver le rappel dans la base, même si l'application est fermée. La livraison sur téléphone dépend encore d'un fournisseur configuré et d'un terminal autorisé ; ne pas annoncer une livraison externe réelle sans preuve.
- Fuseau IANA explicite et date absolue UTC pour éviter une interprétation locale implicite. Rappel facultatif, avance technique de 0 à 10080 minutes.
- Annulation logique et reçus de commandes : aucun effacement irréversible. Les notifications obsolètes en attente ne doivent pas être envoyées après modification/annulation. Une livraison déjà partie chez un fournisseur ne peut pas être rappelée.
- Ne pas développer ici la base hors ligne Flutter, le nouveau dashboard, les rapports, des paiements ou des seuils médicaux.

## Étapes

- [x] 1. Tests rouges du blocage résiduel des notifications personnelles sans affectation ; corriger uniquement le droit personnel de distribution.
- [x] 2. Tests rouges des routes d'agenda personnel : CRUD logique, propriété, validation, versions et réessais.
- [x] 3. Modèle et migration V33 : origine personnelle, fuseau, professionnel libre, rappel lié et reçu unique par patient/référence.
- [x] 4. Service/API personnels sous verrou patient, liste paginée et tri stable, protection des anciennes routes professionnelles et des rappels privés.
- [x] 5. Déclenchement idempotent des rappels échus et réévaluation de validité lors de la distribution. Annuler les envois et masquer les notifications rendues obsolètes.
- [x] 6. Tests ciblés puis suite backend/PostgreSQL complète, préservation des anciennes migrations et des accès professionnels ; revue indépendante.
- [x] 7. Documentation API, bilan et limites en local. Aucun push GitHub, aucun changement des clients.
