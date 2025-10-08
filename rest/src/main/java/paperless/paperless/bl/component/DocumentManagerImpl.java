package paperless.paperless.bl.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import paperless.paperless.bl.mapper.DocumentMapper;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.bl.model.BlUploadRequest;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.dal.repository.DocumentRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import paperless.paperless.messaging.OcrJobMessage;
import paperless.paperless.messaging.OcrProducer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;

@Component
public class DocumentManagerImpl implements DocumentManager {

    private static final Logger log = LoggerFactory.getLogger(DocumentManagerImpl.class);
    private final DocumentRepository repository;
    private final DocumentMapper mapper;
    private final Validator validator;
    private final OcrProducer ocrProducer;
    private final Path storageDir;

    public DocumentManagerImpl(DocumentRepository repository, DocumentMapper mapper, Validator validator, OcrProducer ocrProducer) throws IOException {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = validator;
        this.ocrProducer = ocrProducer;
        this.storageDir = Paths.get("storage");
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
            log.info("Created storage directory at {}", storageDir.toAbsolutePath());
        }
    }

    @Override
    public BlDocument upload(MultipartFile file) throws IOException {
        var filename = Path.of(file.getOriginalFilename() == null ? "" : file.getOriginalFilename())
                .getFileName().toString();

        // Validate incoming parameters at the BL boundary
        BlUploadRequest req = new BlUploadRequest(filename, file.getContentType(), file.getSize());
        var violations = validator.validate(req);
        if (!violations.isEmpty()) {
            log.warn("Validation failed for upload request: {}", violations);
            throw new ConstraintViolationException(violations);
        }

        // proceed with storing file and persisting metadata
        var target = storageDir.resolve(System.currentTimeMillis() + "_" + filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored file '{}' at {}", filename, target.toAbsolutePath());

        DocumentEntity entity = new DocumentEntity();
        entity.setFilename(req.getFilename());
        entity.setContentType(req.getContentType());
        entity.setSize(req.getSize());
        entity.setUploadedAt(OffsetDateTime.now());

        var saved = repository.save(entity);
        log.info("Persisted document metadata with id={}", saved.getId());

        // Send OCR job to RabbitMQ
        OcrJobMessage msg = new OcrJobMessage(saved.getId(), saved.getFilename(), saved.getContentType(), saved.getSize(), target.toAbsolutePath().toString(), saved.getUploadedAt());
        ocrProducer.send(msg);

        return mapper.toBl(saved);
    }

    @Override
    public BlDocument getById(long id) {
        return repository.findById(id).map(mapper::toBl).orElse(null);
    }
}