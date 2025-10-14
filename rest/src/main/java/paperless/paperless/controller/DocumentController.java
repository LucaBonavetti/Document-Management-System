package paperless.paperless.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import paperless.paperless.bl.service.DocumentService;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.model.Document;

import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(
            path = "/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Document> upload(@RequestPart("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            log.warn("Upload file is empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        BlDocument saved = documentService.saveDocument(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getBytes()
        );

        var location = "/api/documents/" + saved.getId();
        var api = new Document(saved.getId(), saved.getFilename(), saved.getContentType(),
                saved.getSize(), saved.getUploadedAt());

        log.info("Upload successful id={} -> {}", saved.getId(), location);

        return ResponseEntity
                .created(URI.create(location))
                .header(HttpHeaders.LOCATION, location)
                .body(api);
    }

    @GetMapping(path = "/documents/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Document> get(@PathVariable long id) {
        BlDocument doc = documentService.getById(id);
        if (doc == null) {
            log.info("Document with id {} not found", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        var api = new Document(doc.getId(), doc.getFilename(), doc.getContentType(),
                doc.getSize(), doc.getUploadedAt());
        return ResponseEntity.ok(api);
    }
}