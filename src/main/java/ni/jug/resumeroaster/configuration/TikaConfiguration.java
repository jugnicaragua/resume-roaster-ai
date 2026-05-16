package ni.jug.resumeroaster.configuration;

import org.apache.tika.Tika;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jxareas
 */
@Configuration(proxyBeanMethods = false)
public class TikaConfiguration {

    @Bean
    public Tika tika() {
        return new Tika();
    }
}
