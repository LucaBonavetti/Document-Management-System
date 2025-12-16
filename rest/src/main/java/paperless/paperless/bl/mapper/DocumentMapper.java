package paperless.paperless.bl.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.dal.entity.CategoryEntity;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.dal.entity.TagEntity;
import paperless.paperless.model.Document;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "category", source = "category", qualifiedByName = "catName")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "tagNames")
    BlDocument toBl(DocumentEntity entity);

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "tags", ignore = true)
    DocumentEntity toEntity(BlDocument bl);

    Document toApi(BlDocument bl);
    List<Document> toApiList(List<BlDocument> bls);

    @Named("catName")
    static String catName(CategoryEntity cat) {
        return cat == null ? null : cat.getName();
    }

    @Named("tagNames")
    static List<String> tagNames(Set<TagEntity> tags) {
        if (tags == null) return null;
        return tags.stream().map(TagEntity::getName).collect(Collectors.toList());
    }
}
