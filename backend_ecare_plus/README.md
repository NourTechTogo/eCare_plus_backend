# eCare+ Backend

> Backend T1–T13 puis intégration Flutter/React réalisés le 25 octobre 2026 : [bilan actuel et limites](../docs/integration-clients/BILAN.md), [contrats API](../docs/backend/API.md), [recette initiale](../docs/backend/RECETTE.md). La validation du 27 octobre 2026 compte 186 tests réussis, dont les tests PostgreSQL, sans test ignoré. La migration V28 ajoute la prescription atomique/idempotente ; V29 ajoute l’idempotence des messages. V30 ajoute l’archivage et la suppression logique des notifications. V31 ajoute la durée serveur des messages vocaux privés. V32 ajoute le dossier personnel sans structure ; voir [la phase 1](../docs/refonte/PHASE_1_AUTONOMIE_PATIENT.md). Les sections historiques ci-dessous décrivent le socle initial ; le bilan actuel précise les raccordements clients et ce qui attend encore les fournisseurs et les appareils.

Backend de la plateforme togolaise de télésuivi eCare+, centrée dans son premier périmètre sur le diabète et l'hypertension.

Le système aide les patients à conserver et transmettre leurs informations de suivi entre deux consultations. Il aide les professionnels et les structures de santé à organiser le suivi, sans produire de diagnostic automatique et sans remplacer une décision médicale.

## État actuel

Le socle « Utilisateurs, Structures et Organisation » ainsi que les premiers cas
d'usage de télésuivi sont en place :

- `Utilisateur`, classe mère abstraite ;
- `Patient` et `ProcheAidant` ;
- `ProfessionnelSante`, `Medecin` et `Infirmier` ;
- `AdministrateurStructure` et `StructureSante` ;
- `AffectationPatient` ;
- `DossierSuivi` et `ObjectifSuivi` ;
- `Consentement` et `AutorisationPartage` ;
- `JournalActivite` ;
- `ElementSynchronisation` ;
- `AbonnementStructure` ;
- repositories Spring Data associés ;
- services applicatifs pour les comptes, structures, affectations, dossiers, consentements, synchronisations, abonnements et audit ;
- commandes applicatives, DTO de requête/réponse et mappers explicites ;
- contrôleurs REST versionnés sous `/api/v1` ;
- erreurs HTTP homogènes au format `ProblemDetail` ;
- mots de passe reçus en clair uniquement à l'inscription puis hachés avec BCrypt (coût 12) ;
- pathologies, mesures de santé, traitements, médicaments et confirmations de prise ;
- alertes, règles d'alerte et interventions de suivi ;
- rendez-vous, rappels, notifications et contenus éducatifs ;
- journalisation transversale des lectures, créations, modifications et échecs
  pour tous les services applicatifs ;
- onze migrations PostgreSQL versionnées ;
- tests du domaine, mappings JPA, services et contrats HTTP.

L'authentification JWT, la récupération de compte par OTP, l'espace mobile et la messagerie sont implémentés. Les contrôles HTTP vérifient le compte actif, le rôle, la structure réelle du dossier, l'affectation et le consentement. Les écrans clients sont gérés séparément.

## Technologies

- Java 21 LTS ;
- Spring Boot 4.1.1 ;
- Spring MVC ;
- Spring Data JPA et Hibernate 7 ;
- PostgreSQL ;
- Flyway ;
- Maven Wrapper ;
- JUnit 6, AssertJ et H2 pour les tests ;
- OpenAPI/Swagger pour la future API.

## Architecture

Le projet est un monolithe modulaire organisé par domaine fonctionnel :

```text
com.nourtech.ecareplus
├── shared          # briques techniques partagées
├── identity        # utilisateurs et profils
├── organization    # structures et affectations
├── care            # dossiers, mesures, traitements et suivi clinique
├── alert           # règles, alertes et interventions
├── communication   # rappels, rendez-vous, notifications et contenus
├── consent         # consentements et autorisations
├── audit           # journal d'activité
├── sync            # idempotence des synchronisations mobiles
└── subscription    # abonnements des structures
```

