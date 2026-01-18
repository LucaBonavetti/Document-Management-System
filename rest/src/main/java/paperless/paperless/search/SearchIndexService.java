package paperless.paperless.search;

import co.elastic.clients.elasticsearch._types.Result;
import paperless.paperless.search.dto.IndexedDocument;

import java.io.IOException;
import java.util.Optional;

public interface SearchIndexService {

    Result indexDocument(IndexedDocument document) throws IOException;

    Optional<IndexedDocument> getDocumentById(long id);

    boolean deleteDocumentById(long id);
}
