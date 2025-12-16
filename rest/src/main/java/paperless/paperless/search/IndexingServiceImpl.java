package paperless.paperless.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IndexingServiceImpl implements IndexingService {

    private final ElasticsearchClient client;
    private final String index;

    public IndexingServiceImpl(ElasticsearchClient client,
                               @Value("${elasticsearch.index:documents}") String index) {
        this.client = client;
        this.index = index;
    }

    @Override
    public void index(IndexedDocument doc) {
        try {
            IndexRequest<IndexedDocument> req = IndexRequest.of(b -> b
                    .index(index)
                    .id(String.valueOf(doc.getId()))
                    .document(doc));
            client.index(req);
        } catch (Exception e) {
            // don't break the app; log & continue
            System.err.println("Failed to index doc " + doc.getId() + ": " + e.getMessage());
        }
    }
}
