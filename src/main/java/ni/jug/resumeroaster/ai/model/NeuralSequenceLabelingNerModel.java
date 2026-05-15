package ni.jug.resumeroaster.ai.model;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.huggingface.tokenizers.jni.CharSpan;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import ni.jug.resumeroaster.config.DjlConfiguration;
import tools.jackson.databind.ObjectMapper;
import ni.jug.resumeroaster.model.EntityMention;
import ni.jug.resumeroaster.model.NerResponse;
import ni.jug.resumeroaster.model.NerSource;
import ni.jug.resumeroaster.ai.annotations.NeuralNer;
import org.springframework.beans.factory.annotation.Qualifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NER inference service running a neural sequence-labeling pipeline over an ONNX-exported
 * transformer encoder via Deep Java Library (DJL) with the ONNX Runtime execution provider.
 *
 * <h2>Pipeline</h2>
 * <ol>
 *   <li><b>Tokenization</b> — HuggingFace subword tokenizer produces {@code input_ids},
 *       {@code attention_mask}, {@code special_token_mask}, per-token {@link CharSpan}s, and
 *       raw token strings. The {@code [CLS]}/{@code [SEP]} boundaries are tracked via
 *       {@code special_token_mask} so they are skipped during decoding.</li>
 *   <li><b>Sequence alignment</b> — The ONNX graph is compiled for a fixed input shape
 *       ({@code [1, MODEL_MAX_LENGTH]}, no {@code dynamic_axes}). Sequences shorter than
 *       {@link #MODEL_MAX_LENGTH} are zero-padded via {@link Arrays#copyOf}; longer sequences
 *       are silently truncated. {@code processLen} tracks how many token positions carry real
 *       signal so the output arrays are sliced back to that length before BIO decoding.</li>
 *   <li><b>Forward pass</b> — {@code NDManager} and {@code Predictor} are opened as
 *       try-with-resources to guarantee native memory release. Named {@code NDArray}s
 *       ({@code input_ids}, {@code attention_mask}) are passed as an {@link NDList}; the model
 *       returns logits shaped {@code [1, MODEL_MAX_LENGTH, num_labels]}.</li>
 *   <li><b>Label decoding</b> — The batch dimension is squeezed to
 *       {@code [MODEL_MAX_LENGTH, num_labels]}, softmax is applied along axis 1 to get
 *       per-token label distributions, then argmax and max are taken along the same axis to
 *       produce the predicted label index and its probability for each token position.</li>
 *   <li><b>BIO span extraction</b> — see {@link #extractEntities}.</li>
 * </ol>
 *
 * <h2>Label set</h2>
 * Resolved at construction time from the {@code id2label} field in {@code config.json}.
 * BIO prefixes ({@code B-}, {@code I-}) are interpreted structurally; the entity type string
 * is passed through as-is to {@link EntityMention}.
 *
 * <h2>Thread safety</h2>
 * {@link ZooModel} and {@link HuggingFaceTokenizer} are thread-safe; a new {@link Predictor}
 * is created per call to {@link #infer}, so this service is safe for concurrent use.
 *
 * @author jxareas
 * @see DjlConfiguration
 * @see NerSource#DEEP_LEARNING
 */
@NeuralNer
public class NeuralSequenceLabelingNerModel implements NerModel {

    /**
     * DJL model wrapper around the ONNX Runtime session. Holds the loaded ONNX graph and
     * the raw-NDList translator (identity pass-through — tokenization and decoding are done
     * manually in this service, not via a DJL {@code Translator}).
     */
    private final ZooModel<NDList, NDList> nerZooModel;

    /**
     * HuggingFace subword tokenizer loaded from the {@code tokenizer.json} bundled with the
     * model artifact. Produces {@code input_ids}, {@code attention_mask}, {@code special_token_mask},
     * character-level span offsets, and the raw token strings needed for subword continuation
     * merging (e.g., {@code ##}-prefixed WordPiece tokens).
     */
    private final HuggingFaceTokenizer tokenizer;

    /**
     * Immutable mapping from integer label index to BIO label string (e.g., {@code 1 → "B-PER"}).
     * Loaded once from {@code config.json} at construction time; key order is preserved via
     * {@link LinkedHashMap} before the map is wrapped in {@link Collections#unmodifiableMap}.
     */
    private final Map<Integer, String> id2label;

    /**
     * Fixed sequence length the ONNX graph was compiled for.
     * The model was exported with {@code max_length=128, padding="max_length"} and without
     * dynamic axes, so every input tensor must be exactly {@code [1, 128]}. Inputs are padded
     * with zeros or truncated to this length before inference; outputs are sliced back to the
     * number of real (non-padded) tokens before BIO decoding.
     */
    private static final int MODEL_MAX_LENGTH = 128;

    /**
     * Constructs the service, wiring in the pre-loaded DJL model and tokenizer, and eagerly
     * deserializing the {@code id2label} map from the model's {@code config.json}.
     *
     * @param nerZooModel    DJL {@link ZooModel} wrapping the ONNX Runtime session
     * @param nerTokenizer   HuggingFace WordPiece tokenizer for the same checkpoint
     * @param onnxModelPath  path to the local directory containing {@code model.onnx} and
     *                       {@code config.json}, resolved from the MLflow artifact download
     * @param objectMapper   Jackson {@link ObjectMapper} for deserializing {@code config.json}
     */
    public NeuralSequenceLabelingNerModel(
            ZooModel<NDList, NDList> nerZooModel,
            HuggingFaceTokenizer nerTokenizer,
            @Qualifier("onnxModelPath") Path onnxModelPath,
            ObjectMapper objectMapper) {
        this.nerZooModel = nerZooModel;
        this.tokenizer = nerTokenizer;
        this.id2label = loadId2Label(onnxModelPath.resolve("config.json"), objectMapper);
    }

    /**
     * Runs the full inference pipeline on a raw text string and returns all detected entity spans.
     *
     * <p>The sequence is encoded with the HuggingFace WordPiece tokenizer, padded or truncated
     * to {@link #MODEL_MAX_LENGTH}, forwarded through the ONNX graph, and decoded
     * from per-token logits into {@link EntityMention} spans via BIO extraction.
     *
     * <p>Tokens beyond position {@link #MODEL_MAX_LENGTH} are silently dropped. For resume-length
     * inputs this is rarely a concern in practice, but callers processing arbitrarily long documents
     * should chunk their input before calling this method.
     *
     * @param text the raw input string to run NER over
     * @return a {@link NerResponse} containing the original text and all extracted entity mentions,
     *         in the order they appear in the text
     * @throws RuntimeException wrapping a {@link TranslateException} if ONNX Runtime inference fails
     */
    public NerResponse infer(String text) {
        Encoding encoding = tokenizer.encode(text);

        long[] inputIds = encoding.getIds();
        long[] attentionMask = encoding.getAttentionMask();
        long[] specialTokenMask = encoding.getSpecialTokenMask();
        CharSpan[] charSpans = encoding.getCharTokenSpans();
        String[] tokens = encoding.getTokens();

        // Number of real tokens to extract entities from (capped at model's fixed length)
        int processLen = Math.min(inputIds.length, MODEL_MAX_LENGTH);

        // Pad (or truncate) to the model's fixed sequence length
        long[] paddedInputIds = Arrays.copyOf(inputIds, MODEL_MAX_LENGTH);
        long[] paddedAttentionMask = Arrays.copyOf(attentionMask, MODEL_MAX_LENGTH);

        try (NDManager manager = NDManager.newBaseManager();
             Predictor<NDList, NDList> predictor = nerZooModel.newPredictor()) {

            NDArray inputIdsArr = manager.create(new long[][]{paddedInputIds});
            inputIdsArr.setName("input_ids");
            NDArray attentionMaskArr = manager.create(new long[][]{paddedAttentionMask});
            attentionMaskArr.setName("attention_mask");

            NDList output = predictor.predict(new NDList(inputIdsArr, attentionMaskArr));

            // [1, MODEL_MAX_LENGTH, num_labels] → [MODEL_MAX_LENGTH, num_labels]
            NDArray logits = output.getFirst().squeeze(0);
            NDArray probs = logits.softmax(1);
            long[] labelIds = Arrays.copyOf(probs.argMax(1).toLongArray(), processLen);
            float[] confidences = Arrays.copyOf(probs.max(new int[]{1}).toFloatArray(), processLen);

            return new NerResponse(text, extractEntities(text, labelIds, confidences, specialTokenMask, charSpans, tokens));

        } catch (TranslateException e) {
            throw new RuntimeException("DJL NER inference failed", e);
        }
    }

    /**
     * Decodes per-token BIO label predictions into contiguous {@link EntityMention} spans,
     * handling WordPiece subword continuation tokens and special tokens.
     *
     * <h3>BIO decoding</h3>
     * <ul>
     *   <li>{@code B-X} — opens a new span of entity type {@code X}, flushing any previously
     *       open span first.</li>
     *   <li>{@code I-X} — extends the current open span; only accepted when a span of type
     *       {@code X} is already open (stray {@code I-} tokens without a preceding {@code B-}
     *       are treated as {@code O}).</li>
     *   <li>{@code O} — closes any open span.</li>
     * </ul>
     *
     * <h3>WordPiece subword merging</h3>
     * WordPiece splits OOV words into subword units marked with a {@code ##} prefix (e.g.,
     * {@code "JUG"} → {@code ["J", "##U", "##G"]}). The model assigns a label to every subword,
     * but only the first subword's label is semantically meaningful for span detection. Subsequent
     * {@code ##} tokens are merged into the currently open span: their {@link CharSpan#getEnd()}
     * position extends the entity boundary and their softmax confidence is folded into the running
     * average, ensuring the final confidence reflects the model's certainty across the full word.
     *
     * <h3>Confidence scoring</h3>
     * Entity confidence is the arithmetic mean of the per-token softmax max over all constituent
     * tokens (including {@code ##} continuations). This gives a token-averaged probability rather
     * than a single-token proxy.
     *
     * @param text              original input string; used to extract the surface form via
     *                          {@link String#substring} over character offsets
     * @param labelIds          argmax label index per token position, length {@code processLen}
     * @param confidences       softmax max (peak probability) per token position, length {@code processLen}
     * @param specialTokenMask  1 for {@code [CLS]}/{@code [SEP]}/padding positions, 0 otherwise;
     *                          positions where this is 1 are unconditionally skipped
     * @param charSpans         character-level start/end offsets per token into {@code text}
     * @param tokens            raw token strings from the tokenizer; used to detect {@code ##} subwords
     * @return list of {@link EntityMention}s in text order, each carrying its surface form,
     *         entity type, averaged confidence, character offsets, and {@link NerSource#DEEP_LEARNING}
     */
    private List<EntityMention> extractEntities(
            String text, long[] labelIds, float[] confidences,
            long[] specialTokenMask, CharSpan[] charSpans, String[] tokens) {

        List<EntityMention> entities = new ArrayList<>();
        String currentType = null;
        int entityStart = -1;
        int entityEnd = -1;
        double confidenceSum = 0.0;
        int tokenCount = 0;

        for (int i = 0; i < labelIds.length; i++) {
            if (specialTokenMask[i] == 1) continue;

            // WordPiece subword tokens (##suffix) are always merged into the current entity.
            // The model assigns per-subtoken labels, but only the first subword of a word is
            // meaningful; subsequent subtokens should extend whatever span is already open.
            if (tokens[i].startsWith("##")) {
                if (currentType != null) {
                    entityEnd = charSpans[i].getEnd();
                    confidenceSum += confidences[i];
                    tokenCount++;
                }
                continue;
            }

            String label = id2label.getOrDefault((int) labelIds[i], "O");
            int tokenStart = charSpans[i].getStart();
            int tokenEnd = charSpans[i].getEnd();

            if (label.startsWith("B-")) {
                if (currentType != null) {
                    entities.add(buildEntity(text, currentType, entityStart, entityEnd,
                            confidenceSum / tokenCount));
                }
                currentType = label.substring(2);
                entityStart = tokenStart;
                entityEnd = tokenEnd;
                confidenceSum = confidences[i];
                tokenCount = 1;

            } else if (label.startsWith("I-") && currentType != null) {
                entityEnd = tokenEnd;
                confidenceSum += confidences[i];
                tokenCount++;

            } else {
                if (currentType != null) {
                    entities.add(buildEntity(text, currentType, entityStart, entityEnd,
                            confidenceSum / tokenCount));
                    currentType = null;
                }
            }
        }

        if (currentType != null) {
            entities.add(buildEntity(text, currentType, entityStart, entityEnd,
                    confidenceSum / tokenCount));
        }

        return entities;
    }

    /**
     * Constructs an {@link EntityMention} from a decoded BIO span, extracting the surface form
     * directly from the original text using character offsets rather than reconstructing it from
     * tokens (which would require de-wordpiece-ing and whitespace heuristics).
     *
     * @param text       original input string
     * @param type       entity type string as defined in the model's {@code id2label} (e.g., {@code "PER"}, {@code "ORG"})
     * @param start      inclusive character start offset in {@code text}
     * @param end        exclusive character end offset in {@code text}
     * @param confidence token-averaged softmax max probability for this span
     * @return a fully populated {@link EntityMention} tagged with {@link NerSource#DEEP_LEARNING}
     */
    private static EntityMention buildEntity(
            String text, String type, int start, int end, double confidence) {
        return new EntityMention(text.substring(start, end), type, confidence,
                NerSource.DEEP_LEARNING, start, end);
    }

    /**
     * Deserializes the {@code id2label} field from a HuggingFace {@code config.json} file into
     * an integer-keyed map. HuggingFace serializes label ids as JSON string keys (e.g.,
     * {@code {"0": "O", "1": "B-PER", ...}}), so keys are parsed to {@code int} during loading.
     * Insertion order is preserved via {@link LinkedHashMap} and the result is wrapped in an
     * unmodifiable view.
     *
     * @param configPath    path to {@code config.json} inside the downloaded model artifact directory
     * @param objectMapper  Jackson mapper used to deserialize the JSON file
     * @return unmodifiable map from label id to BIO label string; empty map if {@code id2label}
     *         is absent from the config
     */
    @SuppressWarnings("unchecked")
    private static Map<Integer, String> loadId2Label(Path configPath, ObjectMapper objectMapper) {
        Map<String, Object> config = objectMapper.readValue(configPath.toFile(), Map.class);
        Map<String, String> raw = (Map<String, String>) config.get("id2label");
        if (raw == null) return Map.of();
        Map<Integer, String> result = new LinkedHashMap<>();
        raw.forEach((k, v) -> result.put(Integer.parseInt(k), v));
        return Collections.unmodifiableMap(result);
    }
}
