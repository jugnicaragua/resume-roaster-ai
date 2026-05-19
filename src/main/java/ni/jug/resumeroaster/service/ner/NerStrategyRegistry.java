package ni.jug.resumeroaster.service.ner;

import ni.jug.resumeroaster.model.NerInferenceBackend;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author jxareas
 */
@Component
public class NerStrategyRegistry {

    private final Map<NerInferenceBackend, NerStrategy> strategies;

    public NerStrategyRegistry(List<NerStrategy> strategyList) {
        strategies = new EnumMap<>(NerInferenceBackend.class);
        strategyList.forEach(s -> strategies.put(s.backend(), s));

        for (NerInferenceBackend backend : NerInferenceBackend.values()) {
            if (!strategies.containsKey(backend)) {
                throw new IllegalStateException("No NerStrategy registered for backend: " + backend);
            }
        }
    }

    public NerStrategy get(NerInferenceBackend backend) {
        return strategies.get(backend);
    }
}
