package paperless.paperless.search;

import paperless.paperless.model.Document;

import java.util.List;

public interface SearchService {
    List<Document> search(String query, int limit);
}
