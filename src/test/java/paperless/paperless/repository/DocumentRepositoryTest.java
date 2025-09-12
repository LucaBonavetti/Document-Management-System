package paperless.paperless.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import paperless.paperless.domain.Document;

import java.util.Optional;

@SpringBootTest
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository repository;

    @Test
    void saveAndFindById() {
        Document doc = new Document("Test title", "Test content");
        Document saved = repository.save(doc);

        Optional<Document> found = repository.findById(saved.getId());
        assert(found).isPresent();
        assert(found.get().getTitle()).equals("Test title");
        assert(found.get().getContentText()).equals("Test content");
    }
}
