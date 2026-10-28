# Backend — synchronisation du suivi personnel

Lot local du 27 octobre 2026. Aucun changement mobile/web, aucun commit ni push. Les travaux précédents sur l’agenda et les rappels sont conservés.

## Contrat et comportement

`POST /api/v1/mobile/me/synchronisations` conserve son enveloppe versionSchema=1, un identifiantAppareil stable et 1 à 100 éléments. Les types CREER_MESURE et REPONDRE_PRISE sont conservés. Quatre types sont ajoutés :

| typeOperation | contenu | ressourceId / versionServeur |
| --- | --- | --- |
| CREER_RENDEZ_VOUS | AgendaPersonnelRequest, identique au POST direct | Rendez-vous / version du rendez-vous |
| MODIFIER_RENDEZ_VOUS | AgendaPersonnelRequest avec rendezVousId et version courante | Rendez-vous / version après modification |
| ANNULER_RENDEZ_VOUS | rendezVousId, sourceRef UUID, version courante | Rendez-vous / version après annulation logique |
| DECLARER_TRAITEMENT | DeclarationTraitementRequest, identique au POST direct | Traitement / version du traitement (pas de la prise) |

Pour ces nouveaux types, conserver la même sourceRef UUID métier dans contenu entre envoi direct et envoi par lot. Le patient est imposé par le JWT. Une affectation à un médecin n’est pas requise. Modifier/annuler un agenda d’un autre patient est refusé ; ce lot ne crée pas de réservation médicale et ne transforme pas une déclaration en prescription.

Le couple identifiant local/type, associé au compte et à l’appareil, garde son reçu de synchronisation. Un réessai identique retourne le reçu initial, même si la ressource a évolué ensuite. Changer le contenu après acceptation donne CONFLIT. Les services métier ajoutent leur propre déduplication sourceRef : réessayer une commande directe par lot ou une commande de lot en direct ne crée pas un second objet.

Chaque élément conserve sa transaction : succès SUCCES, conflit de version/référence CONFLIT, données invalides ou ressource inaccessible REJETE. Un échec ne laisse ni traitement partiel ni reçu de succès et n’annule pas les éléments valides. Les dates ou fuseaux métier invalides dans contenu sont normalisés en erreur de l’élément ; le reste du lot est traité. Aucun corps de données médicales n’est ajouté aux erreurs de conversion du nouvel adaptateur.

## Exemple

```json
{
  "identifiantAppareil": "installation-stable",
  "elements": [{
    "identifiantLocal": "operation-locale-stable",
    "typeOperation": "CREER_RENDEZ_VOUS",
    "versionSchema": 1,
    "dateCreationLocale": "2026-10-27T10:00:00Z",
    "contenu": {
      "sourceRef": "e304a6b9-d947-4e31-aaeb-423b5fd27ad9",
      "motif": "Agenda personnel",
      "dateHeure": "2026-11-01T10:00:00Z",
      "fuseau": "Africa/Lome",
      "rappelMinutes": 30
    }
  }]
}
```

Les dates sont illustratives. Conserver aussi dateCreationLocale lors d’un réessai : elle fait partie de l’empreinte du reçu. Pour modifier/annuler, utiliser l’identifiant et la version renvoyés par le serveur et une nouvelle référence d’opération.

## Architecture et limites

OperationPersonnelleSynchronisationService adapte les commandes aux services existants ; aucune duplication du métier, nouvelle table, migration ou suppression de données. Le schéma reste en V34. Les reçus existants restent lisibles.

La persistance de la file hors connexion du téléphone reste à implémenter dans un lot mobile. Ce backend n’ajoute pas de résolution d’identifiants locaux : une modification dépendant d’une création doit attendre son identifiant/version serveur, puis être envoyée séparément. Il ne fusionne pas automatiquement des modifications concurrentes. Après CONFLIT, le client doit relire et faire traiter le conflit. L’API de synchronisation nécessite une connexion ; les notifications liées au serveur restent indisponibles pendant la coupure.

## Validation

Tests ciblés initiaux : 20 réussis sur H2/PostgreSQL. Défaut de fuseau invalide reproduit avant correction ; la nouvelle suite couvre aussi les dates extrêmes. Revue indépendante : aucun autre P1/P2 dans ce lot. Recette API réelle réussie avec un patient fictif autonome : lot partiellement valide, agenda et traitement persistés, mêmes identifiants après réessais directs/par lot, modification/annulation, reconnexion et restitution du reçu initial sans recréation. Aucun fournisseur externe appelé.

Validation complète finale : 259 tests réussis, aucun échec, erreur ou test ignoré ; `mvnw -o verify` et paquet Spring Boot réussis à 15:55:33 UTC.

Un test existant de transport de notifications a échoué au premier passage complet (ACCEPTE attendu, A_REESSAYER observé), puis a réussi isolément et dans la relance complète, sans modification. Sa cause intermittente reste non établie ; elle n’est pas présentée comme corrigée. Historique et résultats dans docs/VALIDATION.md.
