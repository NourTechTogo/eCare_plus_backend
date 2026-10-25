package com.nourtech.ecareplus.messaging.api.controller;
import com.nourtech.ecareplus.messaging.application.service.PieceJointeService;
import com.nourtech.ecareplus.messaging.application.service.PieceJointeService.*;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.auth.infrastructure.security.AccesVerifieParService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import java.util.UUID;
@RestController @RequiredArgsConstructor @AccesVerifieParService @RequestMapping("/api/v1/mobile/conversations/{conversationId}/pieces-jointes")
public class PieceJointeController {
    private final PieceJointeService service;private final AccesPatientService acces;
    @GetMapping public org.springframework.data.domain.Page<PieceVue> lister(@PathVariable UUID conversationId,org.springframework.data.domain.Pageable page){return service.lister(acces.acteurId(),conversationId,page);}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public PieceVue deposer(@PathVariable UUID conversationId,@RequestPart MultipartFile fichier,@RequestParam String sourceRef){return service.deposer(acces.acteurId(),conversationId,fichier,sourceRef);}
    @GetMapping("/{id}/contenu") public ResponseEntity<byte[]> contenu(@PathVariable UUID conversationId,@PathVariable UUID id){var c=service.contenu(acces.acteurId(),conversationId,id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(c.piece().typeMime())).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(c.piece().nomOriginal(),java.nio.charset.StandardCharsets.UTF_8).build().toString()).body(c.octets());}
}
