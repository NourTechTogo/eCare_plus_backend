# Agenda personnel et rappels — backend

Travaux locaux du 27 octobre 2026, sans publication GitHub. Aucun changement Flutter ou React : le raccordement des interfaces à ces nouvelles routes reste à faire.

## Périmètre

Le patient conserve ses rendez-vous personnels sur le serveur sans médecin ni structure affectés. Le nom libre du professionnel et le lieu sont informatifs : cette saisie ne réserve aucun créneau et ne confirme aucune consultation auprès d'un établissement.

Le backend réutilise `RendezVous`, `Rappel`, la boîte privée et la file de distribution existants. L'origine personnelle distingue ces entrées des rendez-vous professionnels. La migration V33 conserve les données historiques ; elle ne transforme pas les anciens rendez-vous en événements personnels.

## Routes personnelles

Propriétaire imposé par le JWT, avec compte actif et consentement de suivi :

- `GET /api/v1/mobile/me/rendez-vous` : page personnelle, tri date puis identifiant, maximum 100 éléments par page. `inclureAnnules=true` inclut les annulations. `du` et `au` sont deux instants requis ensemble, intervalle `[du, au[` de 366 jours maximum.
- `GET /api/v1/mobile/me/rendez-vous/{id}` : détail personnel.
- `POST /api/v1/mobile/me/rendez-vous` : création, réponse 201.
- `PATCH /api/v1/mobile/me/rendez-vous/{id}` : modification avec version courante, réponse 200.
- `DELETE /api/v1/mobile/me/rendez-vous/{id}` : annulation logique, réponse 200 ; corps `{sourceRef, version}`. L'alias `POST /api/v1/mobile/me/rendez-vous/{id}/annulation` accepte la même commande.

Exemple de création, avec données fictives :

```json
{
  "sourceRef": "11111111-1111-4111-8111-111111111111",
  "motif": "Consultation de suivi",
  "dateHeure": "2026-11-12T10:00:00Z",
  "fuseau": "Africa/Lome",
  "lieu": "Lieu indiqué par le patient",
  "professionnelNom": "Professionnel indiqué par le patient",
  "commentaire": null,
  "rappelMinutes": 60
}
```

La modification reprend ces champs avec un nouveau `sourceRef` et la `version` courante. Limites : motif 200 caractères, fuseau IANA 80, lieu 300, nom 200, commentaire 2000. Les années 1 à 9999 sont prises en charge. Date absolue UTC et fuseau sont distincts ; les instants sont normalisés à la microseconde pour PostgreSQL.

`rappelMinutes:null` désactive le rappel ; 0 le programme à l'heure de l'événement ; maximum technique 10080 minutes, soit sept jours. Ces paramètres ne sont pas des recommandations médicales.

La réponse contient `id`, `patientId`, `motif`, `dateHeure`, `fuseau`, `lieu`, `professionnelNom`, `commentaire`, `statut`, `personnel`, `version`, `rappelMinutes`, `rappelId`, `rappelLe`. L'échéance `rappelLe` n'est pas une preuve de livraison ; les routes personnelles de rappels existantes exposent leur statut effectif.

## Réessais et versions

Chaque commande porte une référence UUID nouvelle, conservée lors d'un réessai inchangé. Les écritures sont sérialisées par patient et enregistrent un reçu dans la même transaction : même référence et même commande renvoient le reçu initial, même après une modification ultérieure. Utiliser GET pour relire l'état courant.

Une référence réutilisée pour un autre contenu ou une autre action donne 409. Une version périmée donne 409. Une commande invalide donne 400 sans écriture partielle. L'annulation conserve le rendez-vous dans l'historique.

## Rappels

Le travail planifié d'agenda s'exécute toutes les 60 secondes par défaut (`app.agenda.intervalle-ms`), lorsque `app.traitements-planifies=true`, avec au maximum 100 échéances par passage. Le déclenchement verrouille le patient, recharge l'événement et vérifie compte actif et consentement `SUIVI_MEDICAL`. Deux workers ne produisent pas deux notifications pour la même échéance.

Un rappel échu publie une notification privée de type `RENDEZ_VOUS`, ciblant `RENDEZ_VOUS_PERSONNEL`. Le statut `ENVOYE` du rappel signifie cette publication dans l'application ; les envois externes ont leurs propres statuts dans la file de distribution.

Une tolérance opérationnelle de 15 minutes après l'événement absorbe le retard du scheduler, notamment pour les rappels à zéro minute. Au-delà, le rappel est annulé sans notification tardive. Cette tolérance n'a aucune signification médicale.

La modification ou l'annulation masque l'ancienne notification et annule les envois en attente, en reprise ou réservés dans la base. La provenance permanente `agenda_personnel_id` protège aussi les anciens rappels après remplacement. Elle est stockée comme identifiant opaque, sans clé étrangère inverse : cela évite un cycle de dépendances lors de la restauration des sauvegardes.

La distribution recontrôle préférences, terminal, session et validité de la ressource avant réservation. Les envois au patient lui-même n'exigent plus d'affectation médicale. Les ressources personnelles ne deviennent pas distribuables à un professionnel par simple affectation.

Limite : un worker peut déjà détenir une réservation et être en train d'appeler le transport. L'annulation en base empêche les reprises suivantes, mais ne garantit pas la rétractation de cet appel en cours ni d'une livraison déjà acceptée par le fournisseur.

## Confidentialité et limites

- Refus des accès croisés ; listes et routes professionnelles excluent les rendez-vous personnels, leurs rappels et leurs notifications privées.
- Les rendez-vous professionnels historiques gardent leurs routes et leurs droits.
- Aucun fournisseur n'est choisi ou activé ici. Les tests utilisent des terminaux fictifs et une passerelle locale contrôlée ; ils ne prouvent pas une livraison réelle sur téléphone.
- La publication serveur peut fonctionner avec l'application fermée ; un push reçu sur l'appareil nécessite encore fournisseur, terminal et autorisations configurés.
- Hors connexion durable Flutter, rappels locaux Android/iOS, nouvel accueil, rapports et paiements restent hors de ce lot backend.

Les résultats réels de recette sont consignés dans `docs/VALIDATION.md`.