Chaque module pourra contenir :

```text
module/
├── api/
│   ├── controller/         # adaptation HTTP, sans règle métier
│   ├── dto/request/        # données entrantes et validation Jakarta
│   ├── dto/response/       # contrat public, sans secret ni entité JPA
│   └── mapper/             # transformations explicites
├── domain/
│   ├── entity/             # entités JPA et règles propres à une entité
│   └── enumeration/        # états et types métier fermés
├── application/
│   ├── command/            # paramètres des cas d'utilisation
│   └── service/            # cas d'utilisation et transactions
└── infrastructure/
    └── persistence/        # repositories Spring Data
```

Les packages ne sont créés que lorsqu'ils contiennent une responsabilité réelle. Les mappers sont volontairement explicites : cela rend visible chaque champ exposé et évite de publier accidentellement un hash ou une association JPA.

Le modèle métier ne contient pas de setters publics génériques. Les changements passent par des méthodes explicites comme `retirer`, `activer`, `terminer` ou `cloturer` afin de protéger les invariants.

## Relations principales

```text
Utilisateur
├── Patient
├── ProcheAidant
├── AdministrateurStructure
└── ProfessionnelSante
    ├── Medecin
    └── Infirmier

StructureSante ── ProfessionnelSante
Patient + ProfessionnelSante + StructureSante ── AffectationPatient
Patient + StructureSante ── DossierSuivi
DossierSuivi + Medecin ── ObjectifSuivi
Patient ── Consentement ── AutorisationPartage ── ProcheAidant
StructureSante ── AbonnementStructure
Utilisateur ── JournalActivite / ElementSynchronisation
```

L'héritage utilisateur est persisté avec la stratégie JPA `JOINED`. Les données communes sont stockées dans `utilisateur`, tandis que chaque profil possède sa table spécialisée.

## Démarrage rapide

Pour construire le backend dans un conteneur et le connecter à PostgreSQL,
suivre le [guide de déploiement Docker](docs/DEPLOIEMENT_DOCKER.md).
Le `Dockerfile`, le `.dockerignore` et le fichier autonome
`compose.deploy.yaml` sont fournis à la racine de ce projet Maven.

Prérequis : Java 21, Docker Desktop démarré et Git.

```powershell
$env:APP_JWT_SECRET=[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
docker compose up -d postgres
.\mvnw.cmd spring-boot:run
```

Sous Linux ou macOS :

```bash
export APP_JWT_SECRET="$(openssl rand -base64 32)"
docker compose up -d postgres
./mvnw spring-boot:run
```

L'application écoute par défaut sur `http://localhost:8080`.

- documentation OpenAPI : `http://localhost:8080/v3/api-docs` ;
- interface Swagger : `http://localhost:8080/swagger-ui.html`.

Le guide détaillé se trouve dans [docs/MISE_EN_PLACE_BACKEND.md](docs/MISE_EN_PLACE_BACKEND.md).

## Configuration

Variables disponibles :

- `DB_URL` : URL JDBC, par défaut `jdbc:postgresql://localhost:5432/ecare_plus` ;
- `DB_USERNAME` : utilisateur PostgreSQL, par défaut `ecare_plus` ;
- `DB_PASSWORD` : mot de passe PostgreSQL local, par défaut `ecare_plus` ;
- `SERVER_PORT` : port HTTP, par défaut `8080`.
- `APP_JWT_SECRET` : clé aléatoire d'au moins 32 octets, obligatoire au démarrage ;
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS` : origines autorisées du client web ;
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` : SMTP pour les codes OTP.

Le fichier `.env` est lu par Docker Compose. Spring Boot ne le charge pas automatiquement : exporter les variables dans le terminal ou les définir dans la configuration de lancement de l'IDE.

