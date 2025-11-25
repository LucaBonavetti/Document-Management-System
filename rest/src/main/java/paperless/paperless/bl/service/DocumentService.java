package paperless.paperless.bl.service;

import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.bl.model.BlUploadRequest;

import java.util.List;

public interface DocumentService {

    BlDocument saveDocument(BlUploadRequest request, byte[] data) throws Exception;

    BlDocument getById(Long id);

    List<BlDocument> getRecent(int limit);

    void updateSummary(Long documentId, String summary);
}
