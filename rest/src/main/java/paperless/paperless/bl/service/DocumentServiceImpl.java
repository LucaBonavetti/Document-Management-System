package paperless.paperless.bl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import paperless.paperless.bl.mapper.DocumentMapper;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.bl.model.BlUploadRequest;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.dal.repository.DocumentRepository;
import paperless.paperless.infrastructure.FileStorageService;
import paperless.paperless.messaging.OcrJobMessage;
import paperless.paperless.messaging.OcrProducer;

import java.io.IOException;
import java.time.OffsetDateTime;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final DocumentRepository repository;
    private final DocumentMapper mapper;
    private final Validator validator;
    private final FileStorageService fileStorageService;
    private final OcrProducer ocrProducer;

    public DocumentServiceImpl(DocumentRepository repository, DocumentMapper mapper, Validator validator,
                               FileStorageService fileStorageService, OcrProducer ocrProducer) {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = validator;
        this.fileStorageService = fileStorageService;
        this.ocrProducer = ocrProducer;
    }

    @Override
    public BlDocument saveDocument(String filename, String contentType, long size, byte[] bytes) throws IOException {
        var req = new BlUploadRequest(filename, contentType, size);
        var violations = validator.validate(req);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);

        // Upload to MinIO
        String objectKey = fileStorageService.uploadFile(filename, bytes);
        log.info("File '{}' uploaded to MinIO with key '{}'", filename, objectKey);

        // Save metadata
        DocumentEntity entity = new DocumentEntity();
        entity.setFilename(req.getFilename());
        entity.setContentType(req.getContentType());
        entity.setSize(req.getSize());
        entity.setUploadedAt(OffsetDateTime.now());
        entity.setObjectKey(objectKey);

        var saved = repository.save(entity);

        // Send OCR job
        var msg = new OcrJobMessage(saved.getId(), saved.getFilename(), saved.getContentType(), saved.getSize(),
                objectKey, saved.getUploadedAt());
        ocrProducer.send(msg);

        return mapper.toBl(saved);
    }

    @Override
    public BlDocument getById(long id) {
        return repository.findById(id).map(mapper::toBl).orElse(null);
    }
}