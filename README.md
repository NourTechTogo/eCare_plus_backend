# eCare+ — API commune

Branche `ecare_backend_stable`. Projet indépendant des dépôts mobile et web. Les sources Spring Boot et le Maven Wrapper se trouvent dans `backend_ecare_plus/`.

## Fonctionnalités livrées

Authentification JWT et sessions révocables, profils et consentements, affectation des patients, six types de mesures, seuils et alertes, documents privés, prescriptions atomiques et observance, notifications et boîte d'envoi, notes liées aux ressources, référencement temporaire, messagerie avec pièces jointes privées et réessais idempotents, cycle de vie des appels LiveKit et révocation des accès. Migrations Flyway V1–V30.

Consulter [les ajouts détaillés](AJOUTS_RECENTS.md), [le contrat API](docs/backend/API.md), [les appels](docs/backend/APPELS.md) et [la validation et les limites](docs/VALIDATION.md).

## Démarrer après clonage

Prérequis : Java 21 et PostgreSQL. Créer une base et un compte dédiés, puis injecter dans l'environnement du processus : `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `APP_JWT_SECRET` (secret aléatoire de longueur suffisante), `SERVER_PORT` (8081 pour les deux clients locaux), `APP_DOCUMENT_REPERTOIRE` (répertoire privé persistant). Le serveur applique les migrations automatiquement.

```powershell
cd backend_ecare_plus
./mvnw.cmd test
./mvnw.cmd spring-boot:run
```

Le fichier [config/external-services.env.example](config/external-services.env.example) décrit les paramètres externes sans clés. Spring Boot lit des variables d'environnement ; ce fichier n'est pas chargé automatiquement. Configurer SMTP pour la récupération réelle de compte. Configurer LiveKit et sa révocation avant d'activer les appels. Ne transmettre aucune clé serveur aux clients.

`scripts/demarrer-demo-locale.ps1` est réservé au workspace de démonstration déjà préparé : ses fichiers `.maintenance` privés ne sont pas livrés et ce script n'est pas un installateur pour un nouveau clone. Aucune base ni aucun mot de passe réel n'est inclus.
