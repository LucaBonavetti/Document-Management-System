package paperless.paperless.messaging;

import java.time.OffsetDateTime;

public class OcrResultMessage {

    private Long documentId;
    private String storedPath;
    private String textKey;
    private OffsetDateTime processedAt;

    public OcrResultMessage() { }

    public OcrResultMessage(Long documentId, String storedPath, String textKey, OffsetDateTime processedAt) {
        this.documentId = documentId;
        this.storedPath = storedPath;
        this.textKey = textKey;
        this.processedAt = processedAt;
    }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getStoredPath() { return storedPath; }
    public void setStoredPath(String storedPath) { this.storedPath = storedPath; }

    public String getTextKey() { return textKey; }
    public void setTextKey(String textKey) { this.textKey = textKey; }

    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
}
