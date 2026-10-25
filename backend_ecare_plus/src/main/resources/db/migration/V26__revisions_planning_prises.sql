CREATE TABLE revision_planning_prise (
 id UUID PRIMARY KEY, version BIGINT NOT NULL, date_creation TIMESTAMPTZ NOT NULL, date_modification TIMESTAMPTZ NOT NULL,
 prise_id UUID NOT NULL REFERENCES prise_medicament(id), valide_depuis TIMESTAMPTZ NOT NULL, valide_jusqua TIMESTAMPTZ, contenu TEXT NOT NULL
);
CREATE INDEX idx_revision_prise ON revision_planning_prise(prise_id,valide_depuis);
CREATE UNIQUE INDEX uq_revision_prise_active ON revision_planning_prise(prise_id) WHERE valide_jusqua IS NULL;
