# Validation et limites des livraisons eCare+

## Publication sur la nouvelle branche - 28 octobre 2026

Contenu regroupé sur `ecare_backend_autonomie_rapports`, sans fusion dans main. Vérification prépublication complète : **303 tests réussis**, aucun échec/erreur/ignoré ; paquet Spring Boot compilé à 2026-10-28T09:28:36Z. Voir [le bilan de publication](PUBLICATION_2026-10-28.md). Les mentions « travail local / aucun push » des phases antérieures décrivent leur état avant cette publication.

## 27 octobre 2026 - backend local : export PDF de consultation

- `mvnw -o verify` complet : **303 tests réussis**, aucun échec, erreur ni test ignoré (64 rapports XML). Fin : 2026-10-27T23:21:28Z. Compilation et paquet Spring Boot réussis ; PDFBox 2.0.37, FontBox et les deux polices/licence présents dans le JAR.
- 9 nouveaux tests PDF (5 de rendu, 2 HTTP H2 et 2 HTTP PostgreSQL), couverts dans la suite finale : contrôle propriétaire/consentement, téléchargement privé et gel effectif du texte PDF après nouvelle mesure, rubriques renseignées (forme/voie comprises), points/axes du graphique variable, rapport vide, pagination et Unicode indisponible. Dépassement de pages testé directement et via HTTP : 400 application/problem+json, aucun PDF partiel.
- Le test du plafond de pages a nécessité une fixture dépassant réellement 300 pages ; le contrat d'erreur a été vérifié avec le type application/problem+json existant. La garde de sortie 16 Mio reste défensive, sans test direct de saturation ; aucune validation de charge/concurrence de rendu n'est revendiquée.
- Première suite complète échouée : une règle globale de fixture contaminait le test de seuils, et le nettoyage PostgreSQL saturait les connexions avec les contextes mis en cache. Helpers extraits pour éviter de rejouer 22 tests JSON dans les classes PDF ; règle de fixture limitée au patient ; seconde tentative avec pools réduits à 4 ayant révélé une famine de connexions dans le test de prescriptions à transactions imbriquées. Correction finale : pools de recette à 10 connexions (minimum 1) et fermeture des contextes PostgreSQL après chaque classe avec DirtiesContext(AFTER_CLASS). Aucune règle clinique ni configuration de pool de production modifiée. Les 22 tests JSON originaux restent exécutés. Revue des corrections sans P1/P2 identifié.
- Revue indépendante : omission forme/voie des réponses détectée puis corrigée avec couverture des rubriques. Relecture sans P1/P2 restant identifié.
- Recette API réelle passée : PDF privé avec statistiques attendues, PDF et JSON inchangés après nouvelle saisie, reconnexion conservant le contenu, accès sans jeton et consentement retiré refusés. Aucun fournisseur externe appelé.
- Contrôle visuel : PDF fictif de mesures, PDF téléchargé de l'API et exemple avec traitements/prises/alertes/agenda et caractère indisponible rendus en PNG, deux pages chacun inspectées. Vérification indépendante du texte sans dépassement de page, pagination et polices embarquées réussie.
- API locale relancée après fin de compilation, sauvegarde H2 fichier contrôlée par empreinte avant redémarrage. Aucun changement mobile/web ni des comptes de démonstration ; HEAD conservé, aucun commit ni push.

[Contrat et limites](refonte/PHASE_7_BACKEND_EXPORT_PDF.md).

## 27 octobre 2026 — backend local : synthèse de consultation JSON (V36)

- `mvnw -o verify` complet : **294 tests réussis**, aucun échec, erreur ni test ignoré ; compilation et paquet Spring Boot réussis. Fin : 27/10/2026 à 22:12:36 UTC.
- H2/PostgreSQL : statistiques en unités canoniques et composantes distinctes, période/fuseau et bornes exclusives, absence de données, contenu figé après nouvelle saisie, références/réessais/concurrence, confidentialité même avec médecin affecté et retrait du consentement.
- Rubriques : traitements/configuration courante, réponses enregistrées y compris anticipées pour aujourd’hui, alertes liées aux mesures ou manuelles, captures de règle et agenda annulé. La génération ne crée aucune occurrence de prise et ne déduit aucun taux d’observance. Plus de 5000 mesures est refusé sans rapport partiel ni troncature.
- Migration V35→V36 : comparaison des rapports historiques avant/après réussie, aucun rapport ancien converti. Migrations V1 à V36 et sauvegarde/restauration PostgreSQL passées dans la suite complète.
- Revue indépendante : omission des réponses anticipées identifiée puis corrigée avec test RED/GREEN. Les tests ont aussi révélé le remplissage trop tardif des colonnes insert-only ; le JSON est maintenant attaché avant persist, puis l’UUID stable de la ligne complète la vue API. Revue des corrections sans P1/P2 restant.
- Recette API réelle locale avec patient fictif autonome : génération, moyenne/unité vérifiées, relecture/rejeu identiques après nouvelle mesure, conflit 409 pour référence incompatible, nouvelle synthèse actualisée, liste et relecture après reconnexion. Aucun PDF généré ni fournisseur externe appelé.
- API locale redémarrée après sauvegarde vérifiée et compilation. Mobile/web inchangés, HEAD backend conservé, aucun commit/push. Le PDF et l’intégration aux interfaces restent pour les lots suivants.

