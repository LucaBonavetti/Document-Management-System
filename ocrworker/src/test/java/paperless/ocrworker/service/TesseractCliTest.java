package paperless.ocrworker.service;

import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import paperless.ocrworker.config.MinioConfig;
import paperless.ocrworker.messaging.OcrResultProducer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class OcrWorkerServiceLiteTest {

    private OcrWorkerService svc;

    // reflect private methods we want to probe
    private Method mTesseractCmd;
    private Method mGetExt;
    private Method mIsPdf;
    private Method mToGrayscale;

    @BeforeEach
    void setUp() throws Exception {
        MinioClient minio = Mockito.mock(MinioClient.class);
        MinioConfig cfg = Mockito.mock(MinioConfig.class);
        RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        OcrResultProducer producer = Mockito.mock(OcrResultProducer.class);

        svc = new OcrWorkerService(minio, cfg, rabbitTemplate, producer);

        mTesseractCmd = OcrWorkerService.class.getDeclaredMethod("tesseractCmd");
        mTesseractCmd.setAccessible(true);

        mGetExt = OcrWorkerService.class.getDeclaredMethod("getExt", String.class);
        mGetExt.setAccessible(true);

        mIsPdf = OcrWorkerService.class.getDeclaredMethod("isPdf", String.class);
        mIsPdf.setAccessible(true);

        mToGrayscale = OcrWorkerService.class.getDeclaredMethod("toGrayscale", BufferedImage.class);
        mToGrayscale.setAccessible(true);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("ocr.tesseract-cmd");
    }

    @Test
    void tesseractCmd_usesSystemProperty_whenProvided() throws Exception {
        System.setProperty("ocr.tesseract-cmd", "C:\\dummy\\tess.exe");
        String resolved = (String) mTesseractCmd.invoke(svc);
        assertEquals("C:\\dummy\\tess.exe", resolved);
    }

    @Test
    void getExt_and_isPdf_behaveAsExpected() throws Exception {
        assertEquals(".pdf", (String) mGetExt.invoke(svc, "doc.pdf"));
        assertEquals(".bin", (String) mGetExt.invoke(svc, (Object) null));
        assertEquals(".noext", (String) mGetExt.invoke(svc, "name.noext"));
        assertEquals(".bin", (String) mGetExt.invoke(svc, "no-dot"));

        assertTrue((Boolean) mIsPdf.invoke(svc, ".PDF"));   // case-insensitive
        assertFalse((Boolean) mIsPdf.invoke(svc, ".png"));
    }

    @Test
    void toGrayscale_preservesSize_andTypeIsGray() throws Exception {
        BufferedImage rgb = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, 40, 20);
        } finally {
            g.dispose();
        }

        BufferedImage gray = (BufferedImage) mToGrayscale.invoke(svc, rgb);
        assertNotNull(gray);
        assertEquals(40, gray.getWidth());
        assertEquals(20, gray.getHeight());
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, gray.getType());
    }
}