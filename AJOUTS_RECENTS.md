# Ajouts et corrections récents — Backend eCare+

## Dernière livraison : restauration et appels locaux

- Agenda professionnel réel : route GET filtrée par affectation et consentement.
- Réessai idempotent des messages via UUID `sourceRef`, migration V29 ; nouvelle commande avec même clé refusée.
- Déconnexion : fin des appels et fermeture distante avec reprise persistante. Fermeture LiveKit idempotente si un participant est déjà absent.
- LiveKit 1.13.10 local activé après validation réelle des salles et des anciens jetons ; AUDIO et VIDEO testés avec le Samsung et le portail.
- Démonstration maintenant sur H2 **fichier** ; les données fictives ont été recréées après constat d’une base mémoire vide. Ressources de test exclues du démarrage ; fichiers privés sauvegardés et stockage dédié.
- 167 tests backend réussis, PostgreSQL 18.6 dédié, migrations jusqu’à V29. Démarrage : `scripts/demarrer-demo-locale.ps1` sur cette installation préparée.

Bilan actuel : [restauration et vérifications](docs/restauration/BILAN.md). Les sections ci-dessous décrivent aussi des étapes historiques antérieures à cette livraison.

État documenté le 25 octobre 2026. Ce fichier concerne le serveur commun au mobile Flutter et au frontend React. Il récapitule les travaux du plan backend et de l'intégration des clients ; il ne constitue pas une garantie d'absence de bugs.

## Authentification et contrat commun

- Alignement des clients sur l'API `/api/v1`, les DTO et les unités des six types de mesures.
- Harmonisation des parcours d'authentification, inscription, OTP, réinitialisation du mot de passe, profil et consentement.
- Contrôles JWT, rôles, révocation de session, patient actif, structure et rattachement au médecin.
- Vérification du consentement clinique pour accéder aux données de santé ; le rôle gestionnaire ne donne pas automatiquement cet accès.
- Conservation des anciennes routes nécessaires à la compatibilité.

## Documents médicaux privés

- Dépôt patient ou dépôt assisté par un professionnel autorisé, liste paginée, téléchargement authentifié et retrait logique.
- PDF, JPEG et PNG, limite de 5 Mio par défaut ; contrôle du contenu réel, du type MIME et des dimensions des images.
- Stockage privé avec identifiants opaques, réponses de téléchargement sans cache et protection contre l'interprétation du contenu.
- Référence d'origine pour éviter les doublons, compensation en cas d'échec et nettoyage des fichiers orphelins avec réessais.

## Traitements, prises et observance

- Planning avec fuseau IANA, jours de semaine, historique daté des modifications et instantané des doses prévues.
- Occurrences individuelles et réponses pris, reporter ou ignorer, avec contrôle de version et références stables.
- Distinction entre date prévue, action du client et confirmation serveur ; gestion des annulations et réactivations.
- Observance calculée sur les prises échues non annulées, sans pourcentage trompeur quand aucune prise n'est attendue.
- Prescription atomique via `POST /portail/patients/{patientId}/prescriptions` : traitement, médicament, prise, fuseau, jours et reçu sont enregistrés ensemble.
- Réessai identique sans doublon, y compris en concurrence ou après perte de réponse ; contenu différent avec la même clé refusé par une réponse 409.
- Correction des prescriptions partielles, de la première prise après minuit dans un fuseau positif et des précisions de dates avec PostgreSQL.

## Notifications

- Boîte de notifications commune au compte, lecture persistée et navigation vers les ressources autorisées.
- Préférences serveur et historique du consentement par canal.
- Enregistrement des terminaux lié à la session et à sa version, invalidation et contrôle avant envoi.
- File transactionnelle d'envoi, déduplication, réservation des tâches, reprises et adaptateur HTTP de passerelle.
- Retours de passerelle signés HMAC, contrôle du timestamp, protection contre le rejeu et progression cohérente des statuts.

## Synchronisation et rattachement QR

- Synchronisation typée des créations de mesures et réponses aux prises, avec écriture métier réelle.
- Reçus par acteur, terminal, identifiant local et type ; traitement partiel avec succès, rejet ou conflit par opération.
- Conservation de l'ancien journal opaque sans le présenter comme une synchronisation métier réussie.
- Code de référencement temporaire, secret conservé sous forme de hash, expiration, régénération et révocation.
- Résolution administrative puis rattachement atomique au médecin ; aucune attribution implicite de consentement clinique ou mutation silencieuse de structure.

## Messagerie, notes et appels

