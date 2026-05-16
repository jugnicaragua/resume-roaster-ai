package ni.jug.resumeroaster.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author jxareas
 */
@Data
@Validated
@ConfigurationProperties(prefix = "mlflow")
public class MlflowConfigurationProperties {

    @NotBlank
    private String trackingUri;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String modelName;

    @NotBlank
    private String modelVersion;

    private int modelMaxLength = 128;
}
