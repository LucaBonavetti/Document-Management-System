package paperless.paperless.bl.service;

import paperless.paperless.model.SearchDocumentResult;

import java.util.List;

public interface SearchService {
    List<SearchDocumentResult> search(String query, List<String> tags, int limit);
}
