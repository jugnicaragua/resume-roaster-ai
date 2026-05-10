package ni.jug.resumeroaster.config;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Properties;

/**
 * @author jxareas
 */
@Configuration
@EnableConfigurationProperties(CoreNlpProperties.class)
public class CoreNlpConfig {

    @Bean
    @Lazy
    public StanfordCoreNLP stanfordCoreNLP(CoreNlpProperties properties) {
        Properties nlpProps = new Properties();
        nlpProps.setProperty("annotators", properties.getAnnotators());
        nlpProps.setProperty("ner.useSUTime", String.valueOf(properties.isUseSuTime()));
        properties.getExtraProperties().forEach(nlpProps::setProperty);
        return new StanfordCoreNLP(nlpProps);
    }
}
