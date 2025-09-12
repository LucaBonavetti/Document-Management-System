package paperless.paperless.service;

import org.springframework.stereotype.Service;
import paperless.paperless.domain.Document;
import paperless.paperless.repository.DocumentRepository;
import paperless.paperless.web.DocumentNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository repository;

    public DocumentService(DocumentRepository repository) {
        this.repository = repository;
    }

    public Document create(String title, String contentText) {
        return repository.save(new Document(title, contentText));
    }

    public List<Document> list() {
        return repository.findAll();
    }

    public Document get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
    }

    public Document update(UUID id, String title, String contentText) {
        Document document = repository.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
        if (title != null) document.setTitle(title);
        if (contentText != null) document.setContentText(contentText);
        return repository.save(document);
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) throw new DocumentNotFoundException(id);
        repository.deleteById(id);
    }
}
