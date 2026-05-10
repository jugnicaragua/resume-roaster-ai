package ni.jug.resumeroaster.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jxareas
 */
@RestController
@Tag(name = "Health", description = "Endpoint for checking the liveness of the API.")
public class HealthController {

    @Operation(summary = "Health Check", description = "Returns a simple message confirming the API is up and running.")
    @GetMapping("/api/health")
    public String health() {
        return "CV Roaster API is alive";
    }
}
