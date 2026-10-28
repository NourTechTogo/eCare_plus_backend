ALTER TABLE rendez_vous ADD COLUMN personnel BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE rendez_vous ADD COLUMN fuseau VARCHAR(80);
ALTER TABLE rendez_vous ADD COLUMN professionnel_nom VARCHAR(200);
ALTER TABLE rendez_vous ADD COLUMN rappel_minutes INTEGER;
ALTER TABLE rendez_vous ADD COLUMN rappel_id UUID REFERENCES rappel(id);
ALTER TABLE rendez_vous ADD CONSTRAINT ck_agenda_rappel_minutes CHECK (rappel_minutes IS NULL OR rappel_minutes BETWEEN 0 AND 10080);
ALTER TABLE rendez_vous ADD CONSTRAINT ck_agenda_personnel_fuseau CHECK (NOT personnel OR fuseau IS NOT NULL);
CREATE UNIQUE INDEX uk_agenda_rappel ON rendez_vous(rappel_id);
CREATE INDEX idx_agenda_patient_date ON rendez_vous(patient_id,personnel,date_heure,id);

-- Immutable provenance keeps historical reminders private after a schedule replacement.
-- Opaque immutable provenance, deliberately not a reverse FK: avoid a restore dependency cycle.
ALTER TABLE rappel ADD COLUMN agenda_personnel_id UUID;
CREATE INDEX idx_rappel_agenda ON rappel(agenda_personnel_id);
-- The due-date index idx_rappel_echeance already exists since V8.

CREATE TABLE operation_agenda_patient (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    patient_id UUID NOT NULL REFERENCES patient(id),
    source_ref UUID NOT NULL,
    empreinte VARCHAR(64) NOT NULL,
    resultat TEXT NOT NULL,
    CONSTRAINT uk_operation_agenda_patient UNIQUE(patient_id,source_ref)
);