[Bilan et contrat](refonte/PHASE_6_BACKEND_SYNTHESE_CONSULTATION.md).

## 27 octobre 2026 — backend local : traçabilité des alertes (V35)

- Validation complète finale `mvnw -o verify` : **271 tests réussis**, aucun échec, erreur ou test ignoré ; paquet Spring Boot produit. Fin : 27/10/2026 à 21:32:33 UTC.
- Nouvelles alertes : capture indépendante des paramètres, version technique et provenance de validation ; ancien seuil conservé après modification puis revalidation de la règle. Les changements de paramètres, contexte ou portée effacent date/auteur/source de validation et imposent une revalidation.
- H2/PostgreSQL : vues génériques, portail et mobile, origine DEMONSTRATION distincte, confidentialité maintenue, ancienne alerte sans capture explicitement inconnue. Migration V34→V35 vérifiée avec comparaison des données historiques ; sauvegarde/restauration PostgreSQL réussie.
- Recette API réelle locale réussie avec données fictives, avant puis après reprise de l’environnement : capture initiale, modification sans changement de l’ancienne alerte, métadonnées invalidées, réactivation sans validation refusée, nouvelles alertes avec nouveaux paramètres. Aucun envoi fournisseur externe.
- Premier passage complet interrompu extérieurement pendant le renouvellement de l’environnement (Java et PostgreSQL arrêtés) : il n’est pas compté comme validation réussie. Après récupération automatique PostgreSQL et redémarrage de l’API une fois les classes compilées, l’authentification et la recette ont repassé sans changement des comptes ni mots de passe. Le journal final distinct est la référence.
- Revue indépendante sans P1/P2 sur ce lot. Aucun nouveau seuil médical ni validation clinique externe ; le schéma est V35. Mobile/web inchangés, HEAD backend conservé, aucun commit/push.

[Bilan et contrat](refonte/PHASE_5_BACKEND_TRACABILITE_ALERTES.md).

## 27 octobre 2026 — backend local : synchronisation personnelle

- Contrôle complet final `mvnw -o verify` : **259 tests réussis**, aucun échec, erreur ni test ignoré ; paquet Spring Boot produit. Fin : 27/10/2026 à 15:55:33 UTC.
- 20 nouveaux scénarios H2/PostgreSQL : agenda et déclaration autonomes, réessais direct/lot, concurrence, conflits de versions/références, refus d’un autre propriétaire, erreurs partielles, fuseaux/dates extrêmes sans traitement partiel et reçu initial conservé après évolution.
- Les migrations restent V1 à V34 ; sauvegarde/restauration et fonctionnalités précédentes repassées dans la suite complète.
- Recette API réelle locale : lot partiellement invalide accepté élément par élément, mêmes ressources après réessais, modification puis annulation d’agenda et restitution du reçu initial après reconnexion. Données fictives, aucun envoi fournisseur externe.
- Incident de validation conservé : premier passage complet, 259 tests dont un échec dans DistributionNotificationIntegrationTests.outboxPersistanteReessayeSansAnnoncerLivraison (ACCEPTE attendu, A_REESSAYER observé). Ce test existant passe isolément, puis dans le second passage complet sans modification du code ni de ses assertions. Cause non établie ; aucune correction de ce comportement intermittent n’est revendiquée.
- Revue indépendante du lot : aucun autre P1/P2 que la gestion des DateTimeException, corrigée et couverte par tests.
- API locale redémarrée après sauvegarde vérifiée. Mobile/web inchangés ; aucun commit ni push. La file persistante sur téléphone reste pour le lot mobile.

[Bilan et contrat](refonte/PHASE_4_BACKEND_SYNCHRONISATION.md).

## 27 octobre 2026 — backend local : rappels de traitements (V34)

