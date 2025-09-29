package paperless.paperless.bl.mapper;

import org.mapstruct.Mapper;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.dal.entity.DocumentEntity;

@Mapper(componentModel = "spring")
public interface DocumentMapper {
    DocumentEntity toEntity(BlDocument bl);
    BlDocument toBl(DocumentEntity entity);
}
