# Mise en place du backend eCare+

## 1. Prérequis

Installer :

- Git ;
- Java 21 LTS ;
- Docker Desktop ;
- un IDE Java récent, par exemple IntelliJ IDEA ou VS Code avec les extensions Java.

Maven n'a pas besoin d'être installé globalement : le dépôt fournit Maven Wrapper.

Vérification sous PowerShell :

```powershell
java -version
.\mvnw.cmd -version
docker version
docker compose version
```

La commande Java doit afficher une version 21.

## 2. Récupérer et ouvrir le projet

```powershell
git clone https://github.com/Mimicode2025/eCare_plus_backend.git
cd eCare_plus_backend
```

Ouvrir ensuite le dossier qui contient `pom.xml`, et non son dossier parent.

## 3. Préparer la configuration locale

La base utilise des valeurs locales par défaut. Une clé JWT reste obligatoire. Pour préparer la configuration :

```powershell
Copy-Item .env.example .env
$env:APP_JWT_SECRET=[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Le fichier `.env` est ignoré par Git et lu par Docker Compose. Spring Boot ne le charge pas automatiquement : exporter les variables dans le terminal ou les définir dans l'IDE. Ne jamais y placer un secret destiné à la production.

Variables reconnues :

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/ecare_plus
DB_USERNAME=ecare_plus
DB_PASSWORD=ecare_plus
SERVER_PORT=8080
```

Pour définir temporairement une variable dans PowerShell :

```powershell
$env:DB_PASSWORD = "mot-de-passe-local"
```

Attention : Spring Boot ne charge pas automatiquement un fichier `.env` arbitraire. Docker Compose le lit pour ses interpolations ; pour l'application, les variables doivent être fournies par l'IDE, le terminal ou l'environnement de déploiement.

## 4. Démarrer PostgreSQL

Docker Desktop doit être ouvert et son moteur doit être prêt.

```powershell
docker compose up -d postgres
docker compose ps
```

La base locale créée est :

- base : `ecare_plus` ;
- utilisateur : `ecare_plus` ;
- mot de passe local : `ecare_plus` ;
- port : `5432`.

Ces identifiants sont volontairement locaux et ne doivent jamais être réutilisés en production.

Pour consulter les journaux :

```powershell
docker compose logs -f postgres
```

Pour arrêter les conteneurs sans effacer les données :

```powershell
docker compose stop
```

## 5. Lancer les tests

```powershell
.\mvnw.cmd test
```

Les tests utilisent une base H2 éphémère en mode PostgreSQL. Ils sont rapides, mais ils ne remplacent pas une validation contre un vrai PostgreSQL.

## 6. Lancer l'application

```powershell
.\mvnw.cmd spring-boot:run
```

Au démarrage :

1. Spring ouvre la connexion PostgreSQL.
2. Flyway applique les migrations absentes.
3. Hibernate vérifie que le schéma correspond aux entités.
4. Le serveur démarre sur le port configuré.

Un échec de validation Hibernate doit être corrigé par une nouvelle migration. Il ne faut pas remplacer `validate` par `update` pour contourner le problème.

## 7. Travailler avec les migrations

Les migrations sont dans :

```text
src/main/resources/db/migration/
```

Convention :

```text
V1__create_organization_domain.sql
V2__enforce_single_active_primary_assignment.sql
V3__add_health_measure.sql
```

Une migration appliquée ne doit plus être modifiée. Si une correction est nécessaire, créer la migration suivante.

## 8. Ajouter une fonctionnalité

Ordre recommandé :

1. Décrire le cas d'utilisation et ses droits d'accès.
2. Ajouter ou modifier le modèle métier.
3. Écrire les tests des invariants.
4. Ajouter la migration Flyway.
5. Ajouter le repository nécessaire.
6. Créer le service applicatif transactionnel.
7. Créer une commande applicative lorsque le cas d'utilisation reçoit plusieurs paramètres.
8. Créer les DTO de requête/réponse et leurs validations.
9. Créer un mapper explicite DTO/commande/entité-réponse.
10. Créer un contrôleur REST mince.
11. Ajouter les tests d'intégration et la documentation OpenAPI.

Ne pas exposer directement une entité JPA dans une réponse HTTP.

Organisation type d'un module :

```text
module/
├── api/controller
├── api/dto/request
├── api/dto/response
├── api/mapper
├── application/command
├── application/service
├── domain/entity
├── domain/enumeration
└── infrastructure/persistence
```

Après le démarrage, Swagger est disponible sur `http://localhost:8080/swagger-ui.html`. Les routes métier exigent un JWT et les droits détaillés dans le README. La récupération du mot de passe exige une configuration SMTP réelle.

## 9. Dépannage

### Docker n'est pas disponible

Message typique : connexion impossible au pipe `DockerDesktopLinuxEngine`.

Solution : démarrer Docker Desktop, attendre que le moteur soit prêt, puis relancer `docker compose up -d postgres`.

### Le port 5432 est déjà utilisé

Arrêter l'autre PostgreSQL ou modifier le port hôte dans `compose.yaml`, par exemple `5433:5432`, puis utiliser :

```dotenv
DB_URL=jdbc:postgresql://localhost:5433/ecare_plus
```

### Flyway signale une migration modifiée

Ne pas utiliser `repair` sans comprendre la cause. Restaurer le contenu original de la migration et ajouter une nouvelle migration corrective.

### Java n'est pas en version 21

Configurer `JAVA_HOME` vers un JDK 21 et rouvrir le terminal. L'IDE doit utiliser le même JDK pour Maven et le projet.

## 10. Avant un déploiement réel

Le projet n'est pas encore prêt pour recevoir des données médicales réelles. Il faut auparavant mettre en place :

- authentification et autorisations ;
- chiffrement et gestion des secrets ;
- politique de conservation ;
- sauvegardes testées ;
- journalisation inviolable ;
- supervision ;
- revue juridique, médicale et de cybersécurité ;
- environnement de préproduction ;
- tests PostgreSQL automatisés.
