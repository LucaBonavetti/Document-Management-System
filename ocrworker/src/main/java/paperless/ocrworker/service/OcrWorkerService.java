package paperless.ocrworker.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import paperless.ocrworker.config.MinioConfig;
import paperless.paperless.messaging.OcrJobMessage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

@Service
public class OcrWorkerService {
    private static final Logger log = LoggerFactory.getLogger(OcrWorkerService.class);

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    private static final int MINIO_MAX_ATTEMPTS = 3;
    private static final long MINIO_BACKOFF_MS = 300;

    @Value("${ocr.langs:eng}")
    private String ocrLangs;

    @Value("${ocr.psm:6}")
    private String ocrPsm;

    @Value("${ocr.dpi:300}")
    private int ocrDpi;

    @Value("${ocr.maxPages:3}")
    private int ocrMaxPages;

    @Value("${ocr.timeoutSeconds:60}")
    private long ocrTimeoutSeconds;

    @Value("${ocr.storeText:true}")
    private boolean storeTextToMinio;

    @Value("${ocr.tesseract-cmd:}")
    private String tessCmdProp;

    public OcrWorkerService(MinioClient minioClient, MinioConfig minioConfig) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
    }

    public void process(OcrJobMessage msg) {
        String bucket = minioConfig.getBucketName();
        String key = msg.getStoredPath();
        String filename = msg.getFilename();

        Path temp = null;
        try {
            String textKey = key + ".txt";
            if (storeTextToMinio && objectExists(bucket, textKey)) {
                log.info("Skip OCR; text already exists at '{}'", textKey);
                return;
            }

            // 1) Download object from MinIO
            String ext = getExt(filename);
            temp = Files.createTempFile("ocr_in_", ext);

            try (InputStream in = fetchFromMinioWithRetry(bucket, key)) {
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            } catch (ErrorResponseException e) {
                if ("NoSuchKey".equalsIgnoreCase(e.errorResponse().code())) {
                    log.warn("MinIO object not found: bucket='{}' key='{}' (filename='{}')", bucket, key, filename);
                    return;
                }
                log.error("MinIO error for '{}': {}", filename, e.errorResponse().code(), e);
                throw e;
            }

            // 2) OCR
            String text = isPdf(ext) ? ocrPdf(temp) : ocrImage(temp);

            // 3) Preview
            String preview = text == null ? "" : (text.length() > 400 ? text.substring(0, 400) + "..." : text);
            log.info("OCR result for '{}' (first 400 chars):\n{}", filename, preview);

            // 4) Store .txt next to original
            if (storeTextToMinio && text != null) {
                byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                putToMinioWithRetry(bucket, textKey, bytes);
                log.info("Stored OCR text to MinIO as '{}'", textKey);
            }

        } catch (Exception e) {
            log.error("OCR processing failed for '{}'", filename, e);
        } finally {
            if (temp != null) try { Files.deleteIfExists(temp); } catch (Exception ignored) {}
        }
    }

    // OCR core

    protected String ocrImage(Path imagePath) throws Exception {
        BufferedImage img = ImageIO.read(imagePath.toFile());
        if (img == null) throw new IOException("Unsupported image: " + imagePath);

        BufferedImage gray = toGrayscale(img);
        return doOcrBufferedImageViaCli(gray);
    }

    protected String ocrPdf(Path pdfPath) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int pages = Math.min(doc.getNumberOfPages(), Math.max(1, ocrMaxPages));
            for (int i = 0; i < pages; i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, ocrDpi, ImageType.RGB);
                BufferedImage gray = toGrayscale(img);
                String pageText = doOcrBufferedImageViaCli(gray);
                sb.append(pageText).append("\n");
            }
        }
        return sb.toString();
    }

    private BufferedImage toGrayscale(BufferedImage src) {
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        try { g.drawImage(src, 0, 0, null); } finally { g.dispose(); }
        return gray;
    }

    private String doOcrBufferedImageViaCli(BufferedImage img) throws Exception {
        Path tmpDir  = Files.createTempDirectory("tess_");
        Path inPng   = tmpDir.resolve("in.png");
        Path outBase = tmpDir.resolve("out"); // tesseract will create out.txt
        try {
            ImageIO.write(img, "png", inPng.toFile());

            Process p = new ProcessBuilder(
                    tesseractCmd(),
                    inPng.toString(),
                    outBase.toString(),
                    "-l", ocrLangs,
                    "--psm", ocrPsm,
                    "--oem", "1",
                    "--dpi", String.valueOf(Math.max(100, ocrDpi))
            ).redirectErrorStream(true).start();

            // capture CLI output (useful when it fails)
            StringBuilder cli = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) cli.append(line).append('\n');
            }

            boolean finished = p.waitFor(ocrTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new RuntimeException("tesseract timed out after " + ocrTimeoutSeconds + "s");
            }
            int code = p.exitValue();
            if (code != 0) {
                throw new RuntimeException("tesseract exited with " + code + ": " + cli);
            }

            Path outTxt = Path.of(outBase.toString() + ".txt");
            if (!Files.exists(outTxt)) {
                throw new IOException("tesseract did not produce output: " + outTxt);
            }
            return Files.readString(outTxt, StandardCharsets.UTF_8);

        } finally {
            try { Files.deleteIfExists(tmpDir.resolve("out.txt")); } catch (Exception ignored) {}
            try { Files.deleteIfExists(tmpDir.resolve("in.png")); } catch (Exception ignored) {}
            try { Files.deleteIfExists(tmpDir); } catch (Exception ignored) {}
        }
    }

    private boolean isPdf(String ext) {
        return ".pdf".equalsIgnoreCase(ext);
    }

    private String getExt(String filename) {
        if (filename == null) return ".bin";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".bin";
    }

    // MinIO helpers (original fetch + retry/exists wrappers)

    protected InputStream fetchFromMinio(String bucket, String key) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(key).build()
        );
    }

    private InputStream fetchFromMinioWithRetry(String bucket, String key) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= MINIO_MAX_ATTEMPTS; attempt++) {
            try {
                return fetchFromMinio(bucket, key);
            } catch (io.minio.errors.ErrorResponseException e) {
                // Bubble up "NoSuchKey" immediately; others may be transient
                if ("NoSuchKey".equalsIgnoreCase(e.errorResponse().code())) throw e;

                last = e;
                log.warn("MinIO (protocol) failure on getObject {}/{} attempt {}/{} — code={}. Retrying...",
                        bucket, key, attempt, MINIO_MAX_ATTEMPTS, e.errorResponse().code());
            } catch (Exception e) {
                last = e;
                log.warn("MinIO (client/io) failure on getObject {}/{} attempt {}/{} — {}. Retrying...",
                        bucket, key, attempt, MINIO_MAX_ATTEMPTS, e.toString());
            }

            Thread.sleep(MINIO_BACKOFF_MS * attempt); // simple linear backoff
        }
        throw last != null ? last : new IOException("MinIO getObject failed after retries");
    }

    private void putToMinioWithRetry(String bucket, String key, byte[] bytes) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= MINIO_MAX_ATTEMPTS; attempt++) {
            try (var bais = new ByteArrayInputStream(bytes)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(key)
                                .contentType("text/plain; charset=utf-8")
                                .stream(bais, bytes.length, -1)
                                .build()
                );
                return;
            } catch (Exception e) {
                last = e;
                log.warn("MinIO putObject attempt {}/{} failed ({}). Retrying...",
                        attempt, MINIO_MAX_ATTEMPTS, e.toString());
                Thread.sleep(MINIO_BACKOFF_MS * attempt);
            }
        }
        throw last != null ? last : new IOException("MinIO putObject failed after retries");
    }

    private boolean objectExists(String bucket, String key) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(key).build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equalsIgnoreCase(e.errorResponse().code())) {
                return false; // normal: not found
            }
            log.warn("MinIO statObject failed (protocol) bucket={} key={} code={}",
                    bucket, key, e.errorResponse().code());
            return false;
        } catch (Exception e) {
            log.warn("MinIO statObject failed (client/io) bucket={} key={} err={}",
                    bucket, key, e.toString());
            return false;
        }
    }

    private String tesseractCmd() {
        String p = System.getProperty("ocr.tesseract-cmd");
        if (p != null && !p.isBlank()) return p;
        if (tessCmdProp != null && !tessCmdProp.isBlank()) return tessCmdProp;
        String env = System.getenv("TESSERACT_CMD");
        if (env != null && !env.isBlank()) return env;
        return "tesseract";
    }
}