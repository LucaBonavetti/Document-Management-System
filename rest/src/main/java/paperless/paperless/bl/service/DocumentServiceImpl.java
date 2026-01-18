package paperless.paperless.bl.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import paperless.paperless.bl.mapper.DocumentMapper;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.bl.model.BlUploadRequest;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.dal.repository.DocumentRepository;
import paperless.paperless.infrastructure.FileStorageService;
import paperless.paperless.messaging.OcrJobMessage;
import paperless.paperless.messaging.OcrProducer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final OcrProducer ocrProducer;
    private final DocumentMapper mapper;
    private final Validator validator;

    public DocumentServiceImpl(DocumentRepository documentRepository,
                               FileStorageService fileStorageService,
                               OcrProducer ocrProducer,
                               DocumentMapper mapper,
                               Validator validator) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.ocrProducer = ocrProducer;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Override
    @Transactional
    public BlDocument saveDocument(BlUploadRequest request, byte[] data) throws Exception {
        Set<jakarta.validation.ConstraintViolation<BlUploadRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String validationMsg = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", "));
            throw new IllegalArgumentException(validationMsg);
        }

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("file must not be empty");
        }

        // Upload to MinIO
        String objectKey = fileStorageService.uploadFile(request.getFilename(), data);
        log.info("File '{}' uploaded to MinIO with key '{}'", request.getFilename(), objectKey);

        DocumentEntity entity = new DocumentEntity();
        entity.setFilename(request.getFilename());
        entity.setContentType(request.getContentType());
        entity.setSize(request.getSize() > 0 ? request.getSize() : data.length);
        entity.setUploadedAt(OffsetDateTime.now());
        entity.setObjectKey(objectKey);

        DocumentEntity saved = documentRepository.save(entity);

        OcrJobMessage job = new OcrJobMessage(
                saved.getId(),
                saved.getFilename(),
                saved.getContentType(),
                saved.getSize(),
                objectKey,
                saved.getUploadedAt()
        );
        ocrProducer.send(job);

        return mapper.toBl(saved);
    }

    @Override
    public BlDocument getById(Long id) {
        return documentRepository.findById(id)
                .map(mapper::toBl)
                .orElse(null);
    }

    @Override
    public List<BlDocument> getRecent(int limit) {
        int pageSize = Math.max(1, Math.min(limit, 100));
        var page = documentRepository.findAll(
                PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "uploadedAt"))
        );
        return page.stream().map(mapper::toBl).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateSummary(Long documentId, String summary) {
        documentRepository.findById(documentId).ifPresent(ent -> {
            ent.setSummary(summary);
            documentRepository.save(ent);
            log.info("Summary updated for document {}", documentId);
        });
    }
}