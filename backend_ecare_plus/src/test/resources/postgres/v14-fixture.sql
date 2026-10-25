-- Fictitious legacy domain rows, installed after V14 and preserved through later migrations.
INSERT INTO structure_sante(id,version,date_creation,date_modification,nom,type_structure,adresse,ville,region,telephone,statut,numero_autorisation,date_adhesion)
VALUES ('00000000-0000-0000-0000-000000000001',7,'2026-01-01T00:00:00Z','2026-01-01T00:00:00Z','Fixture','CLINIQUE','Rue fictive','Lome','Maritime','FIXTURE-CENTRE','ACTIVE','FIXTURE-AUTORISATION','2026-01-01');
INSERT INTO utilisateur(id,version,date_creation,date_modification,nom,prenoms,sexe,telephone,email,mot_de_passe_hash,langue_preferee,statut_compte)
VALUES ('00000000-0000-0000-0000-000000000002',2,'2026-01-01T00:00:00Z','2026-01-01T00:00:00Z','Fixture','Patient','NON_RENSEIGNE','FIXTURE-PATIENT','fixture-patient@example.test','non-authentifiable-fixture','fr','ACTIF'),
('00000000-0000-0000-0000-000000000003',3,'2026-01-01T00:00:00Z','2026-01-01T00:00:00Z','Fixture','Medecin','NON_RENSEIGNE','FIXTURE-MEDECIN','fixture-medecin@example.test','non-authentifiable-fixture','fr','ACTIF');
INSERT INTO patient(id,numero_patient,mode_connexion_prefere,date_inscription,statut_suivi,ville,pathologie)
VALUES ('00000000-0000-0000-0000-000000000002','FIXTURE-P001','MOT_DE_PASSE','2026-01-01','ACTIF','Lome','diabete');
INSERT INTO professionnel_sante(id,numero_professionnel,specialite,fonction,statut_professionnel,date_prise_fonction,structure_id)
VALUES ('00000000-0000-0000-0000-000000000003','FIXTURE-M001','Generaliste','Suivi','ACTIF','2026-01-01','00000000-0000-0000-0000-000000000001');
INSERT INTO medecin(id,numero_ordre,domaine_specialisation) VALUES ('00000000-0000-0000-0000-000000000003','FIXTURE-ORDRE','Generaliste');
INSERT INTO affectation_patient(id,version,date_creation,date_modification,patient_id,professionnel_id,structure_id,date_debut,role_professionnel,statut,professionnel_principal)
VALUES ('00000000-0000-0000-0000-000000000004',4,'2026-01-01T00:00:00Z','2026-01-01T00:00:00Z','00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000003','00000000-0000-0000-0000-000000000001','2026-01-01','MEDECIN_REFERENT','ACTIVE',true);
INSERT INTO dossier_suivi(id,version,date_creation,date_modification,patient_id,structure_id,date_ouverture,statut,date_derniere_mise_ajour)
VALUES ('00000000-0000-0000-0000-000000000005',5,'2026-01-01T00:00:00Z','2026-01-01T00:00:00Z','00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000001','2026-01-01','ACTIF','2026-01-01T00:00:00Z');
INSERT INTO traitement(id,version,date_creation,date_modification,dossier_id,libelle,date_debut,statut)
VALUES ('00000000-0000-0000-0000-000000000006',6,'2026-01-01T00:00:00Z','2026-01-01T00:00:00Z','00000000-0000-0000-0000-000000000005','Traitement historique','2026-01-01','ACTIF');
INSERT INTO medicament(id,version,date_creation,date_modification,nom_commercial,denomination,forme,dosage,unite_dosage)
VALUES ('00000000-0000-0000-0000-000000000007',1,'2026-01-01T00:00:00Z','2026-01-01T00:00:00Z','Exemple','Exemple','comprime','10','mg');
INSERT INTO prise_medicament(id,version,date_creation,date_modification,traitement_id,medicament_id,quantite,unite,frequence,heure_prevue,voie_administration,actif)
VALUES ('00000000-0000-0000-0000-000000000008',2,'2026-01-01T00:00:00Z','2026-01-01T00:00:00Z','00000000-0000-0000-0000-000000000006','00000000-0000-0000-0000-000000000007',1,'comprime','QUOTIDIEN','08:00','orale',true);
INSERT INTO confirmation_prise(id,version,date_creation,date_modification,prise_medicament_id,date_prevue,date_confirmation,statut,commentaire)
VALUES ('00000000-0000-0000-0000-000000000009',3,'2026-01-01T00:00:00Z','2026-01-01T00:00:00Z','00000000-0000-0000-0000-000000000008','2026-01-01T08:00:00Z','2026-01-01T08:05:00Z','PRISE','Historique conserve'),
('00000000-0000-0000-0000-000000000012',4,'2026-01-02T00:00:00Z','2026-01-02T00:00:00Z','00000000-0000-0000-0000-000000000008','2026-01-02T09:00:00Z',null,'REPORTEE','Date initiale avant report inconnue');
INSERT INTO mesure_sante(id,version,date_creation,date_modification,patient_id,dossier_id,type_mesure,valeur_principale,unite,date_mesure,source,statut_synchronisation,valeur_confirmee,source_ref)
VALUES ('00000000-0000-0000-0000-000000000010',2,'2026-01-01T00:00:00Z','2026-01-01T00:00:00Z','00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000005','GLYCEMIE',110,'mg/dL','2026-01-01T07:00:00Z','PATIENT','SYNCHRONISEE',true,'fixture-source');

