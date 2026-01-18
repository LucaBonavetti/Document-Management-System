package paperless.paperless.bl.mapper;

import org.mapstruct.Mapper;
import paperless.paperless.bl.model.BlTag;
import paperless.paperless.dal.entity.TagEntity;
import paperless.paperless.model.Tag;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TagMapper {

    // DAL <-> BL
    BlTag toBl(TagEntity entity);
    TagEntity toEntity(BlTag bl);

    // BL -> API
    Tag toApi(BlTag bl);
    List<Tag> toApiList(List<BlTag> bls);
}
