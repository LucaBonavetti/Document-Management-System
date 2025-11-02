package paperless.ocrworker.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import paperless.ocrworker.config.MinioConfig;
import paperless.paperless.messaging.OcrJobMessage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OcrWorkerServiceTest {

    private MinioClient minio;
    private MinioConfig minioConfig;
    private OcrWorkerService service;

    @BeforeEach
    void setup() {
        minio = mock(MinioClient.class, withSettings().lenient());
        minioConfig = new MinioConfig();
        ReflectionTestUtils.setField(minioConfig, "bucketName", "documents");

        service = spy(new OcrWorkerService(minio, minioConfig));
        ReflectionTestUtils.setField(service, "storeTextToMinio", true);
    }

    private static OcrJobMessage msg(long id, String file, String key) {
        return new OcrJobMessage(id, file, "application/pdf", 1234L, key, OffsetDateTime.now());
    }

    @Test
    void process_pdf_happyPath_writesTextNextToOriginal() throws Exception {
        var m = msg(1L, "doc.pdf", "folder/doc.pdf");

        // Force objectExists(...) = false (treat statObject failure as “not exists”)
        doThrow(new RuntimeException("stat fail")).when(minio).statObject(any());

        // Stub the seam used by the private retry helper
        InputStream pdfBytes = new ByteArrayInputStream("%PDF-1.4\n%".getBytes());
        doReturn(pdfBytes).when(service).fetchFromMinio("documents", "folder/doc.pdf");

        // Avoid real Tesseract
        doReturn("HELLO FROM OCR").when(service).ocrPdf(any(Path.class));

        service.process(m);

        ArgumentCaptor<PutObjectArgs> cap = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minio, times(1)).putObject(cap.capture());
        assertEquals("folder/doc.pdf.txt", cap.getValue().object());
    }

    @Test
    void process_image_happyPath_writesTextNextToOriginal() throws Exception {
        var m = msg(2L, "scan.png", "inbox/scan.png");

        // Force objectExists(...) = false
        doThrow(new RuntimeException("stat fail")).when(minio).statObject(any());

        // Stub the seam used by the private retry helper
        InputStream imgBytes = new ByteArrayInputStream(new byte[]{1,2,3,4});
        doReturn(imgBytes).when(service).fetchFromMinio("documents", "inbox/scan.png");

        // Avoid real Tesseract
        doReturn("TEXT FROM IMAGE").when(service).ocrImage(any(Path.class));

        service.process(m);

        ArgumentCaptor<PutObjectArgs> cap = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minio, times(1)).putObject(cap.capture());
        assertEquals("inbox/scan.png.txt", cap.getValue().object());
    }

    @Test
    void process_skips_whenTextAlreadyExists() throws Exception {
        var m = msg(3L, "anything.pdf", "existing/anything.pdf");

        // Make objectExists(...) => true by returning a dummy StatObjectResponse
        when(minio.statObject(any())).thenReturn(mock(StatObjectResponse.class));

        assertDoesNotThrow(() -> service.process(m));

        // If skipped, we neither download nor write
        verify(service, never()).fetchFromMinio(anyString(), anyString());
        verify(minio, never()).putObject(any(PutObjectArgs.class));
    }
}