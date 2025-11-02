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
    void saveDocument_valid_request_persists_and_sends_job() throws Exception {
        BlUploadRequest req = new BlUploadRequest("hello.txt", "text/plain", 5L);
        byte[] data = "hello".getBytes();

        when(validator.validate(req)).thenReturn(Collections.emptySet());

        // NEW: uploadFile returns MinIO object key (String), not a Path
        when(fileStorageService.uploadFile(eq("hello.txt"), any()))
                .thenReturn("objects/12345_hello.txt");

        DocumentEntity saved = new DocumentEntity();
        saved.setId(1L);
        saved.setFilename("hello.txt");
        saved.setContentType("text/plain");
        saved.setSize(5L);
        saved.setUploadedAt(OffsetDateTime.parse("2025-10-22T10:00:00Z"));
        saved.setObjectKey("objects/12345_hello.txt");

        when(documentRepository.save(any(DocumentEntity.class))).thenReturn(saved);

        BlDocument out = service.saveDocument(req, data);

        assertThat(out).isNotNull();
        assertThat(out.getId()).isEqualTo(1L);
        assertThat(out.getFilename()).isEqualTo("hello.txt");
        assertThat(out.getSize()).isEqualTo(5L);

        // verify file upload call
        verify(fileStorageService, times(1)).uploadFile(eq("hello.txt"), any());

        // verify entity persisted
        verify(documentRepository, times(1)).save(any(DocumentEntity.class));

        // verify OCR job sent with storedPath = objectKey
        ArgumentCaptor<OcrJobMessage> msg = ArgumentCaptor.forClass(OcrJobMessage.class);
        verify(ocrProducer, times(1)).send(msg.capture());
        assertThat(msg.getValue().getDocumentId()).isEqualTo(1L);
        assertThat(msg.getValue().getFilename()).isEqualTo("hello.txt");
        assertThat(msg.getValue().getSize()).isEqualTo(5L);
        assertThat(msg.getValue().getStoredPath()).isEqualTo("objects/12345_hello.txt");
    }

    @Test
    void getById_found_maps_to_bl() {
        DocumentEntity e = new DocumentEntity();
        e.setId(7L);
        e.setFilename("x.pdf");
        e.setContentType("application/pdf");
        e.setSize(10L);
        e.setUploadedAt(OffsetDateTime.parse("2025-10-20T00:00:00Z"));
        e.setObjectKey("objects/x.pdf");

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
        Set<ConstraintViolation<BlUploadRequest>> violations =
                (Set) Set.of(mock(ConstraintViolation.class));

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