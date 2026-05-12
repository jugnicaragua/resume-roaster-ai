package ni.jug.resumeroaster.config;

import ai.djl.MalformedModelException;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author jxareas
 */
@Configuration
public class DjlNerConfig {

    @Bean
    public ZooModel<NDList, NDList> nerZooModel(Path onnxModelPath)
            throws ModelNotFoundException, MalformedModelException, IOException {
        Criteria<NDList, NDList> criteria = Criteria.builder()
                .setTypes(NDList.class, NDList.class)
                .optModelPath(onnxModelPath)
                .optModelName("model")
                .optEngine("OnnxRuntime")
                .build();
        return criteria.loadModel();
    }

    @Bean
    public HuggingFaceTokenizer nerTokenizer(Path onnxModelPath) throws IOException {
        return HuggingFaceTokenizer.newInstance(onnxModelPath);
    }
}
