package paperless.paperless.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.dal.repository.DocumentRepository;
import paperless.paperless.infrastructure.FileStorageService;
import paperless.paperless.messaging.OcrProducer;
import paperless.paperless.search.SearchIndexService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentUploadIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DocumentRepository documentRepository;

    // External dependencies -> mock them so the test is stable
    @MockBean private FileStorageService fileStorageService;
    @MockBean private OcrProducer ocrProducer;

    // IMPORTANT: this replaces ElasticsearchService bean so it will NOT try to connect/create index
    @MockBean private SearchIndexService searchIndexService;

    @BeforeEach
    void setup() {
        when(fileStorageService.uploadFile(anyString(), any(byte[].class)))
                .thenReturn("test-object-key");
        doNothing().when(ocrProducer).send(any());
    }

    @Test
    void uploadDocument_createsDocumentInDb_andReturns201() throws Exception {
        long before = documentRepository.count();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "hello pdf content".getBytes()
        );

        mockMvc.perform(
                        multipart("/api/documents")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/documents/")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.filename").value("test.pdf"));

        long after = documentRepository.count();
        assertThat(after).isEqualTo(before + 1);

        // Quick sanity check: the row is there and has the objectKey
        DocumentEntity saved = documentRepository.findAll()
                .stream()
                .max((a, b) -> a.getId().compareTo(b.getId()))
                .orElseThrow();

        assertThat(saved.getFilename()).isEqualTo("test.pdf");
        assertThat(saved.getObjectKey()).isEqualTo("test-object-key");
        assertThat(saved.getUploadedAt()).isNotNull();
    }
}