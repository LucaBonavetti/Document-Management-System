package paperless.paperless.messaging;

import org.junit.jupiter.api.Test;
import paperless.paperless.bl.service.DocumentIndexingService;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.*;

class OcrResultConsumerTest {

    @Test
    void onMessage_delegates_to_indexingService() {
        DocumentIndexingService indexing = mock(DocumentIndexingService.class);
        OcrResultConsumer consumer = new OcrResultConsumer(indexing, "ocr-results");

        OcrResultMessage msg = new OcrResultMessage(1L, "k", "k.txt", OffsetDateTime.now());
        consumer.onMessage(msg);

        verify(indexing, times(1)).handleOcrResult(msg);
    }
}
