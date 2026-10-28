# Publication du 28 octobre 2026

Branche dédiée : `ecare_backend_autonomie_rapports`, dépôt backend indépendant des dépôts mobile et web, sans fusion dans main.

## Contenu regroupé

- Agenda personnel persistant et versionné, annulation idempotente, rappels d'agenda et invalidation des notifications associées.
- Occurrences de traitement partagées entre planning et rappels, réponses de prise/report cohérentes, boîte d'envoi et absence de livraison fictive.
- Synchronisation des opérations personnelles d'agenda et de déclaration de traitement, transactions par élément et références idempotentes.
- Capture immuable des paramètres des règles appliquées aux alertes, provenance et réinitialisation de validation à la modification, historique inconnu conservé comme tel.
- Synthèse de consultation JSON enregistrée, statistiques descriptives en unités canoniques, période/fuseau et limites explicites, accès propriétaire et consentement.
- PDF privé depuis la synthèse figée, graphiques/annexe/rubriques, polices et licence embarquées, pagination et erreurs sans export partiel.
- Migrations additives V33 à V36 et tests H2/PostgreSQL de préservation ; documentation API, plans et bilans par lot.

## Vérification avant publication

`mvnw -o verify` : **303 tests réussis**, aucun échec, erreur ou test ignoré (64 rapports XML). Paquet Spring Boot compilé. Fin : 2026-10-28T09:28:36Z.

La configuration PostgreSQL de test ferme ses contextes après chaque classe et conserve les connexions nécessaires aux transactions imbriquées. Les règles et pools de production n'ont pas été ajustés pour masquer des défauts de test.

Les comptes, jetons, bases, sauvegardes, journaux, PDF de recette et résultats compilés sont exclus de Git. Les polices Poppins sont des ressources source fournies avec leur licence. Les fournisseurs externes, la validation clinique et la recette Internet/appareils restent soumis aux limites décrites dans les bilans ; aucun diagnostic ou nouveau seuil médical n'est introduit.
