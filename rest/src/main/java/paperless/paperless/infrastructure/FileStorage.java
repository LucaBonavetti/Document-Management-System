package paperless.paperless.infrastructure;

public interface FileStorage {
    String upload(String objectName, byte[] data);
    byte[] download(String objectName);
}