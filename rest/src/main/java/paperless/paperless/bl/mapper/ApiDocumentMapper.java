package paperless.paperless.bl.mapper;

import org.mapstruct.Mapper;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.model.Document;

@Mapper(componentModel = "spring")
public interface ApiDocumentMapper {
    Document toApi(BlDocument bl);
}