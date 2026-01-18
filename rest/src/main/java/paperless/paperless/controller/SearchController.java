package paperless.paperless.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import paperless.paperless.bl.service.SearchService;
import paperless.paperless.model.SearchDocumentResult;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SearchDocumentResult>> search(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "tag", required = false) List<String> tags,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(searchService.search(query, tags, limit));
    }
}
