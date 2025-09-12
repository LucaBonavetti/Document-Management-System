package paperless.paperless.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import paperless.paperless.domain.Document;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
}
