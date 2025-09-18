package paperless.paperless.bl.mapper;

import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.dal.entity.DocumentEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentMapperTest {

    private final DocumentMapper mapper = Mappers.getMapper(DocumentMapper.class);

    @Test
    void toBl_mapsAllFields() {
        DocumentEntity e = new DocumentEntity();
        e.setId(3L);
        e.setFilename("x.txt");
        e.setContentType("text/plain");
        e.setSize(10L);
        e.setUploadedAt(OffsetDateTime.parse("2024-03-03T03:03:03Z"));

        BlDocument bl = mapper.toBl(e);

        assertThat(bl).isNotNull();
        assertThat(bl.getId()).isEqualTo(3L);
        assertThat(bl.getFilename()).isEqualTo("x.txt");
        assertThat(bl.getContentType()).isEqualTo("text/plain");
        assertThat(bl.getSize()).isEqualTo(10L);
        assertThat(bl.getUploadedAt()).isEqualTo(OffsetDateTime.parse("2024-03-03T03:03:03Z"));
    }

    @Test
    void toEntity_mapsAllFields() {
        BlDocument bl = new BlDocument();
        bl.setId(9L);
        bl.setFilename("y.pdf");
        bl.setContentType("application/pdf");
        bl.setSize(2048L);
        bl.setUploadedAt(OffsetDateTime.parse("2024-04-04T04:04:04Z"));

        DocumentEntity e = mapper.toEntity(bl);

        assertThat(e).isNotNull();
        assertThat(e.getId()).isEqualTo(9L);
        assertThat(e.getFilename()).isEqualTo("y.pdf");
        assertThat(e.getContentType()).isEqualTo("application/pdf");
        assertThat(e.getSize()).isEqualTo(2048L);
        assertThat(e.getUploadedAt()).isEqualTo(OffsetDateTime.parse("2024-04-04T04:04:04Z"));
    }
}