package paperless.paperless.bl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.dal.entity.DocumentTagEntity;
import paperless.paperless.dal.entity.TagEntity;
import paperless.paperless.dal.repository.DocumentRepository;
import paperless.paperless.dal.repository.DocumentTagRepository;
import paperless.paperless.model.SearchDocumentResult;
import paperless.paperless.search.SearchIndexService;
import paperless.paperless.search.dto.SearchHit;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SearchServiceImplTest {

    private SearchIndexService searchIndexService;
    private DocumentRepository documentRepository;
    private DocumentTagRepository documentTagRepository;

    private SearchServiceImpl service;

    @BeforeEach
    void setup() {
        searchIndexService = mock(SearchIndexService.class);
        documentRepository = mock(DocumentRepository.class);
        documentTagRepository = mock(DocumentTagRepository.class);

        service = new SearchServiceImpl(searchIndexService, documentRepository, documentTagRepository);
    }

    @Test
    void search_returnsDocsInHitOrder_withTags() throws Exception {
        when(searchIndexService.search("hello", List.of("invoice"), 20))
                .thenReturn(List.of(
                        new SearchHit(2L, 1.1),
                        new SearchHit(1L, 0.9)
                ));

        DocumentEntity d1 = new DocumentEntity();
        d1.setId(1L);
        d1.setFilename("a.pdf");
        d1.setContentType("application/pdf");
        d1.setSize(10L);
        d1.setUploadedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));

        DocumentEntity d2 = new DocumentEntity();
        d2.setId(2L);
        d2.setFilename("b.pdf");
        d2.setContentType("application/pdf");
        d2.setSize(20L);
        d2.setUploadedAt(OffsetDateTime.parse("2026-01-02T00:00:00Z"));

        when(documentRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(d1, d2));

        TagEntity t = new TagEntity(); t.setName("invoice");
        DocumentTagEntity link = new DocumentTagEntity(); link.setTag(t);

        when(documentTagRepository.findByDocument_Id(1L)).thenReturn(List.of());
        when(documentTagRepository.findByDocument_Id(2L)).thenReturn(List.of(link));

        List<SearchDocumentResult> out = service.search("hello", List.of("invoice"), 20);

        assertThat(out).hasSize(2);
        assertThat(out.get(0).getId()).isEqualTo(2L);
        assertThat(out.get(0).getTags()).containsExactly("invoice");
        assertThat(out.get(0).getScore()).isEqualTo(1.1);

        assertThat(out.get(1).getId()).isEqualTo(1L);
        assertThat(out.get(1).getTags()).isEmpty();
        assertThat(out.get(1).getScore()).isEqualTo(0.9);
    }
}
