package gsrs.module.substance.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.springUtils.StaticContextAccessor;

import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * All {@code linkingID} manipulation for the substance exporters lives in this
 * one class, so the feature can be dropped as a whole if GSRS ever resolves
 * {@code linkingID} values natively.
 *
 * <p>Every export carries the substance references as nested JSON objects that
 * identify the referenced substance by its {@code refuuid}. GSRS itself derives
 * the {@code linkingID} of such a reference from the {@code approvalID}, the
 * {@code refuuid} or the name of the referenced substance, which is only useful
 * inside the deployment the export was taken from.</p>
 *
 * <p>When the export is configured with a code system name, this class replaces
 * every {@code linkingID} with the value {@code <codeSystem>:<code>}, where
 * {@code <code>} is the code of the referenced substance in that code system.
 * The referenced substance is looked up by its {@code refuuid} and the code is
 * read from the substance repository inside a dedicated read-only transaction,
 * because the codes of a substance are loaded lazily and the export runs outside
 * the transaction that loaded it.</p>
 *
 * @author Egor Puzanov
 */
@Slf4j
public class LinkingIDUpdater {

    /**
     * Name of the reference property holding the uuid of the referenced substance.
     */
    public static final String REF_UUID_PROPERTY = "refuuid";

    /**
     * Name of the reference property holding the linking identifier.
     */
    public static final String LINKING_ID_PROPERTY = "linkingID";

    /**
     * Name of the exporter parameter holding the code system to resolve.
     */
    public static final String PARAMETER_PROPERTY = "linkingIdCodeSystem";

    /**
     * Code type a code must have to be used as a linking identifier.
     */
    private static final String PRIMARY_CODE_TYPE = "PRIMARY";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String codeSystem;
    private final Function<String, String> codeResolver;
    private final Map<String, String> codeCache = new ConcurrentHashMap<>();

    /**
     * Creates an updater.
     *
     * @param codeSystem   the code system name the linking identifiers are taken
     *                     from; an empty value disables the updater
     * @param codeResolver resolves the code of the substance with the given
     *                     {@code refuuid} in the configured code system; may be
     *                     {@code null}, which disables the updater
     */
    public LinkingIDUpdater(String codeSystem, Function<String, String> codeResolver) {
        this.codeSystem = codeSystem == null ? "" : codeSystem.trim();
        this.codeResolver = codeResolver;
    }

    /**
     * Creates the updater for one export, or returns {@code null} when the
     * feature is disabled.
     *
     * <p>This is the single entry point for the feature: it decides whether a
     * code system is configured and the repository is available, opens the
     * read-only transaction the code lookups run in, and builds the resolver.
     * The whole DB access for {@code linkingID} therefore stays in this class.</p>
     *
     * <p>A code system passed in the export request's detailed parameters wins
     * over the configured default, so a single export can resolve a different
     * code system without changing the exporter configuration.</p>
     *
     * <p>The substance repository and the transaction manager are resolved from
     * the Spring context here, so the callers carry no {@code linkingID}
     * plumbing at all.</p>
     *
     * @param configuredCodeSystem the code system configured on the exporter,
     *                             may be {@code null} or empty
     * @param detailedParameters   the detailed parameters of the export request,
     *                             may be {@code null}
     * @return the updater, or {@code null} when the feature is disabled
     */
    public static LinkingIDUpdater create(String configuredCodeSystem, JsonNode detailedParameters) {
        String codeSystem = configuredCodeSystem;
        if (detailedParameters != null) {
            JsonNode requested = detailedParameters.get(PARAMETER_PROPERTY);
            if (requested != null && requested.isValueNode() && !requested.asText().trim().isEmpty()) {
                codeSystem = requested.asText();
            }
        }
        if (codeSystem == null || codeSystem.trim().isEmpty()) {
            return null;
        }
        SubstanceRepository repository = bean(SubstanceRepository.class);
        if (repository == null) {
            return null;
        }
        PlatformTransactionManager transactionManager = bean(PlatformTransactionManager.class);
        TransactionTemplate readTransaction = null;
        if (transactionManager != null) {
            readTransaction = new TransactionTemplate(transactionManager);
            readTransaction.setReadOnly(true);
            readTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        }
        final String system = codeSystem.trim();
        final TransactionTemplate transaction = readTransaction;
        return new LinkingIDUpdater(system, refuuid -> findCode(refuuid, system, repository, transaction));
    }

