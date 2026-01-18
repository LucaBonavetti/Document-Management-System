package paperless.paperless.bl.service;

import co.elastic.clients.elasticsearch._types.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.dal.entity.DocumentTagEntity;
import paperless.paperless.dal.entity.TagEntity;
import paperless.paperless.dal.repository.DocumentRepository;
import paperless.paperless.dal.repository.DocumentTagRepository;
import paperless.paperless.infrastructure.FileStorageService;
import paperless.paperless.messaging.OcrResultMessage;
import paperless.paperless.search.SearchIndexService;
import paperless.paperless.search.dto.IndexedDocument;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentIndexingServiceTest {

    private DocumentRepository documentRepository;
    private DocumentTagRepository documentTagRepository;
    private FileStorageService fileStorageService;
    private SearchIndexService searchIndexService;

    private DocumentIndexingService indexingService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        documentRepository = mock(DocumentRepository.class);
        documentTagRepository = mock(DocumentTagRepository.class);
        fileStorageService = mock(FileStorageService.class);
        searchIndexService = mock(SearchIndexService.class);

        indexingService = new DocumentIndexingService(
                documentRepository,
                documentTagRepository,
                fileStorageService,
                searchIndexService
        );
    }

    @Test
    void handleOcrResult_downloadsText_indexesWithTags_andUpdatesDoc() throws Exception {
        DocumentEntity doc = new DocumentEntity();
        doc.setId(1L);
        doc.setFilename("a.pdf");
        doc.setUploadedAt(OffsetDateTime.parse("2026-01-01T10:00:00Z"));

        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        TagEntity t1 = new TagEntity(); t1.setId(10L); t1.setName("invoice");
        TagEntity t2 = new TagEntity(); t2.setId(11L); t2.setName("important");

        DocumentTagEntity l1 = new DocumentTagEntity(); l1.setTag(t2);
        DocumentTagEntity l2 = new DocumentTagEntity(); l2.setTag(t1);

        // DocumentIndexingService sorts tag names -> ["important","invoice"]
        when(documentTagRepository.findByDocument_Id(1L)).thenReturn(List.of(l1, l2));

        when(fileStorageService.downloadFile("k1.txt"))
                .thenReturn("HELLO OCR".getBytes(StandardCharsets.UTF_8));

        when(searchIndexService.indexDocument(any(IndexedDocument.class)))
                .thenReturn(Result.Created);

        OcrResultMessage msg = new OcrResultMessage(1L, "k1", "k1.txt", OffsetDateTime.parse("2026-01-01T10:05:00Z"));

        indexingService.handleOcrResult(msg);

        ArgumentCaptor<IndexedDocument> cap = ArgumentCaptor.forClass(IndexedDocument.class);
        verify(searchIndexService, times(1)).indexDocument(cap.capture());

        IndexedDocument indexed = cap.getValue();
        assertThat(indexed.getId()).isEqualTo(1L);
        assertThat(indexed.getFilename()).isEqualTo("a.pdf");
        assertThat(indexed.getContent()).contains("HELLO OCR");
        assertThat(indexed.getTags()).containsExactly("important", "invoice");

        verify(documentRepository, atLeastOnce()).save(any(DocumentEntity.class));
    }

    @Test
    void reindexDocument_onlyRunsWhenOcrTextKeyExists() throws Exception {
        DocumentEntity doc = new DocumentEntity();
        doc.setId(2L);
        doc.setFilename("b.pdf");
        doc.setUploadedAt(OffsetDateTime.parse("2026-01-02T10:00:00Z"));
        doc.setOcrTextKey("b.txt");

        when(documentRepository.findById(2L)).thenReturn(Optional.of(doc));
        when(documentTagRepository.findByDocument_Id(2L)).thenReturn(List.of());
        when(fileStorageService.downloadFile("b.txt"))
                .thenReturn("TEXT".getBytes(StandardCharsets.UTF_8));
        when(searchIndexService.indexDocument(any())).thenReturn(Result.Updated);

        indexingService.reindexDocument(2L);

        verify(searchIndexService, times(1)).indexDocument(any(IndexedDocument.class));
        verify(documentRepository, atLeastOnce()).save(any(DocumentEntity.class));
    }
}
