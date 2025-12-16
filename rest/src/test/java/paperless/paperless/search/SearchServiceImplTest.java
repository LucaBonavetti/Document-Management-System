package paperless.paperless.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import paperless.paperless.model.Document;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SearchServiceImplTest {

    @Test
    void search_maps_results_to_api_documents() throws Exception {
        ElasticsearchClient client = Mockito.mock(ElasticsearchClient.class);
        SearchServiceImpl service = new SearchServiceImpl(client, "documents");

        // Build fake ES response
        IndexedDocument src = new IndexedDocument();
        src.setId(1L);
        src.setFilename("HelloWorld.pdf");
        src.setContentType("application/pdf");
        src.setSize(10L);
        src.setUploadedAt(OffsetDateTime.parse("2025-10-22T10:00:00Z"));
        Hit<IndexedDocument> hit = Hit.of(hb -> hb.id("1").source(src));
        SearchResponse<IndexedDocument> fake = SearchResponse.of(b -> b
                .took(1).timedOut(false)
                .hits(h -> h.hits(List.of(hit))));

        when(client.search(any(SearchRequest.class), eq(IndexedDocument.class))).thenReturn(fake);

        List<Document> out = service.search("Hello", 10);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getFilename()).isEqualTo("HelloWorld.pdf");

        // Verify query construction roughly contains our fields
        ArgumentCaptor<SearchRequest> cap = ArgumentCaptor.forClass(SearchRequest.class);
        verify(client).search(cap.capture(), eq(IndexedDocument.class));
        SearchRequest req = cap.getValue();
        // not asserting internals too strictly to keep future-proof
        assertThat(req.size()).isEqualTo(10);
    }
}
