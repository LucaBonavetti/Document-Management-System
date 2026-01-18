package paperless.paperless.bl.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import paperless.paperless.bl.mapper.TagMapper;
import paperless.paperless.bl.model.BlTag;
import paperless.paperless.dal.entity.DocumentEntity;
import paperless.paperless.dal.entity.DocumentTagEntity;
import paperless.paperless.dal.entity.TagEntity;
import paperless.paperless.dal.repository.DocumentRepository;
import paperless.paperless.dal.repository.DocumentTagRepository;
import paperless.paperless.dal.repository.TagRepository;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl implements TagService {

    // Business rules
    private static final int MAX_TAGS_PER_DOCUMENT = 10;
    private static final int MAX_TAG_LENGTH = 50;

    // normalized tag names: a-z 0-9 - _
    private static final Pattern TAG_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9_-]{0,49}$");

    private final DocumentRepository documentRepository;
    private final TagRepository tagRepository;
    private final DocumentTagRepository documentTagRepository;
    private final TagMapper tagMapper;
    private final DocumentIndexingService documentIndexingService;

    public TagServiceImpl(DocumentRepository documentRepository,
                          TagRepository tagRepository,
                          DocumentTagRepository documentTagRepository,
                          TagMapper tagMapper,
                          DocumentIndexingService documentIndexingService) {
        this.documentRepository = documentRepository;
        this.tagRepository = tagRepository;
        this.documentTagRepository = documentTagRepository;
        this.tagMapper = tagMapper;
        this.documentIndexingService = documentIndexingService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlTag> getTagsForDocument(Long documentId) {
        DocumentEntity doc = requireDocument(documentId);

        List<DocumentTagEntity> links = documentTagRepository.findByDocument_Id(doc.getId());
        List<TagEntity> tags = links.stream()
                .map(DocumentTagEntity::getTag)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TagEntity::getName))
                .collect(Collectors.toList());

        return tags.stream().map(tagMapper::toBl).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<BlTag> setTagsForDocument(Long documentId, List<String> rawTagNames) {
        DocumentEntity doc = requireDocument(documentId);

        List<String> desired = normalizeAndValidateList(rawTagNames);

        // load existing links
        List<DocumentTagEntity> existingLinks = documentTagRepository.findByDocument_Id(doc.getId());
        Map<String, DocumentTagEntity> existingByName = new HashMap<>();
        for (DocumentTagEntity link : existingLinks) {
            if (link.getTag() != null && link.getTag().getName() != null) {
                existingByName.put(link.getTag().getName(), link);
            }
        }

        // add missing links
        List<TagEntity> finalTags = new ArrayList<>();
        for (String name : desired) {
            DocumentTagEntity existing = existingByName.remove(name);
            if (existing != null) {
                finalTags.add(existing.getTag());
                continue;
            }

            TagEntity tag = getOrCreateTag(name);

            if (!documentTagRepository.existsByDocument_IdAndTag_Id(doc.getId(), tag.getId())) {
                DocumentTagEntity link = new DocumentTagEntity();
                link.setDocument(doc);
                link.setTag(tag);
                link.setAssignedAt(OffsetDateTime.now());
                documentTagRepository.save(link);
            }
            finalTags.add(tag);
        }

        // remaining entries in existingByName are not desired anymore -> delete
        if (!existingByName.isEmpty()) {
            documentTagRepository.deleteAll(existingByName.values());
        }

        // return in the same order as requested
        documentIndexingService.reindexDocument(doc.getId());
        return finalTags.stream().map(tagMapper::toBl).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BlTag addTagToDocument(Long documentId, String rawTagName) {
        DocumentEntity doc = requireDocument(documentId);

        // enforce max tags rule
        int current = documentTagRepository.findByDocument_Id(doc.getId()).size();
        if (current >= MAX_TAGS_PER_DOCUMENT) {
            throw new IllegalArgumentException("A document can have at most " + MAX_TAGS_PER_DOCUMENT + " tags.");
        }

        String name = normalizeAndValidateOne(rawTagName);
        TagEntity tag = getOrCreateTag(name);

        if (!documentTagRepository.existsByDocument_IdAndTag_Id(doc.getId(), tag.getId())) {
            DocumentTagEntity link = new DocumentTagEntity();
            link.setDocument(doc);
            link.setTag(tag);
            link.setAssignedAt(OffsetDateTime.now());
            documentTagRepository.save(link);
        }
        documentIndexingService.reindexDocument(doc.getId());
        return tagMapper.toBl(tag);
    }

    @Override
    @Transactional
    public void removeTagFromDocument(Long documentId, String rawTagName) {
        DocumentEntity doc = requireDocument(documentId);

        String name = normalizeAndValidateOne(rawTagName);

        TagEntity tag = tagRepository.findByName(name).orElse(null);
        if (tag == null) {
            return;
        }

        documentTagRepository.deleteByDocument_IdAndTag_Id(doc.getId(), tag.getId());
        documentIndexingService.reindexDocument(doc.getId());
    }

    // ---- helpers ----

    private DocumentEntity requireDocument(Long documentId) {
        if (documentId == null) throw new IllegalArgumentException("documentId must not be null");
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
    }

    private List<String> normalizeAndValidateList(List<String> raw) {
        if (raw == null) raw = List.of();

        // normalize + dedupe while keeping order
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String s : raw) {
            String n = normalizeAndValidateOne(s);
            set.add(n);
        }

        if (set.size() > MAX_TAGS_PER_DOCUMENT) {
            throw new IllegalArgumentException("A document can have at most " + MAX_TAGS_PER_DOCUMENT + " tags.");
        }

        return new ArrayList<>(set);
    }

    private String normalizeAndValidateOne(String raw) {
        if (raw == null) throw new IllegalArgumentException("Tag name must not be null");

        String n = raw.trim().toLowerCase(Locale.ROOT);
        n = n.replaceAll("\\s+", "-");

        if (n.isBlank()) throw new IllegalArgumentException("Tag name must not be blank");
        if (n.length() > MAX_TAG_LENGTH) throw new IllegalArgumentException("Tag name too long (max " + MAX_TAG_LENGTH + ")");
        if (!TAG_PATTERN.matcher(n).matches()) {
            throw new IllegalArgumentException("Invalid tag name '" + raw + "'. Allowed: a-z 0-9 '-' '_' (spaces become '-')");
        }

        return n;
    }

    private TagEntity getOrCreateTag(String normalizedName) {
        Optional<TagEntity> existing = tagRepository.findByName(normalizedName);
        if (existing.isPresent()) return existing.get();

        TagEntity tag = new TagEntity();
        tag.setName(normalizedName);
        tag.setCreatedAt(OffsetDateTime.now());

        try {
            return tagRepository.save(tag);
        } catch (DataIntegrityViolationException ex) {
            // race: someone created it in parallel
            return tagRepository.findByName(normalizedName)
                    .orElseThrow(() -> ex);
        }
    }
}
