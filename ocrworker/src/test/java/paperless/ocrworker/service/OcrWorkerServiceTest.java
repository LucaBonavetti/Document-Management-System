package paperless.ocrworker.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
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
        // plain mocks
        minio = mock(MinioClient.class, withSettings().lenient());
        minioConfig = new MinioConfig();
        // inject bucket name
        ReflectionTestUtils.setField(minioConfig, "bucketName", "documents");

        // spy the service so we can stub fetchFromMinio + OCR helpers
        service = spy(new OcrWorkerService(minio, minioConfig));

        // force writing OCR output for verification
        ReflectionTestUtils.setField(service, "storeTextToMinio", true);
    }

    private static OcrJobMessage msg(long id, String file, String key) {
        return new OcrJobMessage(id, file, "application/octet-stream", 1234L, key, OffsetDateTime.now());
    }

    @Test
    void process_pdf_happyPath_writesTextNextToOriginal() throws Exception {
        OcrJobMessage m = msg(1L, "doc.pdf", "folder/doc.pdf");

        // stub: pretend MinIO returns a tiny PDF-ish stream
        InputStream pdfBytes = new ByteArrayInputStream("%PDF-1.4\n%".getBytes());
        doReturn(pdfBytes).when(service).fetchFromMinio("documents", "folder/doc.pdf");

        // stub OCR (avoid real tesseract)
        doReturn("HELLO FROM OCR").when(service).ocrPdf(any(Path.class));

        service.process(m);

        // verify: a .txt was stored next to the original key
        ArgumentCaptor<PutObjectArgs> cap = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minio, times(1)).putObject(cap.capture());
        assertEquals("folder/doc.pdf.txt", cap.getValue().object());
    }

    @Test
    void process_image_happyPath_writesTextNextToOriginal() throws Exception {
        OcrJobMessage m = msg(2L, "scan.png", "inbox/scan.png");

        // stub: pretend MinIO returns image bytes
        InputStream imgBytes = new ByteArrayInputStream(new byte[]{1,2,3,4});
        doReturn(imgBytes).when(service).fetchFromMinio("documents", "inbox/scan.png");

        // stub OCR (avoid real tesseract)
        doReturn("TEXT FROM IMAGE").when(service).ocrImage(any(Path.class));

        service.process(m);

        ArgumentCaptor<PutObjectArgs> cap = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minio, times(1)).putObject(cap.capture());
        assertEquals("inbox/scan.png.txt", cap.getValue().object());
    }

    @Test
    void process_handlesMissingObject_gracefully() throws Exception {
        OcrJobMessage m = msg(3L, "missing.pdf", "missing/missing.pdf");

        // simulate MinIO "NoSuchKey" by throwing from the seam
        doThrow(new RuntimeException("NoSuchKey"))
                .when(service).fetchFromMinio("documents", "missing/missing.pdf");

        assertDoesNotThrow(() -> service.process(m));

        // ensure we did not try to write any .txt on failure
        verify(minio, never()).putObject(any(PutObjectArgs.class));
    }
}