package paperless.paperless.dal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import paperless.paperless.dal.entity.DocumentEntity;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    Optional<DocumentEntity> findByFilename(String filename);
}