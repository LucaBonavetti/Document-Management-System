package paperless.paperless.dal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import paperless.paperless.dal.entity.DocumentTagEntity;

import java.util.List;

public interface DocumentTagRepository extends JpaRepository<DocumentTagEntity, Long> {

    List<DocumentTagEntity> findByDocument_Id(Long documentId);

    boolean existsByDocument_IdAndTag_Id(Long documentId, Long tagId);

    void deleteByDocument_IdAndTag_Id(Long documentId, Long tagId);

    void deleteByDocument_Id(Long documentId);
}
