package ni.jug.resumeroaster.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jxareas
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public String health() {
        return "CV Roaster API is alive";
    }
}
