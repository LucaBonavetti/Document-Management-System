package paperless.paperless.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import paperless.paperless.search.IndexedDocument;
import paperless.paperless.search.IndexingService;

@Service
@Slf4j
public class IndexWorker {

    private final IndexingService indexingService;

    public IndexWorker(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @RabbitListener(queues = "${INDEX_QUEUE_NAME:INDEX_QUEUE}")
    public void handle(IndexJobMessage msg) {
        try {
            if (msg == null) return;
            IndexedDocument doc = new IndexedDocument();
            doc.setId(msg.getDocumentId());
            doc.setFilename(msg.getFilename());
            doc.setContentType(msg.getContentType());
            doc.setSize(msg.getSize());
            doc.setUploadedAt(msg.getUploadedAt());
            doc.setContent(msg.getTextContent());
            doc.setCategory(msg.getCategory());
            doc.setTags(msg.getTags());

            indexingService.index(doc);
            log.info("Indexed document {}", msg.getDocumentId());
        } catch (Exception e) {
            log.error("Indexing failed: {}", e.getMessage(), e);
        }
    }
}
