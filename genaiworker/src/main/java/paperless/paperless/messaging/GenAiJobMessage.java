package paperless.paperless.messaging;

public class GenAiJobMessage {

    private Long documentId;
    private String storedTextPath;

    public GenAiJobMessage() {}

    public GenAiJobMessage(Long documentId, String storedTextPath) {
        this.documentId = documentId;
        this.storedTextPath = storedTextPath;
    }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long id) { this.documentId = id; }

    public String getStoredTextPath() { return storedTextPath; }
    public void setStoredTextPath(String storedTextPath) { this.storedTextPath = storedTextPath; }
}