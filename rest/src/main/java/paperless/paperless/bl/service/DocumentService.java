package paperless.paperless.bl.service;

import paperless.paperless.bl.model.BlDocument;
import java.io.IOException;

public interface DocumentService {
    BlDocument saveDocument(String filename, String contentType, long size, byte[] bytes) throws IOException;
    BlDocument getById(long id);
}