- `mvnw -o verify` complet : **239 tests réussis**, zéro échec, erreur ou test ignoré ; compilation et paquet Spring Boot réussis. Fin de la validation : 27/10/2026 à 15:05:18 UTC.
- H2 et PostgreSQL : génération sans lecture du planning ni affectation, échéances/fuseaux, concurrence, fréquence imprécise, retrait de consentement, report depuis un jour antérieur, réponses PRIS/IGNORER/REPORTER, suspension, confidentialité et anciennes routes cliniques.
- Régressions vérifiées : un rappel invalidé ne peut pas être restauré, même s’il avait d’abord été supprimé par l’utilisateur. La suppression utilisateur ordinaire reste réversible.
- Migrations V1 à V34 et sauvegarde/restauration PostgreSQL vérifiées. Les tests des fonctionnalités précédentes restent verts.
- Recette API réelle locale réussie avec données fictives : déclaration autonome, notification produite par la tâche sans GET planning, réponse PRIS, rappel masqué, restauration 409 et historique relu. Aucun envoi fournisseur externe.
- Revue indépendante : trois chemins de défaut reproduits puis corrigés. Une première validation complète a été volontairement interrompue pour ajouter le dernier scénario ; seule la validation complète finale ci-dessus fait référence.
- API locale redémarrée après sauvegarde vérifiée de sa base. Mobile/web inchangés. Modifications backend conservées localement, sans commit ni push.

Les clients doivent encore raccorder la nouvelle cible de notification PRISE ; les fournisseurs externes et la livraison sur téléphone ne sont pas validés par ce lot. Voir [le bilan](refonte/PHASE_3_BACKEND_RAPPELS_TRAITEMENTS.md).


## Agenda personnel backend — recette locale du 27 octobre 2026

- Commande finale : `mvnw.cmd -o verify`, avec l'instance PostgreSQL dédiée aux tests et ses variables `ECARE_TEST_POSTGRES_*` configurées localement.
- **213 tests réussis, 0 échec, 0 erreur, 0 ignoré** ; package Spring Boot produit. Les 27 nouveaux cas couvrent CRUD personnel, droits, versions, reçus, rappels, distribution et migration.
- H2 et PostgreSQL : double worker sans doublon, rappel à zéro minute avec délai opérationnel, annulation après réservation, ancien rappel toujours privé après report et affectation, retrait du consentement et compte inactif.
- Flyway V1 à V33 et validation JPA ; migration V32 vers V33 préservant les anciennes colonnes et versions. Restauration logique PostgreSQL testée avec des documents privés et de véritables lignes d'agenda personnel/rappel en plus des données historiques.
- La première recette complète a révélé un cycle de dépendances de restauration ; la clé étrangère inverse a été retirée en conservant le marqueur privé permanent. Le test de transport HTTP local a échoué une fois, puis a réussi en reprise ciblée et dans la suite complète finale ; aucune modification du transport n'a été faite sans cause reproductible.
- API locale persistante : compte fictif sans médecin, création/réessai, refus professionnels, scheduler réel publiant une notification privée unique, report masquant l'ancienne notification, annulation et reconnexion avec historique conservé. Aucun terminal enregistré et aucun push externe envoyé lors de cette recette.
- La base locale a été sauvegardée et sa copie vérifiée avant redémarrage. Les logs, sauvegardes et données de recette restent hors du dépôt.
- Frontend et mobile inchangés. L'intégration des nouvelles routes dans les clients et la livraison sur appareil fermé avec un fournisseur réel restent à faire.
- Travaux conservés uniquement en local ; aucun envoi GitHub pour cet incrément. [Bilan, API et limites](refonte/PHASE_2_BACKEND_AGENDA.md).

## Phase 1 — autonomie patient, 27 octobre 2026

- Backend : 186 tests validés, aucun échec, erreur ou test ignoré dans les derniers rapports de chaque classe. La suite complète PostgreSQL/H2 a été exécutée ; après adaptation de l'ancienne attente « nouveau compte sans dossier », les trois scénarios HTTP de cette classe ont été rejoués avec succès via `mvnw -o -Dtest=BackendParcoursCompletTests verify`. Aucun code de production n'a changé entre ces deux dernières exécutions.
- La migration PostgreSQL V32 est vérifiée sur base existante V31 : conservation des dossiers institutionnels et des affectations, création des espaces personnels et unicité du dossier personnel actif. Les tests de schéma, sauvegarde/restauration et concurrence restent inclus.
- Mobile : 177 tests réussis, 2 tests réseau optionnels ignorés ; analyse sans anomalie. Compilation web et APK Android debug réussies. Les outils Android signalent des versions Gradle/AGP/Kotlin à mettre à jour ultérieurement ; cette livraison n'effectue pas cette migration d'outillage.
- Portail web inchangé : 41 tests réussis, 1 test réseau optionnel ignoré, compilation réussie. Vite signale toujours la taille de certains bundles.
- Serveur local persistant : inscription fictive sans médecin, dossier personnel, tension/glycémie, réessais idempotents, traitement déclaré, confirmation/observance, refus d'accès croisés et reconnexion avec conservation des données vérifiés. Le médecin conserve l'accès à son patient affecté.
- Flutter compilé : connexion sans affectation, planning personnel, déclaration réellement envoyée par le formulaire (HTTP 201), historique retrouvé dans une nouvelle session navigateur et affichage à 320/390 px vérifiés sans erreur de page.
- Régressions de confidentialité : les mesures privées ne participent pas aux alertes institutionnelles ; les médicaments des déclarations personnelles ne peuvent pas être réutilisés par un professionnel ; un brouillon ne peut pas être envoyé sous un compte remplaçant la session initiale.

