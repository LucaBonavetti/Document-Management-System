package paperless.paperless.messaging;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OcrAndIndexFlowTest {

    @Test
    void ocrWorker_emits_index_job_with_text_fallback() {
        IndexProducer producer = Mockito.mock(IndexProducer.class);
        OcrWorker worker = new OcrWorker(producer);

        OcrJobMessage job = new OcrJobMessage();
        job.setDocumentId(1L);
        job.setFilename("HelloWorld.pdf");
        job.setContentType("application/pdf");
        job.setSize(10L);
        job.setUploadedAt(OffsetDateTime.parse("2025-10-22T10:00:00Z"));
        job.setStoredPath("C:/does/not/matter");

        worker.handle(job);

        ArgumentCaptor<IndexJobMessage> cap = ArgumentCaptor.forClass(IndexJobMessage.class);
        verify(producer, times(1)).send(cap.capture());
        assertThat(cap.getValue().getDocumentId()).isEqualTo(1L);
        // Fallback makes filename searchable ("HelloWorld" -> contains "Hello")
        assertThat(cap.getValue().getTextContent()).contains("HelloWorld");
    }
}
