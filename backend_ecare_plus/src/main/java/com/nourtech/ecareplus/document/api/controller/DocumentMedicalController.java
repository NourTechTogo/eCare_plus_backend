package com.nourtech.ecareplus.document.api.controller;
import com.nourtech.ecareplus.auth.infrastructure.security.AccesVerifieParService;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.document.application.service.DocumentMedicalService;
import com.nourtech.ecareplus.document.application.service.DocumentMedicalService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.UUID;

@RestController @RequiredArgsConstructor @AccesVerifieParService @RequestMapping("/api/v1")
public class DocumentMedicalController {
    private final DocumentMedicalService service;private final AccesPatientService acces;
    @PostMapping(value={"/mobile/me/documents","/portail/patients/{patientId}/documents"},consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentVue deposer(@PathVariable(required=false) UUID patientId,@RequestPart MultipartFile fichier,@RequestParam String typeDocument,@RequestParam String titre,@RequestParam LocalDate dateDocument,@RequestParam String sourceRef){
        var acteur=acces.acteurId();return service.deposer(acteur,patientId==null?acteur:patientId,new DepotDocumentCommande(fichier,typeDocument,titre,dateDocument,sourceRef));
    }
    @GetMapping({"/mobile/me/documents","/portail/patients/{patientId}/documents"})
    public Page<DocumentVue> lister(@PathVariable(required=false) UUID patientId,Pageable page){var acteur=acces.acteurId();return service.lister(acteur,patientId==null?acteur:patientId,page);}
    @GetMapping({"/mobile/me/documents/{documentId}/contenu","/portail/patients/{patientId}/documents/{documentId}/contenu"})
    public ResponseEntity<byte[]> contenu(@PathVariable(required=false) UUID patientId,@PathVariable UUID documentId){var acteur=acces.acteurId();var resultat=service.contenu(acteur,patientId==null?acteur:patientId,documentId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(resultat.document().typeMime())).cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(resultat.document().nomOriginal(),java.nio.charset.StandardCharsets.UTF_8).build().toString()).body(resultat.octets());}
    @DeleteMapping("/mobile/me/documents/{documentId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable UUID documentId){service.supprimer(acces.acteurId(),documentId);}
}
