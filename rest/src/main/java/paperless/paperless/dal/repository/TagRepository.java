package paperless.paperless.dal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import paperless.paperless.dal.entity.TagEntity;

import java.util.Optional;

public interface TagRepository extends JpaRepository<TagEntity, Long> {
    Optional<TagEntity> findByName(String name);
    boolean existsByName(String name);
}