    /**
     * Resolves a Spring bean without failing the export when it is unavailable.
     *
     * @param type the bean type to look up
     * @return the bean, or {@code null} when it cannot be resolved
     */
    private static <T> T bean(Class<T> type) {
        try {
            return StaticContextAccessor.getBean(type);
        } catch (RuntimeException e) {
            log.warn("Unable to resolve Spring bean {}: {}", type.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Returns the code system name the linking identifiers are taken from.
     *
     * @return the configured code system name, never {@code null}
     */
    public String getCodeSystem() {
        return codeSystem;
    }

    /**
     * Tells whether this updater rewrites anything.
     *
     * @return {@code true} when a code system and a code resolver are available
     */
    public boolean isEnabled() {
        return !codeSystem.isEmpty() && codeResolver != null;
    }

    /**
     * Rewrites the {@code linkingID} of every substance reference in the given
     * JSON document.
     *
     * <p>A document that cannot be parsed is returned unchanged, so a broken
     * {@code linkingID} update never loses an export.</p>
     *
     * @param json the exported document, may be {@code null}
     * @return the rewritten document, or the input when nothing could be changed
     */
    public String apply(String json) {
        if (!isEnabled() || json == null || json.isEmpty()) {
            return json;
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            if (updateLinkingIDs(root) == 0) {
                return json;
            }
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            log.warn("Unable to update linkingID values for code system {}: {}", codeSystem, e.getMessage());
            return json;
        }
    }

    /**
     * Walks the given node and sets the {@code linkingID} of every object that
     * carries a {@code refuuid}.
     *
     * <p>The children are collected before the node is modified so that adding
     * the {@code linkingID} property cannot disturb the traversal.</p>
     *
     * @param node the node to visit, may be {@code null}
     * @return the number of rewritten {@code linkingID} values
     */
    private int updateLinkingIDs(JsonNode node) {
        if (node == null) {
            return 0;
        }
        List<JsonNode> children = new ArrayList<>();
        node.forEach(children::add);
        int updated = 0;
        if (node.isObject()) {
            JsonNode refuuid = node.get(REF_UUID_PROPERTY);
            if (refuuid != null && refuuid.isTextual()) {
                String linkingID = toLinkingID(refuuid.asText());
                if (linkingID != null) {
                    ((ObjectNode) node).put(LINKING_ID_PROPERTY, linkingID);
                    updated = 1;
                }
            }
        }
        for (JsonNode child : children) {
            updated += updateLinkingIDs(child);
        }
        return updated;
    }

    /**
     * Builds the {@code <codeSystem>:<code>} value for the given {@code refuuid}.
     *
     * <p>Codes are resolved at most once per export document, because a
     * substance that is referenced from several places is still one row in the
     * database.</p>
     *
     * @param refuuid the uuid of the referenced substance
     * @return the linking identifier, or {@code null} when the code is unknown
     */
    private String toLinkingID(String refuuid) {
        if (refuuid == null) {
            return null;
        }
        String reference = refuuid.trim();
        if (reference.isEmpty()) {
            return null;
        }
        String code = codeCache.get(reference);
        if (code == null) {
            code = resolveCode(reference);
            if (code == null) {
                code = "";
            }
            codeCache.put(reference, code);
        }
        if (code.isEmpty()) {
            return null;
        }
        return codeSystem + ":" + code;
    }

    /**
     * Resolves a code, turning a failing lookup into a missing one.
     *
     * @param refuuid the uuid of the referenced substance
     * @return the code, or {@code null} when it cannot be resolved
     */
    private String resolveCode(String refuuid) {
        try {
            return codeResolver.apply(refuuid);
        } catch (Exception e) {
            log.warn("Unable to find the {} code of the substance {}: {}", codeSystem, refuuid, e.getMessage());
            return null;
        }
    }

    /**
     * Finds the code of the substance with the given uuid in the given code system.
     *
     * @param refuuid         the uuid of the referenced substance
     * @param codeSystem      the code system name
     * @param repository      the substance repository
     * @param readTransaction the read-only transaction the lookup runs in, may be {@code null}
     * @return the code, or {@code null} when the substance has no code in that system
     */
    private static String findCode(String refuuid, String codeSystem, SubstanceRepository repository,
                                   TransactionTemplate readTransaction) {
        UUID uuid;
        try {
            uuid = UUID.fromString(refuuid);
        } catch (IllegalArgumentException e) {
            // A reference that carries something other than a uuid cannot be resolved.
            return null;
        }
        if (readTransaction == null) {
            return findCode(repository.findById(uuid).orElse(null), codeSystem);
        }
        return readTransaction.execute(status ->
            findCode(repository.findById(uuid).orElse(null), codeSystem));
    }

    /**
     * Finds the code of the given substance in the given code system.
     *
     * <p>Only codes of type {@code PRIMARY} are considered, so secondary or
     * alternate codes never end up in a {@code linkingID}. Classifications are
     * excluded as well, because they carry a different meaning than an
     * identifier.</p>
     *
     * @param substance  the referenced substance, may be {@code null}
     * @param codeSystem the code system name
     * @return the code, or {@code null} when the substance has no code in that system
     */
    private static String findCode(Substance substance, String codeSystem) {
        if (substance == null || substance.getCodes() == null) {
            return null;
        }
        Optional<Code> code = substance.getCodes()
            .stream()
            .filter(c -> c != null && codeSystem.equalsIgnoreCase(c.codeSystem))
            .filter(c -> !c.isClassification())
            .filter(c -> PRIMARY_CODE_TYPE.equalsIgnoreCase(c.type))
            .filter(c -> c.code != null && !c.code.trim().isEmpty())
            .findFirst();
        return code.map(c -> c.code.trim()).orElse(null);
    }
}
