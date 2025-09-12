package paperless.paperless.repository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import paperless.paperless.domain.Document;

import java.util.Optional;

@SpringBootTest
class DocumentRepositoryTest {
    private final DocumentRepository repository;

    DocumentRepositoryTest(DocumentRepository repository) {
        this.repository = repository;
    }

    @Test
    void saveAndFind() {
        Document saved = repository.save(new Document("title", "content"));
        Optional<Document> found = repository.findById(saved.getId());
        assert found.isPresent();
        assert found.get().getTitle().equals("title");
        assert found.get().getContentText().equals("content");
    }

}
