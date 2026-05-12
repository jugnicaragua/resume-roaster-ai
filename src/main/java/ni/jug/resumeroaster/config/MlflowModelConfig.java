package ni.jug.resumeroaster.config;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author jxareas
 */
@Configuration
@EnableConfigurationProperties(MlflowProperties.class)
public class MlflowModelConfig {

    @Bean
    public Path onnxModelPath(MlflowProperties props, ObjectMapper objectMapper) throws IOException {
        Path cacheDir = Path.of(System.getProperty("user.home"),
                ".cache", "mlflow", props.getModelName());

        if (Files.exists(cacheDir)) {
            try (Stream<Path> entries = Files.list(cacheDir)) {
                if (entries.findAny().isPresent()) return cacheDir;
            }
        }
        Files.createDirectories(cacheDir);

        String basicAuth = Base64.getEncoder().encodeToString(
                (props.getUsername() + ":" + props.getPassword()).getBytes(StandardCharsets.UTF_8));

        HttpClient http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        String baseUri = props.getTrackingUri().replaceAll("/+$", "");

        // Step 1: resolve run_id and artifact relative path from the model version
        String mvUrl = baseUri
                + "/api/2.0/mlflow/model-versions/get?name="
                + encode(props.getModelName())
                + "&version=" + encode(props.getModelVersion());

        HttpResponse<String> mvResp = send(http, mvUrl, basicAuth);
        JsonNode mv = objectMapper.readTree(mvResp.body()).path("model_version");
        String runId = mv.path("run_id").asText();
        String artifactRelPath = artifactRelPath(mv.path("source").asText());

        // Step 2: list files inside the model artifact directory
        String listUrl = baseUri
                + "/api/2.0/mlflow/artifacts/list?run_id=" + runId
                + "&path=" + encode(artifactRelPath);

        HttpResponse<String> listResp = send(http, listUrl, basicAuth);
        JsonNode fileNodes = objectMapper.readTree(listResp.body()).path("files");

        List<String> filePaths = new ArrayList<>();
        for (JsonNode node : fileNodes) {
            if (!node.path("is_dir").asBoolean()) {
                filePaths.add(node.path("path").asText());
            }
        }

        // Step 3: download each artifact file into the cache directory
        for (String filePath : filePaths) {
            String downloadUrl = baseUri
                    + "/get-artifact?run_uuid=" + runId
                    + "&path=" + encode(filePath);

            String fileName = Path.of(filePath).getFileName().toString();
            try {
                http.send(
                        HttpRequest.newBuilder(URI.create(downloadUrl))
                                .header("Authorization", "Basic " + basicAuth)
                                .GET().build(),
                        HttpResponse.BodyHandlers.ofFile(cacheDir.resolve(fileName)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while downloading " + fileName, e);
            }
        }

        return cacheDir;
    }

    private static HttpResponse<String> send(HttpClient http, String url, String basicAuth)
            throws IOException {
        try {
            HttpResponse<String> resp = http.send(
                    HttpRequest.newBuilder(URI.create(url))
                            .header("Authorization", "Basic " + basicAuth)
                            .GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new IOException("MLflow API " + resp.statusCode() + " for " + url + ": " + resp.body());
            }
            return resp;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted calling " + url, e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Extracts the artifact path relative to the run's artifact root from a model version source URI.
     * Example: "mlflow-artifacts:/0/abc123/artifacts/model" → "model"
     */
    private static String artifactRelPath(String source) {
        int idx = source.indexOf("/artifacts/");
        return idx >= 0 ? source.substring(idx + "/artifacts/".length()) : "";
    }
}
