CREATE TABLE envoi_notification (
 id UUID PRIMARY KEY, version BIGINT NOT NULL, date_creation TIMESTAMPTZ NOT NULL, date_modification TIMESTAMPTZ NOT NULL,
 evenement_id UUID NOT NULL REFERENCES notification(id), destinataire_id UUID NOT NULL REFERENCES utilisateur(id), cible_id UUID NOT NULL,
 patient_id UUID REFERENCES patient(id), canal VARCHAR(20) NOT NULL, statut VARCHAR(30) NOT NULL,
 tentatives INTEGER NOT NULL, prochaine_tentative TIMESTAMPTZ NOT NULL, bail_expire_le TIMESTAMPTZ, reservation UUID,
 reference_fournisseur VARCHAR(200), dernier_code_erreur VARCHAR(80), UNIQUE(evenement_id,cible_id,canal)
);
CREATE INDEX idx_envoi_a_traiter ON envoi_notification(statut,prochaine_tentative);
