# Export PDF de la synthèse de consultation

Périmètre : backend uniquement, changements locaux sans commit ni push.

1. Tester le téléchargement authentifié, l'isolement patient, le consentement et le contenu figé avant d'ajouter la route.
2. Rendre en Java la synthèse déjà enregistrée, sans nouvelle lecture des mesures ni modification du rapport. PDF A4, police embarquée et licence, pagination, graphiques descriptifs et détails des rubriques. Les glyphes indisponibles sont remplacés explicitement et signalés ; aucune interprétation médicale.
3. Fournir GET /api/v1/mobile/me/rapports-consultation/{id}/pdf, pièce jointe privée sans cache. Aucun lien public ni fichier serveur persistant. Borner pages et taille avec erreur explicite, jamais un PDF tronqué.
4. Vérifier extraction de texte, pages, caractères, graphiques, limites, accès et gel des données ; exécuter les tests H2/PostgreSQL, demander une revue ciblée, contrôler visuellement un exemple fictif.
5. Documenter le contrat et les limites, vérifier les clients inchangés. L'intégration des boutons dans le mobile et le web sera une étape distincte.

## Avancement

- Implémentation validée en ciblé. Après extraction des helpers, 9 tests PDF distincts restent dans la suite (5 de rendu, 2 HTTP H2, 2 HTTP PostgreSQL), sans duplication des 22 tests JSON originaux.
- Revue indépendante : omission forme/voie des réponses corrigée et couverte ; aucune P1/P2 restante identifiée à la relecture.
- Recette API réelle : téléchargement privé, gel après nouvelle saisie/reconnexion, accès sans jeton et consentement retiré refusés.
- PDF de test et PDF de l'API rendus et inspectés sur leurs deux pages ; contrôle indépendant des coordonnées du texte passé.
- Suite complète et paquet final validés : 303 tests réussis, aucun ignoré, à 2026-10-27T23:21:28Z. Voir docs/VALIDATION.md pour le bilan définitif.

- Premier verify : interférence d’une règle globale de fixture et saturation des pools de contextes PostgreSQL révélées. Portée de fixture limitée au patient, helpers partagés sans héritage des tests et profil de tests limité à 4 connexions par pool. Groupe de contrôle : 32 tests passés. Le second verify a ensuite révélé un timeout de concurrence ; voir la correction du cycle de vie ci-dessous.

- Second verify : 303 tests exécutés, un timeout du scénario de prescription concurrente. Le pool réduit à 4 affamait les transactions imbriquées. Stratégie corrigée : 10 connexions (minimum 1) et DirtiesContext(AFTER_CLASS) sur les seules classes PostgreSQL pour libérer chaque pool après sa classe. Groupe ciblé : 13 tests réussis avec fermeture effective des pools. Troisième verify : 303 tests réussis, aucun échec/erreur/ignoré ; paquet final validé le 27/10/2026 à 23:21:28 UTC.
