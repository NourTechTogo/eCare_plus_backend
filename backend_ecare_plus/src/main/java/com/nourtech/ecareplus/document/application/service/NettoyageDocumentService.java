package com.nourtech.ecareplus.document.application.service;
import com.nourtech.ecareplus.document.application.port.StockagePrive;
import com.nourtech.ecareplus.document.infrastructure.persistence.DocumentMedicalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class NettoyageDocumentService {
    private final DocumentMedicalRepository documents;private final StockagePrive stockage;
    private final com.nourtech.ecareplus.messaging.infrastructure.persistence.PieceJointeRepository pieces;
    @Transactional public int nettoyer(){int nombre=0;for(var document:documents.findTop100BySupprimeLeIsNotNullAndFichierSupprimeFalse()){
        try{stockage.supprimer(document.getCleStockage());document.fichierSupprime();nombre++;}catch(org.springframework.web.server.ResponseStatusException indisponible){/* Tombstone retained for the next retry. */}
    }
        for(var cle:stockage.candidatsNettoyage(java.time.Instant.now().minusSeconds(86400)))if(!documents.existsByCleStockage(cle)&&!pieces.existsByCleStockage(cle)){
            try{stockage.supprimer(cle);nombre++;}catch(org.springframework.web.server.ResponseStatusException indisponible){/* Keep orphan for next cleanup attempt. */}
        }
        return nombre;}
}
