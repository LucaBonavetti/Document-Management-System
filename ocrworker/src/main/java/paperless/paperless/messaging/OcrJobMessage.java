package paperless.paperless.messaging;

import java.time.OffsetDateTime;

public class OcrJobMessage {
    private Long documentId;
    private String filename;
    private String contentType;
    private long size;
    private String storedPath;
    private OffsetDateTime uploadedAt;

    public OcrJobMessage() { }

    public OcrJobMessage(Long documentId, String filename, String contentType, long size, String storedPath, OffsetDateTime uploadedAt) {
        this.documentId = documentId;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.storedPath = storedPath;
        this.uploadedAt = uploadedAt;
    }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getStoredPath() { return storedPath; }
    public void setStoredPath(String storedPath) { this.storedPath = storedPath; }
    public OffsetDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(OffsetDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}