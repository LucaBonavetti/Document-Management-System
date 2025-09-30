package paperless.paperless.bl.component;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import paperless.paperless.bl.mapper.DocumentMapper;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.bl.model.BlUploadRequest;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.dal.repository.DocumentRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;

@Component
public class DocumentManagerImpl implements DocumentManager {

    private final DocumentRepository repository;
    private final DocumentMapper mapper;
    private final Validator validator;
    private final Path storageDir;

    public DocumentManagerImpl(DocumentRepository repository, DocumentMapper mapper, Validator validator) throws IOException {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = validator;
        this.storageDir = Paths.get("storage");
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
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
            throw new ConstraintViolationException(violations);
        }

        // proceed with storing file and persisting metadata
        var target = storageDir.resolve(System.currentTimeMillis() + "_" + filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        DocumentEntity entity = new DocumentEntity();
        entity.setFilename(req.getFilename());
        entity.setContentType(req.getContentType());
        entity.setSize(req.getSize());
        entity.setUploadedAt(OffsetDateTime.now());

        var saved = repository.save(entity);
        return mapper.toBl(saved);
    }

    @Override
    public BlDocument getById(long id) {
        return repository.findById(id).map(mapper::toBl).orElse(null);
    }
}