package paperless.paperless.bl.service;

import co.elastic.clients.elasticsearch._types.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.dal.repository.DocumentRepository;
import paperless.paperless.infrastructure.FileStorageService;
import paperless.paperless.messaging.OcrResultMessage;
import paperless.paperless.search.SearchIndexService;
import paperless.paperless.search.dto.IndexedDocument;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@Service
public class DocumentIndexingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexingService.class);

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final SearchIndexService searchIndexService;

    public DocumentIndexingService(DocumentRepository documentRepository,
                                   FileStorageService fileStorageService,
                                   SearchIndexService searchIndexService) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.searchIndexService = searchIndexService;
    }

    @Transactional
    public void handleOcrResult(OcrResultMessage msg) {
        Long id = msg.getDocumentId();
        if (id == null) {
            log.warn("OCR result without documentId. Ignoring.");
            return;
        }
        if (msg.getTextKey() == null || msg.getTextKey().isBlank()) {
            log.warn("OCR result documentId={} without textKey. Ignoring.", id);
            return;
        }

        DocumentEntity doc = documentRepository.findById(id).orElse(null);
        if (doc == null) {
            log.warn("OCR result for unknown documentId={}. Ignoring.", id);
            return;
        }

        String ocrText;
        try {
            byte[] bytes = fileStorageService.downloadFile(msg.getTextKey());
            ocrText = new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to download OCR text from MinIO for documentId={} textKey='{}': {}",
                    id, msg.getTextKey(), e.toString(), e);
            return;
        }

        IndexedDocument indexed = new IndexedDocument();
        indexed.setId(doc.getId());
        indexed.setFilename(doc.getFilename());
        indexed.setUploadedAt(doc.getUploadedAt());
        indexed.setContent(ocrText);

        try {
            Result r = searchIndexService.indexDocument(indexed);
            log.info("Indexed documentId={} into Elasticsearch (result={})", id, r);
        } catch (Exception e) {
            log.error("Failed to index documentId={} into Elasticsearch: {}", id, e.toString(), e);
            return;
        }

        doc.setOcrTextKey(msg.getTextKey());
        doc.setOcrProcessedAt(msg.getProcessedAt() != null ? msg.getProcessedAt() : OffsetDateTime.now());
        doc.setOcrIndexedAt(OffsetDateTime.now());
        documentRepository.save(doc);
    }
}
