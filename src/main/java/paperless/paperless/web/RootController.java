package paperless.paperless.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class RootController {
    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of("app","paperless","status","up","docs","/api/documents");
    }
}
