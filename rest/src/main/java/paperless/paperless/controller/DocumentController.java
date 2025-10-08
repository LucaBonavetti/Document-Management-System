package paperless.paperless.controller;

import org.antlr.v4.runtime.misc.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import paperless.paperless.bl.component.DocumentManager;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.model.Document;

@RestController
@RequestMapping("/api")
public class DocumentController {
    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentManager manager;

    public DocumentController(DocumentManager manager) {
        this.manager = manager;
    }

    @CrossOrigin
    @PostMapping(path = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Document> upload(@RequestPart("file") @NotNull MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            log.warn("Upload file is empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        BlDocument saved = manager.upload(file);
        var location = "/api/documents/" + saved.getId();
        var api = new Document(saved.getId(), saved.getFilename(), saved.getContentType(), saved.getSize(), saved.getUploadedAt());
        log.info("Upload successful id={} -> {}", saved.getId(), location);
        return ResponseEntity.created(java.net.URI.create(location))
                .header(HttpHeaders.LOCATION, location)
                .body(api);
    }

    @CrossOrigin
    @GetMapping(path = "/documents/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Document> get(@PathVariable long id) {
        BlDocument doc = manager.getById(id);
        if (doc == null) {
            log.info("Document with id {} not found", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        var api = new Document(doc.getId(), doc.getFilename(), doc.getContentType(), doc.getSize(), doc.getUploadedAt());
        return ResponseEntity.ok(api);
    }
}