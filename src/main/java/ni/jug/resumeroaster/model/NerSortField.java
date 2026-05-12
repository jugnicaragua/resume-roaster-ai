package ni.jug.resumeroaster.model;

import java.util.Comparator;

/**
 * Defines the field by which {@link EntityMention} results are sorted in a NER response.
 *
 * @author jxareas
 */
public enum NerSortField {

    /** Sort entities by their position in the source text (ascending). */
    START_OFFSET {
        @Override
        public Comparator<EntityMention> comparator() {
            return Comparator.comparingInt(EntityMention::startOffset);
        }
    },

    /** Sort entities by model confidence score (descending — highest first). */
    CONFIDENCE {
        @Override
        public Comparator<EntityMention> comparator() {
            return Comparator.comparingDouble(EntityMention::confidence).reversed();
        }
    };

    public abstract Comparator<EntityMention> comparator();
}