Les valeurs du fichier `compose.yaml` sont uniquement prévues pour le développement. Les secrets d'un environnement réel doivent venir d'un gestionnaire de secrets et ne doivent jamais être commités.

Le profil `prod` exige explicitement les trois variables de base de données et désactive Swagger. Il s'active avec `SPRING_PROFILES_ACTIVE=prod`.

## Base de données

Flyway applique les migrations présentes dans `src/main/resources/db/migration` au démarrage.

Règles importantes :

- ne jamais modifier une migration déjà déployée ;
- ajouter une nouvelle migration `V2__description.sql`, puis `V3__...` ;
- conserver `spring.jpa.hibernate.ddl-auto=validate` hors tests ;
- utiliser H2 uniquement pour les tests rapides, jamais comme base de production.

La première migration impose notamment :

- un seul dossier actif par patient et structure ;
- des contraintes de cohérence sur les dates et valeurs ;
- des index pour les affectations, consentements, journaux et synchronisations ;
- une clé d'idempotence par utilisateur, appareil et identifiant local.

Les migrations V2 à V7 complètent ce socle avec l'unicité de l'affectation
principale, le suivi clinique, les traitements, les alertes, la communication et
l'idempotence des confirmations de prise. V8 distingue l'origine des alertes,
empêche le doublonnage d'un déclenchement automatique et structure la récurrence
des rappels. Une migration déjà intégrée ne doit jamais être réécrite : toute
nouvelle évolution du schéma utilise le prochain numéro disponible. V9 ajoute l'OTP,
V10 la messagerie et V11 l'index des jetons de réinitialisation. V11 invalide les
anciens jetons déjà vérifiés ; les utilisateurs concernés doivent demander un nouveau code.

## Alertes automatiques, alertes manuelles et rappels

Les trois notions ont des responsabilités distinctes :

- une alerte `AUTOMATIQUE` est créée dans la même transaction que la nouvelle
  `MesureSante` lorsqu'une `RegleAlerte` validée et active atteint son seuil et son
  nombre minimal d'occurrences ;
- une alerte `MANUELLE` est créée explicitement par `POST /api/v1/alertes` ;
- un `Rappel` est une notification planifiée par le client à une date précise,
  indépendante du moteur de seuils médicaux.

`periodeObservation` est exprimée en **minutes**. Une fenêtre de 24 heures doit
donc être envoyée sous la forme `1440`. Une règle modifiée redevient inactive et
doit être validée puis activée à nouveau avant d'être utilisée. Le couple
`regleId` + `mesureId` est idempotent : il ne peut produire qu'une seule alerte.

Le frontend pourra programmer ou modifier un rappel avec :

- `dateHeurePrevue`, au format ISO-8601 UTC (par exemple
  `2026-10-01T08:00:00Z`) ;
- `frequence` parmi `AUCUNE`, `QUOTIDIEN`, `HEBDOMADAIRE`, `MENSUELLE` ;
- `dateFin`, facultative ;
- `canal` selon les canaux exposés par l'API.

Les routes utiles au futur frontend et au futur worker de diffusion sont :

- `GET /api/v1/alertes/patients/{patientId}?statut=NOUVELLE` ;
- `GET /api/v1/rappels/patients/{patientId}?statut=PLANIFIE` ;
- `GET /api/v1/rappels/echeances?jusqua={instant}` ;
- `PATCH /api/v1/rappels/{rappelId}/envoi` après confirmation d'un envoi réussi ;
- `PATCH /api/v1/rappels/{rappelId}/echec` après échec du fournisseur.

Pour un rappel récurrent, la confirmation d'envoi conserve le statut `PLANIFIE`
et calcule l'échéance suivante. Après `dateFin`, il passe à `TERMINE`. Pour un
rappel sans récurrence, il passe à `ENVOYE`. Aucun SMS, e-mail ou push n'est
simulé : l'intégration d'un fournisseur devra appeler les routes de confirmation
uniquement après le résultat réel de la diffusion.

