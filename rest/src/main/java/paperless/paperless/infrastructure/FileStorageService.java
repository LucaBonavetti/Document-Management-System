package paperless.paperless.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private final Path storageDir = Paths.get("storage");

    public FileStorageService() throws IOException {
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
            log.info("Created storage directory at {}", storageDir.toAbsolutePath());
        }
    }

    public Path saveFile(String originalFilename, byte[] bytes) throws IOException {
        String cleanFilename = Path.of(originalFilename == null ? "unnamed" : originalFilename)
                .getFileName().toString();
        Path target = storageDir.resolve(System.currentTimeMillis() + "_" + cleanFilename);
        Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return target;
    }
}