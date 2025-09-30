package paperless.paperless.bl.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class BlUploadRequest {

    @NotBlank(message = "filename must not be blank")
    private String filename;

    private String contentType;

    @Positive(message = "size must be > 0")
    private long size;

    public BlUploadRequest() {}

    public BlUploadRequest(String filename, String contentType, long size) {
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
    }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}