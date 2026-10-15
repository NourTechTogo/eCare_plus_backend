# Décisions de modélisation - lot organisation

## Héritage des utilisateurs

Décision : stratégie JPA `JOINED`.

Motif : les profils partagent une identité et des informations de compte, mais possèdent des attributs spécialisés. Cette stratégie respecte le diagramme sans produire une table unique contenant de nombreuses colonnes nulles.

## Patient et structure

Décision : la relation métier passe par `DossierSuivi`, qui associe un patient à une structure. La base garantit au maximum un dossier actif par couple patient/structure.

Motif : un patient peut être suivi dans plusieurs structures et son dossier doit rester contextualisé par structure.

## AffectationPatient

Décision : l'affectation référence explicitement le patient, le professionnel et la structure.

Motif : une simple relation patient-professionnel ne suffit pas dans un système multi-structure. La structure est nécessaire pour l'autorisation, l'audit et l'historique.

La règle « un seul professionnel principal actif par rôle » devra être appliquée dans un service transactionnel. Elle dépend des affectations existantes et ne peut pas être garantie correctement par une entité isolée.

## Consentement et autorisation

Décision : `AutorisationPartage` référence un `Consentement`, un patient et un proche aidant.

Une autorisation n'est valide que si :

- son propre état est `ACTIVE` ;
- sa période est valide ;
- le consentement associé est encore valide.

Le retrait du consentement invalide donc immédiatement l'accès, même avant le traitement asynchrone qui fera passer les autorisations à l'état `RETIREE`.

## ObjectifSuivi

Décision : l'objectif appartient à un `DossierSuivi` et conserve le `Medecin` qui l'a défini.

Le médecin doit appartenir à la même structure que le dossier. Une future délégation inter-structure devra être modélisée explicitement plutôt que de contourner cette règle.

## ElementSynchronisation

Décision : cette classe est conservée dans le backend avec un rôle limité.

Le stockage hors ligne et la file d'attente principale appartiennent à l'application mobile. Côté serveur, `ElementSynchronisation` sert à :

- reconnaître une requête déjà reçue ;
- empêcher les doublons ;
- connaître l'appareil et l'identifiant local ;
- suivre le nombre de tentatives ;
- conserver le résultat ou l'erreur technique.

La clé unique est `(utilisateur, appareil, identifiantLocal)`.

## AbonnementStructure

Décision : une structure possède plusieurs abonnements dans le temps.

Motif : conserver l'historique des formules, périodes, limites et montants. Un service applicatif devra empêcher plusieurs abonnements actifs qui se chevauchent.

## Audit

Décision : `JournalActivite` accepte un utilisateur absent pour représenter une action système, mais exige toujours une action, une ressource, une date et un résultat.

À terme, l'audit sensible devra utiliser un stockage append-only et une politique de conservation validée. La table actuelle constitue le modèle applicatif initial, pas une garantie d'inviolabilité réglementaire.

## Dates et heures

- `Instant` pour les événements techniques et les traces ;
- `LocalDate` pour les dates civiles sans heure ;
- conversion d'affichage selon le fuseau de l'utilisateur ;
- stockage JDBC configuré en UTC.

## Points encore à valider avec les métiers

- liste officielle des types de structure ;
- référentiel des spécialités et numéros professionnels au Togo ;
- catégories et versions de consentement ;
- données accessibles aux proches ;
- règles de conservation et d'effacement ;
- formules, devises et règles de facturation ;
- procédure de délégation inter-structure ;
- seuils médicaux et protocoles d'alerte.
