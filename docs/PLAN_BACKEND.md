# Plan de réalisation du backend eCare+

## Principes directeurs

Le backend doit rester un monolithe modulaire tant que la charge et l'organisation de l'équipe ne justifient pas des services séparés. Les frontières métier sont néanmoins explicites afin de permettre une extraction future sans réécriture complète.

Priorités transversales : confidentialité, traçabilité, fonctionnement avec une connexion intermittente, simplicité opérationnelle et absence de décision médicale automatique.

## Lot 0 - Socle technique

État : réalisé.

- Spring Boot 4.1.1 et Java 21 ;
- Maven Wrapper ;
- PostgreSQL et Docker Compose ;
- Flyway ;
- JPA/Hibernate ;
- profil de test H2 ;
- conventions de packages ;
- contrôle du schéma par `ddl-auto=validate`.

Critère de sortie : le contexte Spring démarre et la suite Maven passe.

## Lot 1 - Utilisateurs, structures et organisation

État : modèle, persistence, services applicatifs et première API REST réalisés ; sécurisation et lectures avancées à poursuivre.

Périmètre :

- utilisateurs et héritage des profils ;
- structures de santé ;
- rattachement des professionnels ;
- affectation patient/professionnel/structure ;
- dossier actif par patient et structure ;
- objectifs définis par un médecin ;
- consentements et partage avec un proche ;
- journalisation ;
- abonnement d'une structure ;
- registre serveur des synchronisations mobiles.

Réalisé dans l'API initiale :

1. DTO HTTP d'entrée et de sortie, distincts des commandes applicatives.
2. Mappers explicites par module.
3. API REST versionnée sous `/api/v1`.
4. Gestion homogène des erreurs avec `ProblemDetail`.
5. Tests d'intégration HTTP sur H2.

Travail restant :

1. Compléments du lot 2 : rotation des sessions, limitation des demandes OTP et granularité des consentements.
2. Pagination, filtres et endpoints de consultation complémentaires.
3. Documentation OpenAPI enrichie avec exemples et descriptions métier.
4. Maintenir les tests automatiques des migrations PostgreSQL en CI.

Critère de sortie : une structure peut inscrire un patient, enregistrer son consentement, ouvrir un dossier et l'affecter à un professionnel dans une seule transaction cohérente.

## Lot 2 - Authentification et autorisations

État : JWT, connexion e-mail/téléphone, BCrypt coût 12, OTP, contrôle de structure/affectation/consentement et révocation des comptes suspendus implémentés. Les points suivants représentent la cible complète, pas une liste de fonctions déjà livrées.

Périmètre :

- Spring Security ;
- authentification par téléphone ou e-mail ;
- hash Argon2id des mots de passe et PIN ;
- jetons courts et rotation des jetons de renouvellement ;
- verrouillage après tentatives anormales ;
- récupération de compte ;
- rôles et permissions ;
- isolation des données par structure ;
- révocation immédiate des partages ;
- journalisation des accès sensibles.

L'authentification est actuellement gérée par le backend. Une migration vers un fournisseur externe ou vers Argon2id nécessitera une décision distincte.

Critère de sortie : les tests démontrent qu'un utilisateur ne peut jamais consulter les données d'une autre structure sans affectation et autorisation explicites.

## Lot 3 - Mesures et fonctionnement hors ligne

Périmètre :

- `MesureSante` ;
- tension, glycémie, poids et fréquence cardiaque ;
- validation des unités et plages plausibles ;
- correction avec conservation de la trace ;
- endpoint de synchronisation par lots ;
- idempotence ;
- détection et résolution explicite des conflits ;
- réponses compactes adaptées aux réseaux mobiles.

`ElementSynchronisation` ne remplace pas la file locale de l'application mobile. Il enregistre les lots reçus et leurs résultats pour éviter les doublons et permettre un diagnostic technique.

Critère de sortie : renvoyer trois fois la même mesure avec la même clé locale ne crée qu'une seule donnée médicale.

## Lot 4 - Traitements, rappels et rendez-vous

Périmètre :

- traitements et médicaments ;
- prises planifiées ;
- rappels ;
- confirmations déclaratives de prise ;
- rendez-vous ;
- canaux push, SMS et e-mail ;
- planification robuste et reprises après erreur.

Critère de sortie : un rappel est traçable de sa création jusqu'à sa livraison ou son échec, sans prétendre prouver médicalement la prise d'un médicament.

## Lot 5 - Alertes et interventions

Périmètre :

- règles d'alerte validées par un médecin ;
- alertes sur mesure et absence de mesure ;
- priorité ;
- affectation à l'infirmier ;
- escalade vers le médecin ;
- intervention de suivi ;
- délais attendus par structure ;
- tableau de bord des alertes non traitées.

Les seuils doivent être validés par les partenaires médicaux avant utilisation réelle. Une alerte attire l'attention ; elle ne constitue pas un diagnostic.

Critère de sortie : toute modification d'état d'une alerte est attribuée, datée et auditée.

## Lot 6 - Rapports, contenus et pilotage

Périmètre :

- observations ;
- rapports PDF ;
- contenus éducatifs validés ;
- statistiques opérationnelles ;
- indicateurs du pilote ;
- exports contrôlés ;
- gestion des abonnements.

Critère de sortie : les indicateurs sont calculés à partir de définitions versionnées et les exports respectent les droits d'accès.

## Lot 7 - Préparation du pilote

- tests de charge ciblés ;
- audit de sécurité ;
- sauvegarde et restauration testées ;
- supervision et alertes techniques ;
- procédures d'incident ;
- politique de conservation ;
- revue juridique et médicale ;
- formation des structures pilotes ;
- environnement de préproduction ;
- stratégie de déploiement et de retour arrière.

Critère de sortie : un exercice de restauration, une revue des droits et un scénario d'incident ont été exécutés et documentés.

## Définition de « terminé » pour une fonctionnalité

Une fonctionnalité n'est terminée que si :`w23
- les règles métier sont écrites et testées ;
- les migrations sont versionnées ;
- l'API est documentée ;
- les erreurs sont prévisibles ;
- les accès sont autorisés explicitement ;
- les actions sensibles sont auditées ;
- aucune donnée sensible n'apparaît dans les logs ;
- les métriques utiles existent ;
- la documentation d'exploitation est à jour.
