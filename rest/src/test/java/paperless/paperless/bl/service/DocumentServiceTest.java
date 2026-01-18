package paperless.paperless.bl.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private DocumentMapper mapper;

    private DocumentServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new DocumentServiceImpl(documentRepository, fileStorageService, ocrProducer, mapper, validator);
    }

    @Test
    @DisplayName("saveDocument() uploads file to MinIO, persists entity, and sends OCR job")
    void saveDocument_success() throws Exception {
        // given
        byte[] fileBytes = "abc".getBytes();
        BlUploadRequest req = new BlUploadRequest("test.pdf", "application/pdf", 100L);

        when(validator.validate(any(BlUploadRequest.class)))
                .thenReturn(Collections.emptySet());

        when(fileStorageService.uploadFile(eq("test.pdf"), any(byte[].class)))
                .thenReturn("1234_test.pdf");

        when(documentRepository.save(any(DocumentEntity.class)))
                .thenAnswer(inv -> {
                    DocumentEntity e = inv.getArgument(0, DocumentEntity.class);
                    e.setId(1L);
                    if (e.getUploadedAt() == null) {
                        e.setUploadedAt(OffsetDateTime.now());
                    }
                    return e;
                });

        when(mapper.toBl(any(DocumentEntity.class)))
                .thenAnswer(inv -> {
                    DocumentEntity e = inv.getArgument(0, DocumentEntity.class);
                    BlDocument bl = new BlDocument();
                    bl.setId(e.getId());
                    bl.setFilename(e.getFilename());
                    bl.setContentType(e.getContentType());
                    bl.setSize(e.getSize());
                    bl.setUploadedAt(e.getUploadedAt());
                    return bl;
                });

        // when
        BlDocument result = service.saveDocument(req, fileBytes);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFilename()).isEqualTo("test.pdf");

        verify(fileStorageService).uploadFile(eq("test.pdf"), any(byte[].class));
        verify(documentRepository).save(any(DocumentEntity.class));

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

        BlDocument mapped = new BlDocument();
        mapped.setId(7L);
        mapped.setFilename("x.pdf");
        mapped.setContentType("application/pdf");
        mapped.setSize(10L);
        mapped.setUploadedAt(e.getUploadedAt());

        when(documentRepository.findById(7L)).thenReturn(Optional.of(e));
        when(mapper.toBl(e)).thenReturn(mapped);

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
        ConstraintViolation<BlUploadRequest> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("filename must not be blank");

        @SuppressWarnings("unchecked")
        Set<ConstraintViolation<BlUploadRequest>> violations = (Set) Set.of(violation);

        when(validator.validate(req)).thenReturn(violations);

        assertThatThrownBy(() -> service.saveDocument(req, new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(ocrProducer);
        verify(documentRepository, never()).save(any());
    }
}
