package paperless.paperless.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(UUID id) {
        super("Document" + id + " not found");
    }
}
