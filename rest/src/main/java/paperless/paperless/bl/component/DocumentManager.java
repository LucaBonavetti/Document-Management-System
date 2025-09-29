package paperless.paperless.bl.component;

import org.springframework.web.multipart.MultipartFile;
import paperless.paperless.bl.model.BlDocument;

import java.io.IOException;

public interface DocumentManager {
    BlDocument upload(MultipartFile file) throws IOException;
    BlDocument getById(long id);
}