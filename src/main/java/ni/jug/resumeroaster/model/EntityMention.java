package ni.jug.resumeroaster.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author jxareas
 */
public record EntityMention(String text, String type, double confidence, int count, DetectionMethod detectionMethod) {

    public EntityMention {
        confidence = Math.round(confidence * 100.0) / 100.0;
    }

    public static List<EntityMention> deduplicate(List<EntityMention> entities) {
        return entities.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(e -> Map.entry(e.text().strip(), e.type())),
                        groups -> groups.values().stream()
                                .map(group -> {
                                    EntityMention first = group.getFirst();
                                    return new EntityMention(
                                            first.text().strip(),
                                            first.type(),
                                            first.confidence(),
                                            group.size(),
                                            first.detectionMethod()
                                    );
                                })
                                .sorted((a, b) -> Double.compare(b.confidence(), a.confidence()))
                                .toList()
                ));
    }
}
