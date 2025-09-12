package paperless.paperless.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import paperless.paperless.domain.Document;
import paperless.paperless.service.DocumentService;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Document> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public Document get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<Document> create(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        String contentText = body.get("contentText");
        Document created = service.create(title, contentText);
        return ResponseEntity.created(URI.create("/api/documents/" + created.getId())).body(created);
    }

    @PutMapping
    public Document update(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String title = body.get("title");
        String contentText = body.get("contentText");
        return service.update(id, title, contentText);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
