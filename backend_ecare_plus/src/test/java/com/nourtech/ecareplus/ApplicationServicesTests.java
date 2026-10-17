package com.nourtech.ecareplus;

import com.nourtech.ecareplus.identity.application.command.CreerAdministrateurStructureCommand;
import com.nourtech.ecareplus.identity.application.command.CreerInfirmierCommand;
import com.nourtech.ecareplus.identity.application.command.CreerMedecinCommand;
import com.nourtech.ecareplus.identity.application.command.CreerPatientCommand;
import com.nourtech.ecareplus.identity.application.service.GestionAdministrateurStructureService;
import com.nourtech.ecareplus.identity.application.service.GestionPatientService;
import com.nourtech.ecareplus.identity.application.service.GestionProfessionnelSanteService;
import com.nourtech.ecareplus.identity.domain.entity.AdministrateurStructure;
import com.nourtech.ecareplus.identity.domain.entity.Infirmier;
import com.nourtech.ecareplus.identity.domain.entity.Medecin;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import com.nourtech.ecareplus.organization.application.command.CreerStructureSanteCommand;
import com.nourtech.ecareplus.organization.application.service.GestionStructureService;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@Transactional
class ApplicationServicesTests {

    @Autowired
    private GestionStructureService structureService;

    @Autowired
    private GestionPatientService patientService;

    @Autowired
    private GestionProfessionnelSanteService professionnelService;

    @Autowired
    private GestionAdministrateurStructureService administrateurService;

    @Test
    void creeLesPrincipauxProfilsParLesServicesApplicatifs() {
        StructureSante structure = structureService.creer(new CreerStructureSanteCommand(
                "Clinique des services",
                "CLINIQUE",
                "10 avenue eCare",
                "Lomé",
                "Maritime",
                "+22822000100",
                "structure-services@example.test",
                "AUT-SERVICES-001"
        ));
        Patient patient = patientService.creer(patientCommand());
        Medecin medecin = professionnelService.creerMedecin(new CreerMedecinCommand(
                "PRO-SERVICES-001",
                "ORDRE-SERVICES-001",
                "Mensah",
                "Kossi",
                Sexe.MASCULIN,
                LocalDate.of(1978, 1, 5),
                "+22890000101",
                "medecin-services@example.test",
                "hash",
                "fr",
                "Médecine interne",
                "Diabétologie",
                LocalDate.of(2022, 1, 1),
                structure.getId()
        ));
        Infirmier infirmier = professionnelService.creerInfirmier(new CreerInfirmierCommand(
                "PRO-SERVICES-002",
                "Atakpa",
                "Afi",
                Sexe.FEMININ,
                LocalDate.of(1990, 6, 12),
                "+22890000102",
                "infirmier-services@example.test",
                "hash",
                "fr",
                "Soins généraux",
                "Télésuivi",
                "REFERENT",
                LocalDate.of(2023, 2, 1),
                structure.getId()
        ));
        AdministrateurStructure administrateur = administrateurService.creer(
                new CreerAdministrateurStructureCommand(
                        "Agbo",
                        "Esi",
                        Sexe.FEMININ,
                        LocalDate.of(1988, 3, 18),
                        "+22890000103",
                        "admin-services@example.test",
                        "hash",
                        "fr",
                        "Responsable e-santé",
                        "STRUCTURE",
                        structure.getId()
                )
        );

        assertThat(patient.getId()).isNotNull();
        assertThat(medecin.getStructure().memeIdentiteQue(structure)).isTrue();
        assertThat(infirmier.getStructure().memeIdentiteQue(structure)).isTrue();
        assertThat(administrateur.getStructure().memeIdentiteQue(structure)).isTrue();

        assertThatExceptionOfType(ConflitMetierException.class)
                .isThrownBy(() -> patientService.creer(patientCommand()));
    }

    private static CreerPatientCommand patientCommand() {
        return new CreerPatientCommand(
                "PAT-SERVICES-001",
                "Amavi",
                "Kayi",
                Sexe.FEMININ,
                LocalDate.of(1985, 7, 20),
                "+22890000100",
                "patient-services@example.test",
                "hash",
                "fr"
        );
    }
}
