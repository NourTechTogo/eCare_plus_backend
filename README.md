# eCare+ — API commune

Branche de publication : `ecare_backend_autonomie_rapports`, sans fusion dans main. Agenda, rappels, synchronisation, alertes capturées et rapports JSON/PDF : [contenu et validation](docs/PUBLICATION_2026-10-28.md). Vérification complète : 303 tests réussis le 28 octobre 2026.

Export PDF personnel de consultation ajouté en local : téléchargement privé depuis la synthèse figée, graphiques et annexe. [Contrat et limites](docs/refonte/PHASE_7_BACKEND_EXPORT_PDF.md).

Travaux backend locaux : agenda personnel persistant et rappels serveur sans médecin obligatoire. [Contrat, recette et limites](docs/refonte/PHASE_2_BACKEND_AGENDA.md). Aucune publication GitHub de cet incrément.

Phase 1 : le suivi patient fonctionne sans médecin ni structure obligatoires. [Bilan de l’autonomie patient](docs/refonte/PHASE_1_AUTONOMIE_PATIENT.md).

Messages vocaux privés disponibles : enregistrement, brouillon, envoi explicite et lecture avec progression, via le backend commun. Voir [les ajouts récents](AJOUTS_RECENTS.md) et [la validation du 26 octobre 2026](docs/VALIDATION.md).

Branche de publication `ecare_backend_autonomie_rapports`, sans fusion dans main. Projet indépendant des dépôts mobile et web. Les sources Spring Boot et le Maven Wrapper se trouvent dans `backend_ecare_plus/`.

## Fonctionnalités livrées

Authentification JWT et sessions révocables, profils et consentements, affectation des patients, six types de mesures, seuils et alertes, documents privés, prescriptions atomiques et observance, notifications et boîte d'envoi, notes liées aux ressources, référencement temporaire, messagerie avec pièces jointes privées et réessais idempotents, cycle de vie des appels LiveKit et révocation des accès. Migrations Flyway V1–V36.

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


### Suite backend locale — rappels de traitements

La tâche serveur matérialise le planning versionné et publie les rappels privés de prises du jour. La migration V34 ajoute un index d’échéance et un marqueur d’invalidation permanente des notifications obsolètes. Les clients ne sont pas modifiés dans ce lot et les envois externes nécessitent toujours un fournisseur configuré. Consulter le bilan `docs/refonte/PHASE_3_BACKEND_RAPPELS_TRAITEMENTS.md` à la racine du dépôt et `docs/VALIDATION.md` pour les résultats.


### Synchronisation personnelle — suite backend locale

Les lots de synchronisation acceptent aussi les créations/modifications/annulations de rendez-vous personnels et les déclarations de traitements, en réutilisant les références et versions des API directes. Le schéma reste en V34 ; la file persistante sur téléphone relève d’un lot mobile distinct. [Contrat et bilan](docs/refonte/PHASE_4_BACKEND_SYNCHRONISATION.md).


### Historique des règles d’alerte — backend local

V35 ajoute la capture immuable des paramètres et de la provenance de validation pour les nouvelles alertes. Les anciennes alertes sans capture gardent leurs paramètres historiques comme inconnus ; aucun seuil médical nouveau. [Bilan et limites](docs/refonte/PHASE_5_BACKEND_TRACABILITE_ALERTES.md).


### Préparation de consultation — backend local

V36 ajoute les synthèses personnelles JSON enregistrées, relues à l’identique et réservées au propriétaire : statistiques descriptives, traitements/réponses enregistrées, alertes capturées et agenda personnel. Aucun PDF ni diagnostic automatique dans ce lot. [Contrat et limites](docs/refonte/PHASE_6_BACKEND_SYNTHESE_CONSULTATION.md).
