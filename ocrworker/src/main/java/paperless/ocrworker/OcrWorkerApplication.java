package paperless.ocrworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OcrWorkerApplication {
    public static void main(String[] args) {
        System.setProperty("jna.nosys", "true");
        SpringApplication.run(OcrWorkerApplication.class, args);
    }
}