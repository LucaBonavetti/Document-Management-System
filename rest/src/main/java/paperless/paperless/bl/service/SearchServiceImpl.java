package paperless.paperless.bl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.dal.entity.DocumentTagEntity;
import paperless.paperless.dal.repository.DocumentRepository;
import paperless.paperless.dal.repository.DocumentTagRepository;
import paperless.paperless.model.SearchDocumentResult;
import paperless.paperless.search.SearchIndexService;
import paperless.paperless.search.dto.SearchHit;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    private final SearchIndexService searchIndexService;
    private final DocumentRepository documentRepository;
    private final DocumentTagRepository documentTagRepository;

    public SearchServiceImpl(SearchIndexService searchIndexService,
                             DocumentRepository documentRepository,
                             DocumentTagRepository documentTagRepository) {
        this.searchIndexService = searchIndexService;
        this.documentRepository = documentRepository;
        this.documentTagRepository = documentTagRepository;
    }

    @Override
    public List<SearchDocumentResult> search(String query, List<String> tags, int limit) {
        int size = Math.max(1, Math.min(limit, 100));

        List<SearchHit> hits;
        try {
            hits = searchIndexService.search(query, tags, size);
        } catch (Exception e) {
            log.error("Elasticsearch search failed: {}", e.toString(), e);
            return List.of();
        }

        if (hits.isEmpty()) return List.of();

        List<Long> ids = hits.stream()
                .map(SearchHit::getId)
                .filter(Objects::nonNull)
                .toList();

        List<DocumentEntity> docs = documentRepository.findAllById(ids);
        Map<Long, DocumentEntity> docById = docs.stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(DocumentEntity::getId, d -> d, (a, b) -> a));

        List<SearchDocumentResult> out = new ArrayList<>();
        for (SearchHit h : hits) {
            if (h.getId() == null) continue;
            DocumentEntity doc = docById.get(h.getId());
            if (doc == null) continue;

            List<String> tagNames = loadTags(doc.getId());

            SearchDocumentResult r = new SearchDocumentResult();
            r.setId(doc.getId());
            r.setFilename(doc.getFilename());
            r.setContentType(doc.getContentType());
            r.setSize(doc.getSize());
            r.setUploadedAt(doc.getUploadedAt());
            r.setTags(tagNames);
            r.setScore(h.getScore());

            out.add(r);
        }

        return out;
    }

    private List<String> loadTags(Long documentId) {
        List<DocumentTagEntity> links = documentTagRepository.findByDocument_Id(documentId);
        return links.stream()
                .map(DocumentTagEntity::getTag)
                .filter(Objects::nonNull)
                .map(t -> t.getName() == null ? "" : t.getName())
                .filter(s -> !s.isBlank())
                .sorted()
                .toList();
    }
}
