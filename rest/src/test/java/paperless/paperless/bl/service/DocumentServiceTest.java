package paperless.paperless.bl.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import paperless.paperless.bl.mapper.DocumentMapper;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.bl.model.BlUploadRequest;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.dal.repository.DocumentRepository;
import paperless.paperless.infrastructure.FileStorageService;
import paperless.paperless.messaging.OcrJobMessage;
import paperless.paperless.messaging.OcrProducer;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private OcrProducer ocrProducer;
    @Mock
    private Validator validator;

    private DocumentMapper mapper;
    private DocumentServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mapper = Mappers.getMapper(DocumentMapper.class);
        service = new DocumentServiceImpl(documentRepository, fileStorageService, ocrProducer, mapper, validator);
    }

    @Test
    @DisplayName("saveDocument() uploads file to MinIO, persists entity, and sends OCR job")
    void saveDocument_success() throws IOException {
        // given
        when(validator.validate(any(BlUploadRequest.class)))
                .thenReturn(Collections.emptySet());

        // mock the uploadFile() method to return a fake MinIO object key
        when(fileStorageService.uploadFile(anyString(), any()))
                .thenReturn("1234_test.pdf");

        DocumentEntity savedEntity = new DocumentEntity();
        savedEntity.setId(1L);
        savedEntity.setFilename("test.pdf");
        savedEntity.setContentType("application/pdf");
        savedEntity.setSize(100);
        savedEntity.setUploadedAt(OffsetDateTime.now());
        savedEntity.setObjectKey("1234_test.pdf");

        when(repository.save(any(DocumentEntity.class)))
                .thenReturn(savedEntity);

        when(mapper.toBl(any(DocumentEntity.class)))
                .thenReturn(new BlDocument());

        // when
        BlDocument result = service.saveDocument("test.pdf", "application/pdf", 100, "abc".getBytes());

        // then
        assertThat(result).isNotNull();

        // verify that uploadFile() was called with the correct filename
        verify(fileStorageService).uploadFile(eq("test.pdf"), any());

        // verify entity persisted
        verify(repository).save(any(DocumentEntity.class));

        // verify that an OCR job was sent
        ArgumentCaptor<OcrJobMessage> msgCaptor = ArgumentCaptor.forClass(OcrJobMessage.class);
        verify(ocrProducer).send(msgCaptor.capture());
        OcrJobMessage sentMsg = msgCaptor.getValue();

        assertThat(sentMsg.getFilename()).isEqualTo("test.pdf");
        assertThat(sentMsg.getStoredPath()).isEqualTo("1234_test.pdf");
    }

    @Test
    void getById_found_maps_to_bl() {
        DocumentEntity e = new DocumentEntity();
        e.setId(7L);
        e.setFilename("x.pdf");
        e.setContentType("application/pdf");
        e.setSize(10L);
        e.setUploadedAt(OffsetDateTime.parse("2025-10-20T00:00:00Z"));

        when(documentRepository.findById(7L)).thenReturn(Optional.of(e));

        BlDocument out = service.getById(7L);

        assertThat(out).isNotNull();
        assertThat(out.getId()).isEqualTo(7L);
        assertThat(out.getFilename()).isEqualTo("x.pdf");
    }

    @Test
    void getById_not_found_returns_null() {
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());
        BlDocument out = service.getById(999L);
        assertThat(out).isNull();
    }

    @Test
    void saveDocument_validation_error_throws_IllegalArgumentException() {
        BlUploadRequest req = new BlUploadRequest("", "text/plain", 1L);

        @SuppressWarnings("unchecked")
        Set<ConstraintViolation<BlUploadRequest>> violations = (Set) Set.of(mock(ConstraintViolation.class));

        when(validator.validate(req)).thenReturn(violations);

        try {
            service.saveDocument(req, new byte[]{1});
        } catch (Exception ex) {
            assertThat(ex).isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(ocrProducer);
        verify(documentRepository, never()).save(any());
    }
}
