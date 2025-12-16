package paperless.paperless.search;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import paperless.paperless.messaging.IndexJobMessage;
import paperless.paperless.messaging.IndexWorker;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IndexWorkerAndServiceTest {

    @Test
    void indexWorker_calls_indexingService() {
        IndexingService svc = Mockito.mock(IndexingService.class);
        IndexWorker worker = new IndexWorker(svc);

        IndexJobMessage msg = new IndexJobMessage();
        msg.setDocumentId(5L);
        msg.setFilename("HelloWorld.pdf");
        msg.setContentType("application/pdf");
        msg.setSize(100L);
        msg.setUploadedAt(OffsetDateTime.parse("2025-10-22T10:00:00Z"));
        msg.setTextContent("Hello World");

        worker.handle(msg);

        ArgumentCaptor<IndexedDocument> cap = ArgumentCaptor.forClass(IndexedDocument.class);
        verify(svc, times(1)).index(cap.capture());
        assertThat(cap.getValue().getId()).isEqualTo(5L);
        assertThat(cap.getValue().getContent()).contains("Hello");
    }
}
