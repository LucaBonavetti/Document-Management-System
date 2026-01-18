package paperless.paperless.bl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import paperless.paperless.bl.mapper.TagMapper;
import paperless.paperless.bl.model.BlTag;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.dal.entity.DocumentTagEntity;
import paperless.paperless.dal.entity.TagEntity;
import paperless.paperless.dal.repository.DocumentRepository;
import paperless.paperless.dal.repository.DocumentTagRepository;
import paperless.paperless.dal.repository.TagRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TagServiceImplTest {

    private DocumentRepository documentRepository;
    private TagRepository tagRepository;
    private DocumentTagRepository documentTagRepository;
    private TagMapper tagMapper;
    private DocumentIndexingService indexingService;

    private TagServiceImpl service;

    @BeforeEach
    void setup() {
        documentRepository = mock(DocumentRepository.class);
        tagRepository = mock(TagRepository.class);
        documentTagRepository = mock(DocumentTagRepository.class);
        tagMapper = mock(TagMapper.class);
        indexingService = mock(DocumentIndexingService.class);

        service = new TagServiceImpl(documentRepository, tagRepository, documentTagRepository, tagMapper, indexingService);
    }

    @Test
    void addTag_normalizes_andTriggersReindex() {
        DocumentEntity doc = new DocumentEntity();
        doc.setId(1L);
        doc.setFilename("x.pdf");
        doc.setUploadedAt(OffsetDateTime.now());

        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(documentTagRepository.findByDocument_Id(1L)).thenReturn(List.of()); // 0 tags currently

        when(tagRepository.findByName("invoice-2026")).thenReturn(Optional.empty());

        TagEntity saved = new TagEntity();
        saved.setId(99L);
        saved.setName("invoice-2026");
        saved.setCreatedAt(OffsetDateTime.now());

        when(tagRepository.save(any(TagEntity.class))).thenReturn(saved);
        when(documentTagRepository.existsByDocument_IdAndTag_Id(1L, 99L)).thenReturn(false);

        BlTag bl = new BlTag();
        bl.setId(99L);
        bl.setName("invoice-2026");
        when(tagMapper.toBl(any(TagEntity.class))).thenReturn(bl);

        BlTag out = service.addTagToDocument(1L, " Invoice   2026 ");

        assertThat(out.getName()).isEqualTo("invoice-2026");

        verify(tagRepository).save(any(TagEntity.class));
        verify(indexingService).reindexDocument(1L);
    }

    @Test
    void addTag_maxTags_throws() {
        DocumentEntity doc = new DocumentEntity();
        doc.setId(1L);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        // create 10 existing tag links
        List<DocumentTagEntity> ten = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ten.add(new DocumentTagEntity());
        }
        when(documentTagRepository.findByDocument_Id(1L)).thenReturn(ten);

        assertThatThrownBy(() -> service.addTagToDocument(1L, "any"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
