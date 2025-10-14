package paperless.paperless.bl.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import paperless.paperless.bl.mapper.DocumentMapper;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.bl.model.BlUploadRequest;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.dal.repository.DocumentRepository;
import paperless.paperless.infrastructure.FileStorageService;
import paperless.paperless.messaging.OcrJobMessage;
import paperless.paperless.messaging.OcrProducer;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentServiceImplTest {

    private DocumentRepository repository;
    private DocumentMapper mapper;
    private Validator validator;
    private FileStorageService fileStorageService;
    private OcrProducer ocrProducer;

    private DocumentServiceImpl service;

    @BeforeEach
    void setup() {
        repository = mock(DocumentRepository.class);
        mapper = mock(DocumentMapper.class);
        validator = mock(Validator.class);
        fileStorageService = mock(FileStorageService.class);
        ocrProducer = mock(OcrProducer.class);

        service = new DocumentServiceImpl(repository, mapper, validator, fileStorageService, ocrProducer);
    }

    @Test
    @DisplayName("saveDocument() stores file, persists entity, and sends OCR job")
    void saveDocument_success() throws IOException {
        // given
        when(validator.validate(any(BlUploadRequest.class))).thenReturn(Collections.emptySet());
        when(fileStorageService.saveFile(anyString(), any())).thenReturn(Path.of("storage/test.pdf"));

        DocumentEntity savedEntity = new DocumentEntity();
        savedEntity.setId(1L);
        savedEntity.setFilename("test.pdf");
        savedEntity.setContentType("application/pdf");
        savedEntity.setSize(100);
        savedEntity.setUploadedAt(OffsetDateTime.now());

        when(repository.save(any(DocumentEntity.class))).thenReturn(savedEntity);
        when(mapper.toBl(any(DocumentEntity.class))).thenReturn(new BlDocument());

        // when
        BlDocument result = service.saveDocument("test.pdf", "application/pdf", 100, "abc".getBytes());

        // then
        assertThat(result).isNotNull();

        verify(fileStorageService).saveFile(eq("test.pdf"), any());
        verify(repository).save(any(DocumentEntity.class));

        ArgumentCaptor<OcrJobMessage> msgCaptor = ArgumentCaptor.forClass(OcrJobMessage.class);
        verify(ocrProducer).send(msgCaptor.capture());
        assertThat(msgCaptor.getValue().getFilename()).isEqualTo("test.pdf");
    }

    @Test
    @DisplayName("saveDocument() throws ConstraintViolationException if validation fails")
    void saveDocument_validationFails() {
        // given
        ConstraintViolation<BlUploadRequest> violation = mock(ConstraintViolation.class);
        when(validator.validate(any(BlUploadRequest.class))).thenReturn(Set.of(violation));

        // then
        assertThatThrownBy(() ->
                service.saveDocument("x.pdf", "application/pdf", 10, "data".getBytes()))
                .isInstanceOf(ConstraintViolationException.class);

        verifyNoInteractions(fileStorageService, repository, ocrProducer);
    }

    @Test
    @DisplayName("getById() returns mapped BlDocument when entity found")
    void getById_found() {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(entity));
        when(mapper.toBl(entity)).thenReturn(new BlDocument());

        BlDocument result = service.getById(5L);

        assertThat(result).isNotNull();
        verify(repository).findById(5L);
    }

    @Test
    @DisplayName("getById() returns null when entity not found")
    void getById_notFound() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        BlDocument result = service.getById(99L);

        assertThat(result).isNull();
    }
}