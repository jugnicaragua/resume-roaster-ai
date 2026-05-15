package ni.jug.resumeroaster.model;

import java.util.Comparator;

/**
 * Defines the field by which {@link EntityMention} results are sorted in a NER response.
 *
 * @author jxareas
 */
public enum NerSortField {

    /** Sort entities by model confidence score (descending — highest first). */
    CONFIDENCE {
        @Override
        public Comparator<EntityMention> comparator() {
            return Comparator.comparingDouble(EntityMention::confidence).reversed();
        }
    },

    /** Sort entities by occurrence count (descending — most frequent first). */
    COUNT {
        @Override
        public Comparator<EntityMention> comparator() {
            return Comparator.comparingInt(EntityMention::count).reversed();
        }
    };

    public abstract Comparator<EntityMention> comparator();
}
