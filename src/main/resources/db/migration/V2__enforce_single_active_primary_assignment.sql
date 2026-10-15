CREATE UNIQUE INDEX uk_affectation_principal_active
    ON affectation_patient(patient_id, structure_id, role_professionnel)
    WHERE statut = 'ACTIVE' AND professionnel_principal = TRUE;
