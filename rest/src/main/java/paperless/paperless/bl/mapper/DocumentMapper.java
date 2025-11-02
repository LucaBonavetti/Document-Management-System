package paperless.paperless.bl.mapper;

import org.mapstruct.Mapper;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.model.Document;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    // DAL <-> BL
    BlDocument toBl(DocumentEntity entity);
    DocumentEntity toEntity(BlDocument bl);

    // BL -> API DTO (used by controller)
    Document toApi(BlDocument bl);
    List<Document> toApiList(List<BlDocument> bls);
}
