# Phase 1 — autonomie du patient

Livraison du 27 octobre 2026, sur les branches `ecare_backend_autonomie_patient` et `ecare_mobile_autonomie_patient`.

## Périmètre

Un patient peut s'inscrire et accéder à son suivi sans médecin ni structure affectés. Les dossiers, mesures et prescriptions institutionnels existants sont conservés. Cette phase ne change pas le design général, ne développe ni PDF ni paiement et ne rend pas encore le hors connexion durable.

## Décisions et modifications

- `DossierSuivi` conserve son constructeur institutionnel exigeant une structure ; la fabrique `personnel` crée explicitement un dossier dont la structure est nulle. La migration V32 autorise ce cas, ajoute un index unique du dossier personnel actif et crée un dossier personnel pour les patients historiques sans effacer ni déplacer leurs dossiers précédents.
- `DossierPersonnelService` centralise la création sous verrou du patient. L'inscription crée le dossier personnel dans la même transaction ; la consultation du profil prend aussi en charge les anciens comptes sans dossier.
- `PortailService.soi` vérifie toujours l'identité authentifiée et le consentement de suivi pour les actions cliniques personnelles, mais ne requiert plus d'affectation. Les contrôles des professionnels restent fondés sur leurs affectations, leur structure et le consentement.
- Compatibilité : pour un patient déjà rattaché disposant d'un dossier institutionnel actif dans cette structure, les anciennes routes de saisie continuent d'utiliser ce dossier. Sinon elles utilisent son dossier personnel. L'historique personnel reste consultable par son propriétaire. Une déclaration de traitement personnelle est toujours enregistrée dans son dossier personnel.
- `AccesPatientService`, les mappers et les détails d'alertes refusent explicitement l'accès professionnel à un dossier personnel sans structure. Le référencement QR ignore ces dossiers personnels lors du rapprochement institutionnel, en les conservant séparément.
- Le moteur d'alertes accepte les mesures personnelles pour les règles globales applicables et produit une notification au patient. Il ne leur affecte pas de professionnel et ne notifie pas un médecin. Le comptage des répétitions reste dans le dossier concerné : une ancienne mesure privée n'alimente pas une alerte institutionnelle. Aucun seuil médical de production n'est ajouté.
- Les réessais de mesures et de synchronisation conservent le dossier d'origine de la mesure déjà enregistrée, même si une affectation est créée entre l'envoi initial et son réessai.
- `DeclarationTraitementService` ajoute un parcours de déclaration personnelle, distinct d'une prescription médicale : traitement, médicament, horaire et reçu idempotent sont enregistrés ensemble. Un professionnel ne peut pas consulter, modifier ou réutiliser le médicament privé dans une autre prescription via les anciennes routes du catalogue.
- Flutter ne bloque plus sur `professionnelId` absent. Le consentement, l'authentification et la disponibilité du dossier restent contrôlés. Le statut personnel et les textes d'aide présentent le rattachement comme facultatif.
- Un formulaire ajouté à « Mes traitements » reprend le style existant et propose une prise quotidienne en UTC explicitement affiché. Les déclarations personnelles sont identifiées dans le planning. Le formulaire conserve une même référence en cas de réponse incertaine ; il doit rester lié à la session qui l'a ouvert.

## API ajoutée et compatibilité

`POST /api/v1/mobile/me/traitements`, patient authentifié, réponse 201 :

```json
{
  "sourceRef": "UUID conservé lors d'un réessai identique",
  "libelle": "Traitement existant déclaré",
  "dateDebut": "2026-10-27",
  "dateFin": null,
  "medicament": {
    "nomCommercial": "Exemple fictif",
    "dosage": "10",
    "uniteDosage": "mg",
    "forme": "comprimé"
  },
  "prise": {
    "quantite": 1,
    "unite": "comprimé",
    "heurePrevue": "08:00",
    "fuseau": "UTC",
    "joursSemaine": [1, 2, 3, 4, 5, 6, 7]
  }
}
```

Cet exemple décrit un contrat technique et aucune recommandation thérapeutique. Le propriétaire vient du JWT ; aucun identifiant patient, dossier ou professionnel n'est demandé. Même référence et même contenu renvoient le même reçu ; un contenu différent avec la même référence donne 409. Réponse : `traitementId`, `medicamentId`, `priseId`, `versionPrise`, `fuseau`, `joursSemaine`.

Les routes de planning, d'observance et de réponse aux prises existantes sont réutilisées. Les traitements du planning ajoutent le booléen `declarationPersonnelle`. Les champs `structureId` du profil et `structure` du dossier mobile peuvent être nuls. Les routes professionnelles gardent leurs droits et formats existants.

## Vérifications

Les résultats finaux sont consignés après exécution dans `docs/VALIDATION.md`. Les journaux, comptes fictifs, bases de test, sauvegardes et captures restent locaux, hors du dépôt.

## Limites restantes

- Le consentement de suivi existant est conservé ; cette phase ne crée pas un nouveau système de partage temporaire.
- Le formulaire initial propose une prise quotidienne, avec horaire explicitement UTC ; les horaires avancés et leur édition ne sont pas ajoutés.
- Une déclaration personnelle ne valide ni médicament, ni dose, ni prescription. Les règles de démonstration ne sont pas présentées comme validées médicalement.
- Les notifications externes et rappels avec application fermée nécessitent encore la configuration et la recette des fournisseurs/appareils.
- Le hors connexion durable, l'agenda persistant, le nouvel accueil et les rapports relèvent des phases suivantes.
