package paperless.paperless.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import paperless.paperless.bl.mapper.DocumentMapper;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.bl.model.BlUploadRequest;
import paperless.paperless.bl.service.DocumentService;
import paperless.paperless.model.Document;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentMapper mapper;

    // 50 MB soft guard (in addition to nginx client_max_body_size)
    private static final long MAX_UPLOAD_BYTES = DataSize.ofMegabytes(50).toBytes();

    public DocumentController(DocumentService documentService, DocumentMapper mapper) {
        this.documentService = documentService;
        this.mapper = mapper;
    }

    @PostMapping(
            path = "/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Document> upload(@RequestPart("file") MultipartFile file,
                                           UriComponentsBuilder uriBuilder) throws Exception {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty.");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("File exceeds maximum allowed size of 50 MB.");
        }

        BlUploadRequest req = new BlUploadRequest(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );

        BlDocument saved = documentService.saveDocument(req, file.getBytes());
        Document dto = mapper.toApi(saved);

        URI location = uriBuilder
                .path("/api/documents/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .header(HttpHeaders.LOCATION, location.toString())
                .body(dto);
    }

    @GetMapping(path = "/documents/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Document> getById(@PathVariable("id") Long id) {
        BlDocument bl = documentService.getById(id);
        if (bl == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.toApi(bl));
    }

    @GetMapping(path = "/documents", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Document>> list(@RequestParam(name = "limit", defaultValue = "10") int limit) {
        List<BlDocument> items = documentService.getRecent(limit);
        return ResponseEntity.ok(mapper.toApiList(items));
    }
}