- Pièces jointes privées PDF/images, jusqu'à cinq fichiers de 5 Mio par défaut ; contrôle des participants, brouillons et liaison transactionnelle au message.
- Notes ciblées sur messages, appels, mesures et alertes ; visibilité privée ou partagée et modification/suppression versionnées par l'auteur.
- États des appels : création, acceptation, refus, fin et expiration ; historique et salle limitée aux participants.
- Adaptateur LiveKit, autorisations de courte durée, identités opaques, contrôle des webhooks signés et déduplication.
- Durée connue seulement lorsque la présence des deux participants est constatée ; fermeture distante et révocation avec reprises persistantes.
- Réponse 503 explicite si le fournisseur d'appel n'est pas configuré.

## Suivi clinique et confidentialité

- Résumé patient via `/portail/patients/{patientId}/resume`, pagination filtrée par droits avant constitution de la page.
- Harmonisation des sévérités/statuts d'alertes, déduplication par sévérité et variation relative de poids.
- Retrait du consentement effectivement appliqué aux accès cliniques.
- Suivi d'une demande d'effacement ; cette demande ne déclenche pas une suppression physique automatique.

## Base de données et vérifications

Migrations additives V15 à V28 : documents, prises/planning, préférences/terminaux, distribution des notifications, synchronisation/QR, pièces/notes, appels, événements signés/effacement, règles cliniques, déduplication, instantanés de doses, historique des plannings, navigation des notifications et reçus de prescription atomique. Les migrations V1 à V14 ont été conservées.

La recette enregistrée comprend **162 tests backend réussis**, dont **15 sur PostgreSQL réel 18.6**, sans échec ni test ignoré. Elle couvre notamment les migrations V1–V28, la montée depuis une V14 peuplée, la concurrence et la validation JPA. Ces résultats proviennent de la recette précédente, pas d'une nouvelle exécution lors de la rédaction de ce fichier.

Des parcours HTTP et navigateur ont aussi vérifié la persistance commune entre web et Flutter. Les corrections comprennent les types MIME, CORS, les fuseaux, les réessais de prescription et les droits sur les fichiers.

## Configuration locale et limites

Backend de démonstration : `http://127.0.0.1:8081/api/v1`, avec données fictives et base H2 sur disque pour la démonstration locale. Cette configuration reste destinée au développement. Pour le téléphone, l'APK installé cible cette adresse à travers un tunnel USB ADB.

### Comptes courts de développement

Comptes fictifs configurés et connexions vérifiées sur le backend 8081 : patient `p@e.test`, médecin `m@e.test`, gestionnaire `a@e.test`. Après le constat de base mémoire vide, les comptes fictifs ont été recréés ; leurs nouveaux identifiants internes sont consignés dans la fixture locale. Le changement de mot de passe invalide leurs anciennes sessions : il faut se reconnecter avec les nouveaux identifiants.

La configuration sans mot de passe est dans [development-accounts.json](config/development-accounts.json). Le script [configurer-comptes-demo.py](scripts/configurer-comptes-demo.py) applique ces valeurs par les API authentifiées, uniquement sur `127.0.0.1:8081`. Il lit le mot de passe local dans la clé `APP_DEMO_LOCAL_PASSWORD` du fichier ignoré `.maintenance/runtime-secrets.json`. Aucune règle de mot de passe de production n'a été affaiblie.

Après réinitialisation de la base H2, recréer les données fictives avec le script de recette `.maintenance/integration-clients/seed.py`, puis relancer ce script de configuration. Il peut être relancé sans recréer les comptes. Les anciens rapports décrivent les adresses utilisées au moment de leur recette ; les nouvelles connexions sont consignées dans `.maintenance/integration-clients/development-accounts-proof.json`.

La configuration des fournisseurs est préparée dans [external-services.env.example](config/external-services.env.example), sans secrets. Le push externe reste à configurer ; les appels locaux ont désormais une recette réelle décrite dans le bilan de restauration. HTTPS de production, charge, sauvegardes d'exploitation, seuils cliniques et politique de conservation restent à traiter avant une exploitation réelle.

## Références

- [Contrat et routes API](docs/backend/API.md)
- [Bilan backend](docs/backend/BILAN.md)
- [Bilan d'intégration et limites](docs/integration-clients/BILAN.md)
- [Preuves de vérification](docs/VALIDATION.md)
- [Configuration des services](docs/integration-clients/CONFIGURATION.md)
- [Ajouts web](https://github.com/NourTechTogo/eCare_plus_frontend/blob/ecare_frontend_stable/AJOUTS_RECENTS.md) et [ajouts mobile](https://github.com/NourTechTogo/eCare_plus_mobile/blob/ecare_mobile_stable/AJOUTS_RECENTS.md)
