package paperless.ocrworker.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import paperless.ocrworker.config.MinioConfig;
import paperless.ocrworker.messaging.OcrResultProducer;
import paperless.paperless.messaging.OcrJobMessage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OcrWorkerServiceTest {

    private MinioClient minio;
    private MinioConfig minioConfig;
    private RabbitTemplate rabbitTemplate;
    private OcrResultProducer resultProducer;

    private OcrWorkerService service;

    @BeforeEach
    void setup() {
        minio = mock(MinioClient.class, withSettings().lenient());
        rabbitTemplate = mock(RabbitTemplate.class, withSettings().lenient());
        resultProducer = mock(OcrResultProducer.class, withSettings().lenient());

        minioConfig = new MinioConfig();
        ReflectionTestUtils.setField(minioConfig, "bucketName", "documents");

        service = spy(new OcrWorkerService(minio, minioConfig, rabbitTemplate, resultProducer));

        ReflectionTestUtils.setField(service, "storeTextToMinio", true);
    }

    private static OcrJobMessage msg(long id, String file, String key) {
        return new OcrJobMessage(id, file, "application/pdf", 1234L, key, OffsetDateTime.now());
    }

    @Test
    void process_pdf_happyPath_writesTextNextToOriginal_andPublishesResult() throws Exception {
        OcrJobMessage m = msg(1L, "doc.pdf", "folder/doc.pdf");

        doThrow(new RuntimeException("stat fail")).when(minio).statObject(any(StatObjectArgs.class));

        InputStream pdfBytes = new ByteArrayInputStream("%PDF-1.4\n%".getBytes());
        doReturn(pdfBytes).when(service).fetchFromMinio(eq("documents"), eq("folder/doc.pdf"));

        doReturn("HELLO FROM OCR").when(service).ocrPdf(any(Path.class));

        service.process(m);

        ArgumentCaptor<PutObjectArgs> cap = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minio, times(1)).putObject(cap.capture());
        assertEquals("folder/doc.pdf.txt", cap.getValue().object());

        verify(resultProducer, times(1)).send(any());
    }

    @Test
    void process_image_happyPath_writesTextNextToOriginal_andPublishesResult() throws Exception {
        OcrJobMessage m = msg(2L, "scan.png", "inbox/scan.png");

        doThrow(new RuntimeException("stat fail")).when(minio).statObject(any(StatObjectArgs.class));

        InputStream imgBytes = new ByteArrayInputStream(new byte[]{1, 2, 3, 4});
        doReturn(imgBytes).when(service).fetchFromMinio(eq("documents"), eq("inbox/scan.png"));

        doReturn("TEXT FROM IMAGE").when(service).ocrImage(any(Path.class));

        service.process(m);

        ArgumentCaptor<PutObjectArgs> cap = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minio, times(1)).putObject(cap.capture());
        assertEquals("inbox/scan.png.txt", cap.getValue().object());

        verify(resultProducer, times(1)).send(any());
    }

    @Test
    void process_skips_whenTextAlreadyExists_butPublishesResult() throws Exception {
        OcrJobMessage m = msg(3L, "anything.pdf", "existing/anything.pdf");

        when(minio.statObject(any(StatObjectArgs.class))).thenReturn(mock(StatObjectResponse.class));

        assertDoesNotThrow(() -> service.process(m));

        verify(service, never()).fetchFromMinio(anyString(), anyString());
        verify(service, never()).ocrPdf(any());
        verify(service, never()).ocrImage(any());

        verify(minio, never()).putObject(any(PutObjectArgs.class));
        verify(resultProducer, times(1)).send(any());
    }
}