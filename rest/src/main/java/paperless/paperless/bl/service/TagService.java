package paperless.paperless.bl.service;

import paperless.paperless.bl.model.BlTag;

import java.util.List;

public interface TagService {

    List<BlTag> getTagsForDocument(Long documentId);

    List<BlTag> setTagsForDocument(Long documentId, List<String> rawTagNames);

    BlTag addTagToDocument(Long documentId, String rawTagName);

    void removeTagFromDocument(Long documentId, String rawTagName);
}
