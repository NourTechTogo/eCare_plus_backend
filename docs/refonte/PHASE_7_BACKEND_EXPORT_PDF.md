# Backend - export PDF de la synthèse de consultation

Lot local du 27 octobre 2026. Mobile et web inchangés, aucun commit/push. Complète la phase 6 JSON ; ne remplace aucun rapport historique.

## Contrat

`GET /api/v1/mobile/me/rapports-consultation/{id}/pdf` renvoie `application/pdf`, `Content-Disposition: attachment` avec un nom fixe dérivé du UUID, `Cache-Control: private, no-store` et `X-Content-Type-Options: nosniff`. Utiliser le jeton Bearer dans une requête authentifiée puis télécharger le blob, sans mettre de jeton dans l'URL.

Le service de consultation existant vérifie le compte, le consentement SUIVI_MEDICAL et la propriété avant de lancer le rendu. Un autre patient ou un professionnel affecté ne reçoit pas le rapport personnel. Le téléchargement n'accorde aucun accès clinique supplémentaire. Sans authentification : 401 ; identité/consentement refusé : 403 ; rapport absent : 404. Aucune URL publique.

## Contenu et confidentialité

Rendu Java à partir du snapshot JSON enregistré uniquement : identité minimale, période et fuseau, génération et référence, avertissements, statistiques descriptives et points de mesure, traitements et horaires courants à la génération, réponses déjà enregistrées avec forme/voie, alertes avec règle capturée disponible, agenda et statuts, annexe de toutes les valeurs par composante.

Graphiques vectoriels avec tous les points, sans interpolation ni seuil clinique. Deux points superposés peuvent apparaître comme un seul ; l'annexe conserve chaque valeur/référence. La tension reste séparée en composantes. Aucun diagnostic, nouveau calcul d'observance, conseil thérapeutique ou réservation médicale.

PDF A4 paginé, polices Poppins embarquées et licence conservée. Retour à la ligne des textes longs, y compris références sans espaces. Un glyphe absent devient [U+XXXX], avec avertissement explicite ; le JSON garde le texte original.

Le rendu ne relit aucune donnée clinique, ne crée aucun fichier permanent sur le serveur et ne modifie ni le snapshot ni le statut JSON/GENERE du rapport. Un téléchargement ultérieur conserve le contenu ; les octets PDF ne constituent pas une empreinte stable (identifiant interne généré par PDFBox). Pas de signature électronique ni d'archivage PDF/A promis.

## Bornes et dépendances

Plafonds par téléchargement : 300 pages et 16 Mio de sortie. Dépassement : 400 JSON explicite invitant à réduire la période, aucun PDF partiel. La synthèse JSON existante reste consultable. Les plafonds de collecte de phase 6 restent applicables. Les contrôles de taille/pages ne remplacent pas une politique de charge/concurrence d'exploitation.

Apache PDFBox 2.0.37, branche 2.x publiée par Apache : https://pdfbox.apache.org/2.0/getting-started.html . Aucune installation Python/Poppler nécessaire au serveur. Les ressources sont embarquées dans le paquet Spring Boot.

## Validation

Tests ciblés, revue et recette : voir le bilan actualisé dans ../VALIDATION.md. L'intégration du bouton de téléchargement aux interfaces est une étape suivante.
