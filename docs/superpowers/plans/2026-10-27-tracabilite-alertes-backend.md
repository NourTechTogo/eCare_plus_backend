# Traçabilité des alertes — plan backend local

Objectif : conserver les paramètres réellement associés à une alerte, sans inventer de seuils médicaux et sans changer les interfaces clientes. Conserver tous les travaux locaux précédents ; aucun commit/push.

Décisions : capturer un instantané immuable de la règle dans chaque nouvelle alerte liée à une règle ; utiliser cette capture pour les seuils affichés, avec version technique et origine de validation. Ne pas reconstituer rétroactivement les règles des alertes historiques. Réinitialiser date/auteur/source de validation lors des modifications de règle. Ajouter les champs aux DTO et aux vues portail/mobile existantes, sans nouveau droit d’accès.

- [x] Tests RED : ancien seuil stable après modification, métadonnées de validation effacées, démonstration identifiable et accès privés maintenus.
- [x] InstantaneRegleAlerte embarqué dans Alerte, DTO RegleAppliqueeVue, migration V35 additive et mappers ; ancien historique sans capture explicitement inconnu.
- [x] Tests H2/PostgreSQL, revue indépendante, suite complète et recette API locale.
- [x] Bilan, API et validation actualisés. Mobile/web inchangés.

Aucun seuil de production ni validation clinique externe n’est ajouté. La version est la version technique JPA de la règle au moment de la capture. Les anciennes alertes gardent leur lien à la règle, mais leurs anciens paramètres inconnus ne sont pas remplacés par les paramètres actuels.

Journal : RED (4 échecs, dont 100 attendu et 130 observé sur une alerte historique), puis GREEN ciblé H2/PostgreSQL (20 tests avec régression clinique). Revue indépendante sans P1/P2. Ajout des fixtures historiques sans capture et du contrôle PostgreSQL V34→V35.

- Préservation : 12 tests H2/PostgreSQL réussis, migration V34→V35 vérifiée sans réécriture de l’historique ni capture fabriquée. Recette API réelle réussie (3 contrôles). Une première fixture API incomplète a été refusée en 400 ; champs obligatoires corrigés dans le script fictif, sans assouplir la validation de production.

- Incident environnement : premier passage complet interrompu extérieurement vers 20:14 UTC lors du renouvellement des outils (Java/API et PostgreSQL arrêtés ; PostgreSQL signale terminaison 0x40010004). Aucun résultat complet revendiqué pour ce passage. Redémarrage avec récupération automatique PostgreSQL et sauvegarde API, puis nouvelle validation complète dans backend-full-final.log.

- Reprise environnement : récupération PostgreSQL réussie. L’API lancée pendant la recompilation refusait aussi les routes publiques ; après redémarrage une fois les classes compilées, authentification et recette complète ont repassé, sans changement des comptes ni mots de passe. La cause précise du chargement initial n’est pas revendiquée comme établie. Une copie diagnostique en lecture seule a uniquement confirmé que les comptes restaient présents et actifs, puis a été supprimée.

- GREEN final : 271 tests sans échec/erreur/ignoré, compilation Spring Boot et paquet produits, 27/10/2026 21:32:33 UTC. Recette API reconfirmée après reprise ; clients propres et HEAD backend inchangé, aucun commit/push.