La base H2 locale a été sauvegardée avant adaptation et redémarrage. Les tests utilisent uniquement des données fictives. La recette Android physique/iOS de cette phase, les notifications externes et le hors connexion durable restent à réaliser. Les détails de périmètre et fichiers sont dans [le bilan de phase 1](refonte/PHASE_1_AUTONOMIE_PATIENT.md).


## Validation des messages vocaux — 26 octobre 2026

- Backend : 171 tests réussis, 0 échec et 0 ignoré, avec PostgreSQL 18.6 dédié et migrations V1 à V31.
- Mobile : 171 tests réussis, 2 tests réseau optionnels ignorés ; `flutter analyze` sans anomalie ; compilations Flutter web et APK Android debug réussies.
- Web : 41 tests réussis, 1 test réseau optionnel ignoré ; lint et compilation TypeScript/Vite réussis. Vite signale encore la taille de certains bundles.
- Recette réelle locale avec microphone synthétique : capture et annulation sans envoi sur chaque client, envoi web vers Flutter puis Flutter vers web, accès privé, durée calculée par le serveur, lecture/pause et déplacement de lecture. Contrôles visuels Flutter à 320, 360 et 604 px, web à 390 et 1280 px ; aucun débordement horizontal web constaté.
- Les tests de régression couvrent les permissions retardées, l'arrêt à la durée maximale sans envoi, les réessais, les changements de session et la concurrence entre lecteurs. Les contrôles backend refusent aussi les WAV falsifiés, trop courts, trop longs ou tronqués.

Ces essais utilisent des sons synthétiques et ne captent pas de conversation personnelle. Le microphone et la lecture sur téléphone physique Android et sur iOS restent à vérifier pour cette livraison vocale ; le Samsung n'était pas détecté par USB au contrôle final. L'APK est compilé, son installation physique n'est donc pas confirmée. Les limites historiques relatives aux fournisseurs de notifications, aux appels Internet TLS/TURN et aux réservations restent applicables.

Cette branche conserve les changements des travaux successifs du workspace eCare+.
Les journaux techniques, captures, bases et fichiers privés restent locaux et ne sont pas publiés.

- Backend : dernière recette complète avant publication, 167 tests réussis, dont les tests de migrations PostgreSQL 18.6 (V1 à V29).
- Web : 29 tests réussis, un test réseau optionnel ignoré ; compilation TypeScript/Vite et lint réussis. La recette navigateur précédente couvre les sept onglets patient de 320 à 1440 px.
- Mobile : 145 tests existants réussis (2 tests réseau optionnels ignorés), puis un test de régression ajouté pour les commandes de préappel. Portrait imposé ; backend commun pour les données de suivi et la messagerie.
- Appels : transport LiveKit local vérifié, avec médias synthétiques entre navigateurs et émission audio/vidéo du Samsung vers le navigateur. Cela ne valide pas tous les appareils ni les réseaux Internet.

## Limites explicites

Le push externe attend un fournisseur. Les tests iOS et les appels sur Internet (TLS/TURN, arrière-plan) restent à réaliser. L'agenda personnel mobile ne constitue pas une réservation médicale. Les interfaces de démonstration historiques ne prouvent pas l'existence d'un service réel. Les bilans historiques gardent leur date et doivent être lus avec ce document.

## Rejouer les contrôles

Backend : Java 21, `./mvnw test` depuis `backend_ecare_plus`. Les tests PostgreSQL nécessitent une instance dédiée et les variables ECARE_TEST_POSTGRES_URL, ECARE_TEST_POSTGRES_USER, ECARE_TEST_POSTGRES_PASSWORD.
Web : `npm ci`, `npm test`, `npm run lint`, `npm run build`.
Mobile : `flutter pub get`, `flutter analyze`, `flutter test`, puis compilation avec `--dart-define=API_URL=...`.

## Mise à jour finale des gestes de notification

Backend : 169 tests réussis, aucun ignoré, avec PostgreSQL 18.6 et migration V30.
Mobile : 151 tests réussis, 2 tests réseau optionnels ignorés ; analyse sans anomalie.
Recette navigateur avec l'API réelle sur 604/360/320 px : toucher pour lecture, gauche pour supprimer, Annuler pour restaurer, droite pour archiver, onglet Archivées et désarchivage, catégorie/recherche/détail. Aucune erreur JavaScript observée.
