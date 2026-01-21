package paperless.paperless.bl.service;

import co.elastic.clients.elasticsearch._types.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.dal.entity.DocumentTagEntity;
import paperless.paperless.dal.repository.DocumentRepository;
import paperless.paperless.dal.repository.DocumentTagRepository;
import paperless.paperless.infrastructure.FileStorageService;
import paperless.paperless.messaging.OcrResultMessage;
import paperless.paperless.search.SearchIndexService;
import paperless.paperless.search.dto.IndexedDocument;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DocumentIndexingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexingService.class);

    private final DocumentRepository documentRepository;
    private final DocumentTagRepository documentTagRepository;
    private final FileStorageService fileStorageService;
    private final SearchIndexService searchIndexService;

    public DocumentIndexingService(DocumentRepository documentRepository,
                                   DocumentTagRepository documentTagRepository,
                                   FileStorageService fileStorageService,
                                   SearchIndexService searchIndexService) {
        this.documentRepository = documentRepository;
        this.documentTagRepository = documentTagRepository;
        this.fileStorageService = fileStorageService;
        this.searchIndexService = searchIndexService;
    }

    @Transactional
    public void handleOcrResult(OcrResultMessage msg) {
        if (msg == null || msg.getDocumentId() == null) {
            log.warn("OCR result message is null or has no documentId.");
            return;
        }
        if (msg.getTextKey() == null || msg.getTextKey().isBlank()) {
            log.warn("OCR result documentId={} has no textKey.", msg.getDocumentId());
            return;
        }

        DocumentEntity doc = documentRepository.findById(msg.getDocumentId()).orElse(null);
        if (doc == null) {
            log.warn("OCR result for unknown documentId={}. Ignoring.", msg.getDocumentId());
            return;
        }

        String ocrText;
        try {
            byte[] bytes = fileStorageService.downloadFile(msg.getTextKey());
            ocrText = new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to download OCR text from MinIO for documentId={} textKey='{}': {}",
                    msg.getDocumentId(), msg.getTextKey(), e.toString(), e);
            return;
        }

        List<String> tags = loadTagNames(doc.getId());

        IndexedDocument indexed = new IndexedDocument();
        indexed.setId(doc.getId());
        indexed.setFilename(doc.getFilename());
        indexed.setUploadedAt(doc.getUploadedAt());
        indexed.setContent(ocrText);
        indexed.setTags(tags);

        try {
            Result r = searchIndexService.indexDocument(indexed);
            log.info("Indexed documentId={} into Elasticsearch (result={})", doc.getId(), r);
        } catch (Exception e) {
            log.error("Failed to index documentId={} into Elasticsearch: {}", doc.getId(), e.toString(), e);
            return;
        }
        try {
            doc.setOcrTextKey(msg.getTextKey());
            doc.setOcrProcessedAt(msg.getProcessedAt() != null ? msg.getProcessedAt() : OffsetDateTime.now());
            doc.setOcrIndexedAt(OffsetDateTime.now());
            documentRepository.save(doc);
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public void reindexDocument(Long documentId) {
        if (documentId == null) return;

        DocumentEntity doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) return;

        // Only reindex when we already have OCR text stored
        String textKey;
        try {
            textKey = doc.getOcrTextKey();
        } catch (Exception e) {
            return;
        }

        if (textKey == null || textKey.isBlank()) {
            return;
        }

        String ocrText;
        try {
            byte[] bytes = fileStorageService.downloadFile(textKey);
            ocrText = new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Reindex failed: cannot read OCR text for documentId={} key='{}': {}",
                    documentId, textKey, e.toString());
            return;
        }

        List<String> tags = loadTagNames(documentId);

        IndexedDocument indexed = new IndexedDocument();
        indexed.setId(doc.getId());
        indexed.setFilename(doc.getFilename());
        indexed.setUploadedAt(doc.getUploadedAt());
        indexed.setContent(ocrText);
        indexed.setTags(tags);

        try {
            Result r = searchIndexService.indexDocument(indexed);
            log.info("Re-indexed documentId={} after tag change (result={})", doc.getId(), r);
        } catch (Exception e) {
            log.warn("Reindex failed for documentId={}: {}", documentId, e.toString());
            return;
        }

        try {
            doc.setOcrIndexedAt(OffsetDateTime.now());
            documentRepository.save(doc);
        } catch (Exception ignored) {
        }
    }

    private List<String> loadTagNames(Long documentId) {
        List<DocumentTagEntity> links = documentTagRepository.findByDocument_Id(documentId);

        // deterministic ordering helps tests + debugging
        return links.stream()
                .map(DocumentTagEntity::getTag)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(t -> t.getName() == null ? "" : t.getName()))
                .map(t -> t.getName() == null ? "" : t.getName())
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }
}