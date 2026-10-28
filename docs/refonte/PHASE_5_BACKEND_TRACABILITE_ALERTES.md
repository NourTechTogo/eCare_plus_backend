# Backend — traçabilité des règles d’alerte

Lot local du 27 octobre 2026. Aucun changement mobile/web, aucun commit ni push. Aucun nouveau seuil médical.

## Correction

Les vues d’alerte consultaient les paramètres actuels de la règle. Modifier son seuil modifiait donc aussi l’explication affichée pour une ancienne alerte. Chaque nouvelle alerte liée à une règle conserve désormais une capture indépendante et immuable : version technique, nom/type, opérateur, seuils, composante, unité, contexte, occurrences/période d’observation, priorité, état actif, date/auteur/source de validation.

La capture est écrite à la création de l’alerte, puis ses colonnes sont non modifiables par JPA. Les champs historiques seuil, seuilMin, seuilMax, operateur et composante des vues portail/mobile sont lus depuis cette capture. Modifier puis revalider la règle produit une nouvelle version dans les alertes suivantes, sans changer les précédentes.

Les changements de paramètres, de contexte ou de portée d’une règle effacent désormais ensemble dateValidation, validateurId et sourceValidation, et désactivent la règle. Une nouvelle validation est nécessaire avant réactivation, comme auparavant.

## API

Les réponses d’alerte génériques et les vues d’alerte mobile/portail ajoutent regleAppliquee. Ce champ contient les paramètres ci-dessus ou null si aucune capture fiable n’existe. Les réponses RegleAlerteResponse ajoutent validateurId et sourceValidation. Les routes et leurs droits restent identiques.

sourceValidation=DEMONSTRATION identifie une règle fictive du catalogue local. MEDECIN signifie qu’une validation a été enregistrée par ce compte ; cette provenance technique ne prouve aucune validation clinique externe. Les métadonnées absentes restent nulles.

Une alerte MANUELLE peut être associée à une règle : sa capture conserve cette association, sans affirmer que la règle a déclenché automatiquement l’alerte. Le champ origine de l’alerte garde son sens.

## Migration V35

Seize colonnes facultatives sont ajoutées à alerte. Les anciennes lignes ne sont pas remplies à partir des règles actuelles : leurs paramètres historiques ne sont pas reconstructibles avec certitude. Le titre, la description, la priorité, les statuts et les liens sont conservés. Pour ces alertes, regleAppliquee et les seuils historiques inconnus sont nulls. Aucune donnée de mesure n’est réécrite et aucune nouvelle relation circulaire n’est créée.

## Validation et limites

Quatre tests RED ont reproduit les défauts avant correction, dont 100 attendu et 130 observé sur une alerte après modification. Vingt tests ciblés H2/PostgreSQL avec régression clinique ont passé, puis douze contrôles de préservation et de validation supplémentaires. Revue indépendante sans P1/P2 sur ce lot.

Recette API réelle réussie : ancienne alerte inchangée après modification de règle, date/auteur/source de validation réinitialisés, réactivation refusée sans validation, puis nouvelle alerte avec les nouveaux paramètres et version. Validation complète finale : 271 tests réussis, aucun échec/erreur/ignoré ; `mvnw -o verify` et compilation Spring Boot réussis le 27/10/2026 à 21:32:33 UTC. Un passage précédent a été interrompu extérieurement ; la validation finale a été rejouée après récupération de l’environnement. Authentification et recette reconfirmées sans changement de comptes. Résultats dans docs/VALIDATION.md. Ce lot n’introduit ni seuil de production, ni recommandations thérapeutiques, ni validation clinique externe. La présentation de ces métadonnées dans les interfaces reste un lot client.
