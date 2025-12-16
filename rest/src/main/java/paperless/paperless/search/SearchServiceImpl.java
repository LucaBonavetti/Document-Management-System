package paperless.paperless.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import paperless.paperless.model.Document;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchClient client;
    private final String index;

    public SearchServiceImpl(ElasticsearchClient client,
                             @Value("${elasticsearch.index:documents}") String index) {
        this.client = client;
        this.index = index;
    }

    @Override
    public List<Document> search(String query, int limit) {
        List<Document> out = new ArrayList<>();
        try {
            SearchRequest req = SearchRequest.of(b -> b
                    .index(index)
                    .size(Math.max(1, Math.min(limit, 100)))
                    .query(q -> q
                            .multiMatch(m -> m
                                    .query(query)
                                    .fields("filename^2", "content")
                            )
                    )
            );

            SearchResponse<IndexedDocument> resp = client.search(req, IndexedDocument.class);
            for (Hit<IndexedDocument> hit : resp.hits().hits()) {
                IndexedDocument src = hit.source();
                if (src == null) continue;
                Document d = new Document();
                d.setId(src.getId());
                d.setFilename(src.getFilename());
                d.setContentType(src.getContentType());
                d.setSize(src.getSize());
                d.setUploadedAt(src.getUploadedAt());
                d.setCategory(src.getCategory());
                d.setTags(src.getTags());
                out.add(d);
            }
        } catch (Exception e) {
            // return empty list on errors; app must not fail
            return List.of();
        }
        return out;
    }
}
