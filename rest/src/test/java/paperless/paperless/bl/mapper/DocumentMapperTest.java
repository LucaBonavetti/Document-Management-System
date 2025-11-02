package paperless.paperless.bl.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.model.Document;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentMapperTest {

    private final DocumentMapper mapper = Mappers.getMapper(DocumentMapper.class);

    @Test
    void map_DalEntity_to_BlDocument() {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(42L);
        entity.setFilename("hello.pdf");
        entity.setContentType("application/pdf");
        entity.setSize(12345L);
        entity.setUploadedAt(OffsetDateTime.parse("2025-10-21T10:15:30+00:00"));

        BlDocument bl = mapper.toBl(entity);

        assertThat(bl).isNotNull();
        assertThat(bl.getId()).isEqualTo(42L);
        assertThat(bl.getFilename()).isEqualTo("hello.pdf");
        assertThat(bl.getContentType()).isEqualTo("application/pdf");
        assertThat(bl.getSize()).isEqualTo(12345L);
        assertThat(bl.getUploadedAt()).isEqualTo(OffsetDateTime.parse("2025-10-21T10:15:30+00:00"));
    }

    @Test
    void map_BlDocument_to_DalEntity() {
        BlDocument bl = new BlDocument();
        bl.setId(7L);
        bl.setFilename("report.txt");
        bl.setContentType("text/plain");
        bl.setSize(9L);
        bl.setUploadedAt(OffsetDateTime.parse("2025-10-22T08:00:00Z"));

        DocumentEntity entity = mapper.toEntity(bl);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(7L);
        assertThat(entity.getFilename()).isEqualTo("report.txt");
        assertThat(entity.getContentType()).isEqualTo("text/plain");
        assertThat(entity.getSize()).isEqualTo(9L);
        assertThat(entity.getUploadedAt()).isEqualTo(OffsetDateTime.parse("2025-10-22T08:00:00Z"));
    }

    @Test
    void map_BlDocument_to_ApiDocument() {
        BlDocument bl = new BlDocument();
        bl.setId(1L);
        bl.setFilename("x.png");
        bl.setContentType("image/png");
        bl.setSize(111L);
        bl.setUploadedAt(OffsetDateTime.parse("2025-10-20T12:34:56Z"));

        Document dto = mapper.toApi(bl);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getFilename()).isEqualTo("x.png");
        assertThat(dto.getContentType()).isEqualTo("image/png");
        assertThat(dto.getSize()).isEqualTo(111L);
        assertThat(dto.getUploadedAt()).isEqualTo(OffsetDateTime.parse("2025-10-20T12:34:56Z"));
    }

    @Test
    void map_BlDocument_list_to_ApiDocument_list() {
        BlDocument a = new BlDocument();
        a.setId(1L);
        a.setFilename("a");
        a.setContentType("text/plain");
        a.setSize(1L);
        a.setUploadedAt(OffsetDateTime.parse("2025-10-20T00:00:00Z"));

        BlDocument b = new BlDocument();
        b.setId(2L);
        b.setFilename("b");
        b.setContentType("text/plain");
        b.setSize(2L);
        b.setUploadedAt(OffsetDateTime.parse("2025-10-21T00:00:00Z"));

        List<Document> out = mapper.toApiList(List.of(a, b));

        assertThat(out).hasSize(2);
        assertThat(out.get(0).getId()).isEqualTo(1L);
        assertThat(out.get(1).getId()).isEqualTo(2L);
    }
}
