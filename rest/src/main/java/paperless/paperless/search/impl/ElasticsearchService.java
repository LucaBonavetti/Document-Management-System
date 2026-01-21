package paperless.paperless.search.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import paperless.paperless.config.ElasticsearchConfig;
import paperless.paperless.search.SearchIndexService;
import paperless.paperless.search.dto.IndexedDocument;
import paperless.paperless.search.dto.SearchHit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class ElasticsearchService implements SearchIndexService {

    private final ElasticsearchClient esClient;

    @Autowired
    public ElasticsearchService(ElasticsearchClient esClient) throws IOException {
        this.esClient = esClient;

        if (!esClient.indices().exists(i -> i.index(ElasticsearchConfig.DOCUMENTS_INDEX_NAME)).value()) {
            esClient.indices().create(c -> c.index(ElasticsearchConfig.DOCUMENTS_INDEX_NAME));
            log.info("Created Elasticsearch index '{}'", ElasticsearchConfig.DOCUMENTS_INDEX_NAME);
        }
    }

    @Override
    public Result indexDocument(IndexedDocument document) throws IOException {
        IndexResponse response = esClient.index(i -> i
                .index(ElasticsearchConfig.DOCUMENTS_INDEX_NAME)
                .id(document.getId().toString())
                .document(document)
        );

        String logMsg = "Indexed document " + document.getId() + ": result=" + response.result() + ", index=" + response.index();
        if (response.result() != Result.Created && response.result() != Result.Updated) {
            log.error("Failed to " + logMsg);
        } else {
            log.info(logMsg);
        }
        return response.result();
    }

    @Override
    public Optional<IndexedDocument> getDocumentById(long id) {
        try {
            GetResponse<IndexedDocument> response = esClient.get(g -> g
                            .index(ElasticsearchConfig.DOCUMENTS_INDEX_NAME)
                            .id(String.valueOf(id)),
                    IndexedDocument.class
            );
            return (response.found() && response.source() != null) ? Optional.of(response.source()) : Optional.empty();
        } catch (IOException e) {
            log.error("Failed to get document id={} from elasticsearch: {}", id, e.toString());
            return Optional.empty();
        }
    }

    @Override
    public boolean deleteDocumentById(long id) {
        DeleteResponse result = null;
        try {
            result = esClient.delete(d -> d
                    .index(ElasticsearchConfig.DOCUMENTS_INDEX_NAME)
                    .id(String.valueOf(id))
            );
        } catch (IOException e) {
            log.warn("Failed to delete document id={} from elasticsearch: {}", id, e.toString());
        }

        if (result == null) return false;

        if (result.result() != Result.Deleted) {
            log.warn(result.toString());
        }
        return result.result() == Result.Deleted;
    }

    @Override
    public List<SearchHit> search(String query, List<String> tags, int limit) throws IOException {
        int size = Math.max(1, Math.min(limit, 100));
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasTags = tags != null && !tags.isEmpty();
        SearchResponse<IndexedDocument> resp = esClient.search(s -> s
                        .index(ElasticsearchConfig.DOCUMENTS_INDEX_NAME)
                        .size(size)
                        .query(q -> {
                            // 1) no query and no tags -> match_all
                            if (!hasQuery && !hasTags) {
                                return q.matchAll(m -> m);
                            }
                            // 2) build bool query
                            return q.bool(b -> {
                                if (hasQuery) {
                                    b.must(m -> m.multiMatch(mm -> mm
                                            .query(query)
                                            .fields("content", "filename", "tags")
                                    ));
                                } else {
                                    b.must(m -> m.matchAll(ma -> ma));
                                }
                                if (hasTags) {
                                    List<FieldValue> vals = tags.stream()
                                            .filter(t -> t != null && !t.isBlank())
                                            .map(FieldValue::of)
                                            .toList();
                                    b.filter(f -> f.terms(t -> t
                                            .field("tags.keyword")
                                            .terms(v -> v.value(vals))
                                    ));
                                }
                                return b;
                            });
                        }),
                IndexedDocument.class
        );
        List<SearchHit> hits = new ArrayList<>();
        for (var h : resp.hits().hits()) {
            if (h == null || h.id() == null) continue;
            try {
                Long id = Long.parseLong(h.id());
                Double score = h.score() != null ? h.score() : 0.0;
                hits.add(new SearchHit(id, score));
            } catch (NumberFormatException ignored) {
            }
        }
        return hits;
    }
}
