# Déployer le backend avec Docker

Exécuter les commandes depuis `backend_ecare_plus`, le dossier contenant
`pom.xml`, `Dockerfile` et `compose.deploy.yaml`. Utiliser Docker Desktop avec
le moteur Linux sous Windows, ou Docker Engine avec Compose sur le serveur.

## API et PostgreSQL sur la même machine

Le fichier `compose.deploy.yaml` est autonome. Il construit le backend, attend
que PostgreSQL soit prêt et relie les deux services sur le réseau Compose.
L'API utilise le nom `postgres` comme hôte de base de données. PostgreSQL ne
publie aucun port sur la machine ; ses données sont conservées dans un volume
distinct de celui du Compose de développement.

```powershell
Copy-Item .env.docker.example .env.docker
```

Renseigner dans `.env.docker` :

- `DB_PASSWORD` : un mot de passe aléatoire pour PostgreSQL ;
- `APP_JWT_SECRET` : une clé aléatoire d'au moins 32 octets ;
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS` : l'origine réelle du frontend ;
- les variables `MAIL_*` si la récupération de compte par OTP est utilisée.

Pour générer une clé avec PowerShell :

```powershell
$jwtBytes = New-Object byte[] 32
$jwtGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtGenerator.GetBytes($jwtBytes)
[Convert]::ToBase64String($jwtBytes)
$jwtGenerator.Dispose()
```

Copier le résultat dans `APP_JWT_SECRET`. Le fichier `.env.docker` est ignoré
par Git et exclu du contexte de construction Docker.

```powershell
docker compose --env-file .env.docker -f compose.deploy.yaml config --quiet
docker compose --env-file .env.docker -f compose.deploy.yaml up -d --build
docker compose --env-file .env.docker -f compose.deploy.yaml ps
docker compose --env-file .env.docker -f compose.deploy.yaml logs -f backend
```

L'API écoute sur `http://localhost:8080` par défaut. Le port publié se configure
avec `HTTP_PORT`. Le profil `prod` désactive Swagger. Les routes protégées
nécessitent un JWT. Flyway applique les migrations au démarrage.

Le Dockerfile compile et exécute les tests Maven dans une première étape Java
21, puis copie le JAR dans une image Java 21 JRE exécutée sans privilèges root.
Aucune installation locale de Java ou Maven n'est nécessaire pour ce build.
Le premier build nécessite un accès aux registres Docker et aux dépôts Maven.

Pour arrêter les services en conservant les données :

```powershell
docker compose --env-file .env.docker -f compose.deploy.yaml down
```

Ne pas ajouter `-v` si les données doivent être conservées. Modifier
`DB_PASSWORD` ne change pas le mot de passe d'une base déjà initialisée : une
rotation doit aussi être effectuée dans PostgreSQL.

## Hébergeur Docker avec base PostgreSQL externe

Configurer le dossier racine du service sur `backend_ecare_plus`, ou
`eCare_plus_backend/backend_ecare_plus` si le dépôt contient aussi ce dossier
parent, puis sélectionner `Dockerfile`.

```powershell
docker build -t ecare-plus-backend:local .
```

Fournir à l'exécution les variables `DB_URL` (URL JDBC PostgreSQL),
`DB_USERNAME`, `DB_PASSWORD`, `APP_JWT_SECRET` et
`APP_CORS_ALLOWED_ORIGIN_PATTERNS`. Le profil `prod` est celui de l'image.
Utiliser `SERVER_PORT` si l'hébergeur impose un port interne ; l'application
ne lit pas automatiquement une variable nommée `PORT`.

Pour une mise en ligne publique, configurer le domaine et HTTPS sur
l'hébergeur ou le reverse proxy, ainsi que les sauvegardes de PostgreSQL.
Ces fichiers préparent les conteneurs ; ils ne créent pas un hébergement.

Références : [construction en plusieurs étapes](https://docs.docker.com/build/building/multi-stage/)
et [attente d'un service prêt dans Compose](https://docs.docker.com/compose/how-tos/startup-order/).