## API REST disponible

Principales routes du lot actuel :

- `POST /api/v1/patients` et `GET /api/v1/patients/{id}` ;
- `POST /api/v1/proches-aidants` et `GET /api/v1/proches-aidants/{id}` ;
- `POST /api/v1/professionnels-sante/medecins` ;
- `POST /api/v1/professionnels-sante/infirmiers` ;
- `POST /api/v1/administrateurs-structure` ;
- `POST /api/v1/structures`, puis activation ou suspension ;
- `POST /api/v1/affectations-patients`, puis terminaison ;
- `POST /api/v1/dossiers-suivi` et `POST /api/v1/dossiers-suivi/{id}/objectifs` ;
- `POST /api/v1/consentements`, retrait d'un consentement et création d'une autorisation de partage ;
- `POST /api/v1/synchronisations` ;
- `POST /api/v1/abonnements-structures`, puis activation ;
- `/api/v1/pathologies`, `/api/v1/mesures-sante` et `/api/v1/traitements` ;
- `/api/v1/medicaments`, `/api/v1/prises-medicament` et `/api/v1/rapports-suivi` ;
- `/api/v1/regles-alerte` et `/api/v1/alertes` ;
- `/api/v1/rendez-vous`, `/api/v1/rappels` et `/api/v1/notifications` ;
- `/api/v1/contenus-educatifs`, `/api/v1/declarations-symptomes` et
  `/api/v1/observations-suivi`.

Les créations répondent `201 Created` avec `Location`. Les validations répondent `400`, les ressources absentes `404` et les conflits métier `409`, avec un corps compatible RFC 9457 (`application/problem+json`).

Le journal d'activité est alimenté automatiquement autour des services
applicatifs. Une réussite n'est écrite qu'après le commit métier ; un échec est
conservé dans une transaction indépendante. L'adresse IP et le User-Agent sont
capturés lorsqu'un appel HTTP existe, sans enregistrer les paramètres ni les
données médicales. L'utilisateur n'est associé que si le serveur fournit un
principal authentifié correspondant à son UUID ou à son e-mail. Aucun endpoint
public de création du journal n'est exposé afin d'empêcher sa falsification par
un client.

## Tests et vérifications

```powershell
.\mvnw.cmd test
```

La suite vérifie :

- le démarrage du contexte Spring ;
- le chargement des repositories ;
- les mappings d'héritage JPA ;
- la persistence du graphe structure/patient/médecin ;
- le retrait d'un consentement ;
- les autorisations de partage ;
- les objectifs de suivi ;
- les limites d'abonnement ;
- le registre de synchronisation ;
- les mesures, traitements, confirmations de prise et règles d'alerte ;
- le déclenchement automatique et idempotent des alertes sur nouvelle mesure ;
- la distinction entre alertes automatiques et manuelles ;
- les rappels, rendez-vous, notifications et contenus éducatifs ;
- la replanification et la fin des rappels récurrents ;
- l'audit transversal des succès, consultations et échecs avec contexte HTTP ;
- les contrats REST (`201`, `400`, `404`, `409`) ;
- l'absence de mot de passe et de hash dans les réponses JSON.

Pour valider Flyway sur PostgreSQL, Docker Desktop doit être démarré avant de lancer l'application.

## Conventions de développement

- noms métier en français, vocabulaire technique Spring en anglais lorsqu'il est imposé par le framework ;
- une classe publique par fichier ;
- injection par constructeur ;
- transactions dans la couche application ;
- DTO distincts des entités JPA ;
- aucune entité JPA retournée directement par un contrôleur ;
- associations `LAZY` par défaut ;
- aucune donnée médicale sensible dans les logs ;
- mots de passe et codes PIN stockés uniquement sous forme de hash ;
- accès à une donnée médicale conditionné par la structure, l'affectation et le consentement applicable ;
- tests ajoutés avec chaque règle métier.

