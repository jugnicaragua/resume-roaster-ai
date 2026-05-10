package ni.jug.resumeroaster.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jxareas
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Resume Roaster AI")
                        .description("This is a simple REST API that accepts a CV/resume and returns AI-generated roast feedback with configurable personalities.")
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact()
                                .name("jxareas")
                                .url("https://github.com/jxareas"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
