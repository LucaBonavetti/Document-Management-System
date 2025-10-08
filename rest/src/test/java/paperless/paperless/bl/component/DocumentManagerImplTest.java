package paperless.paperless.bl.component;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import paperless.paperless.bl.mapper.DocumentMapper;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.dal.repository.DocumentRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import paperless.paperless.messaging.OcrProducer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentManagerImplTest {

    @Mock
    private DocumentRepository repository;

    @Mock
    private DocumentMapper mapper;

    @Mock
    private OcrProducer ocrProducer;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @AfterEach
    void cleanup() throws IOException {
        Path storage = Path.of("storage");
        if (Files.exists(storage)) {
            Files.walk(storage)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        }
    }

    @Test
    void upload_success_persists_and_sends_ocr_job() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", "Hello".getBytes());

        DocumentEntity savedEntity = new DocumentEntity();
        savedEntity.setId(42L);
        savedEntity.setFilename("hello.txt");
        savedEntity.setContentType("text/plain");
        savedEntity.setSize(5L);
        savedEntity.setUploadedAt(OffsetDateTime.parse("2024-01-01T10:00:00Z"));

        when(repository.save(any(DocumentEntity.class))).thenReturn(savedEntity);

        BlDocument mapped = new BlDocument();
        mapped.setId(42L);
        mapped.setFilename("hello.txt");
        mapped.setContentType("text/plain");
        mapped.setSize(5L);
        mapped.setUploadedAt(OffsetDateTime.parse("2024-01-01T10:00:00Z"));

        when(mapper.toBl(savedEntity)).thenReturn(mapped);

        DocumentManagerImpl manager = new DocumentManagerImpl(repository, mapper, validator, ocrProducer);

        BlDocument result = manager.upload(file);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(42L);

        ArgumentCaptor<DocumentEntity> captor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(repository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getFilename()).isEqualTo("hello.txt");
        // verify OCR message was published
        verify(ocrProducer, times(1)).send(any());
        verify(mapper, times(1)).toBl(savedEntity);
    }

    @Test
    void upload_faultyData_triggersValidation_and_no_publish() throws Exception {
        // size=0 is invalid by @Positive
        MockMultipartFile file = new MockMultipartFile("file", "bad.txt", "text/plain", new byte[0]);

        DocumentManagerImpl manager = new DocumentManagerImpl(repository, mapper, validator, ocrProducer);

        assertThatThrownBy(() -> manager.upload(file))
                .isInstanceOf(ConstraintViolationException.class);

        verifyNoInteractions(repository, mapper, ocrProducer);
    }

    @Test
    void getById_found() throws Exception {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(7L);
        entity.setFilename("doc.pdf");
        entity.setContentType("application/pdf");
        entity.setSize(100L);
        entity.setUploadedAt(OffsetDateTime.parse("2024-02-02T00:00:00Z"));

        when(repository.findById(7L)).thenReturn(Optional.of(entity));

        BlDocument mapped = new BlDocument();
        mapped.setId(7L);
        mapped.setFilename("doc.pdf");
        mapped.setContentType("application/pdf");
        mapped.setSize(100L);
        mapped.setUploadedAt(OffsetDateTime.parse("2024-02-02T00:00:00Z"));

        when(mapper.toBl(entity)).thenReturn(mapped);

        DocumentManagerImpl manager = new DocumentManagerImpl(repository, mapper, validator, ocrProducer);

        BlDocument result = manager.getById(7L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(7L);
        verify(repository, times(1)).findById(7L);
        verify(mapper, times(1)).toBl(entity);
        verifyNoInteractions(ocrProducer);
    }

    @Test
    void getById_notFound() throws Exception {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        DocumentManagerImpl manager = new DocumentManagerImpl(repository, mapper, validator, ocrProducer);

        BlDocument result = manager.getById(99L);

        assertThat(result).isNull();
        verify(repository, times(1)).findById(99L);
        verifyNoMoreInteractions(repository, mapper);
        verifyNoInteractions(ocrProducer);
    }
}