## Documentation du projet

- [Plan backend](docs/PLAN_BACKEND.md)
- [Mise en place locale](docs/MISE_EN_PLACE_BACKEND.md)
- [Décisions de modélisation](docs/DECISIONS_MODELE.md)

## Prochaine étape recommandée

Compléter les lectures paginées, préciser les catégories de consentement et préparer la diffusion réelle des rappels. La CI exécute la suite sur H2 et PostgreSQL 17, avec Flyway et validation Hibernate sur PostgreSQL.

## Règles d'accès actuelles

- Les routes `/api/v1/auth` sont publiques. Les autres routes exigent un JWT et un compte encore actif en base.
- Un professionnel accède aux données cliniques uniquement avec une affectation active et commencée, dans sa propre structure, et un consentement accordé et valide. Chaque référence du corps de requête et chaque ressource imbriquée sont vérifiées. Les listes d'alertes et de rappels excluent les dossiers des autres structures.
- Le modèle actuel représente le type de consentement par un texte libre : le contrôle clinique exige un consentement valide, sans interpréter ce texte comme une catégorie juridique. Une granularité par type reste à définir.
- Les administrateurs gèrent les professionnels, affectations et abonnements de leur structure. Ils ne peuvent pas lire les données cliniques ni créer un tenant indépendant. La première structure et son administrateur doivent être provisionnés par l'exploitant hors de l'API publique. Pour admettre un patient : créer son profil, l'affecter dans la structure, enregistrer son consentement puis ouvrir son dossier.
- Médicaments et règles d'alerte constituent des référentiels communs : lecture par les professionnels et administrateurs, écriture par les médecins. Les contenus éducatifs sont modifiables par leur auteur.
- Rendez-vous, symptômes et rappels sans prise associée sont rattachés au patient, sans structure dans le modèle ; leur accès suit l'affectation du professionnel au patient. Un rappel lié à une prise et une alerte liée à une mesure vérifient également la structure de leur dossier.
- Les identifiants d'auteur d'un objectif ou d'une observation doivent correspondre au professionnel connecté. Les opérations de synchronisation sont réservées à l'utilisateur connecté.
- Les routes mobiles déterminent le patient depuis le JWT. Un professionnel perd l'accès à la conversation après retrait du consentement, fin d'affectation ou suspension de son activité/structure. Le patient conserve la lecture de ses propres anciens messages ; tout nouvel envoi et toute ouverture exigent une affectation et un consentement valides.

## Vérifications PostgreSQL

Sur une base **de test dédiée**, la commande suivante applique les migrations réelles et exécute la suite avec validation du schéma :

```powershell
.\mvnw.cmd verify '-Dspring.datasource.url=jdbc:postgresql://localhost:5432/ecare_test' '-Dspring.datasource.username=ecare_test' '-Dspring.datasource.password=ecare_test' '-Dspring.flyway.enabled=true' '-Dspring.jpa.hibernate.ddl-auto=validate'
```

Les tests de contrats HTTP et d'audit isolent volontairement l'autorisation. `SecurityRegressionTests` utilise les filtres réels, les contrôles de ressources et des transactions indépendantes pour couvrir les refus et les parcours autorisés.
## Connexion avec l’application mobile

La clé `APP_JWT_SECRET` (au moins 32 octets) est obligatoire. En production, `DB_URL`, `DB_USERNAME` et `DB_PASSWORD` le sont également.
L’OTP exige également un serveur réel via `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` et `MAIL_FROM`; sans ces paramètres l’API retourne `503`.

```powershell
$env:APP_JWT_SECRET=[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
docker compose up -d postgres
.\mvnw.cmd spring-boot:run
```

L’API d’authentification est sous `/api/v1/auth`; les données appartenant au patient connecté sont sous `/api/v1/mobile` et sont toujours déterminées par le JWT.
