package paperless.paperless.infrastructure;

import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import paperless.paperless.config.MinioConfig;

import java.io.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final MinioClient minioClient;
    private final MinioConfig config;

    @Autowired
    public FileStorageService(MinioClient minioClient, MinioConfig config) {
        this.minioClient = minioClient;
        this.config = config;
    }

    public String uploadFile(String originalFilename, byte[] bytes) {
        String cleanFilename = (originalFilename == null || originalFilename.isBlank())
                ? "unnamed.pdf"
                : originalFilename;
        String objectKey = UUID.randomUUID() + "_" + cleanFilename;

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(config.getBucketName()).build()
            );
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(config.getBucketName()).build());
                log.info("Created bucket '{}'", config.getBucketName());
            }

            try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(config.getBucketName())
                                .object(objectKey)
                                .stream(in, bytes.length, -1)
                                .contentType("application/pdf")
                                .build()
                );
            }

            log.info("Uploaded '{}' to MinIO bucket '{}'", objectKey, config.getBucketName());
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("MinIO upload failed: " + e.getMessage(), e);
        }
    }

    public byte[] downloadFile(String objectKey) {
        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(config.getBucketName())
                        .object(objectKey)
                        .build()
        )) {
            return IOUtils.toByteArray(response);
        } catch (Exception e) {
            throw new RuntimeException("MinIO download failed: " + e.getMessage(), e);
        }
    }
}