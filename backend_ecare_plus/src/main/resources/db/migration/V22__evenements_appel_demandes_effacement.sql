CREATE TABLE evenement_appel (
 id UUID PRIMARY KEY, version BIGINT NOT NULL, date_creation TIMESTAMPTZ NOT NULL, date_modification TIMESTAMPTZ NOT NULL,
 appel_id UUID NOT NULL REFERENCES session_appel(id), identifiant_externe VARCHAR(128) NOT NULL UNIQUE, type_evenement VARCHAR(40) NOT NULL
);
CREATE TABLE demande_effacement (
 id UUID PRIMARY KEY, version BIGINT NOT NULL, date_creation TIMESTAMPTZ NOT NULL, date_modification TIMESTAMPTZ NOT NULL,
 utilisateur_id UUID NOT NULL REFERENCES utilisateur(id), statut VARCHAR(30) NOT NULL, motif VARCHAR(1000)
);
CREATE UNIQUE INDEX uq_demande_effacement_en_attente ON demande_effacement(utilisateur_id) WHERE statut='EN_ATTENTE';
