package gsrs.module.substance.utils;

import com.fasterxml.jackson.databind.JsonNode;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XsltTransformer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility methods for passing additional parameters to XSL templates.
 *
 * <p>Parameters are configured in the GSRS configuration file as a simple
 * name/value map, for example:</p>
 *
 * <pre>
 * "xslParameters": {
 *     "someFlag": "true",
 *     "someNamespacePrefix:someName": "value",
 *     "{http://example.com/ns}someName": "value"
 * }
 * </pre>
 *
 * <p>Parameter names may be given as a plain local name ({@code name}) or in
 * Clark/EQName notation ({@code {uri}name} or {@code Q{uri}name}). Prefixed
 * names ({@code prefix:name}) are also supported when the prefix is present in
 * the supplied namespace map. All values are passed to the template as
 * {@code xs:string} values.</p>
 *
 * @author Egor Puzanov
 */
public final class XslParameterUtils {

    /**
     * Name of the configuration property holding the additional template parameters.
     */
    public static final String PARAMETERS_PROPERTY = "xslParameters";

    /**
     * Optional property holding a mapping of namespace prefixes to namespace URIs,
     * used to resolve prefixed parameter names.
     */
    public static final String NAMESPACES_PROPERTY = "xslNamespaces";

    private XslParameterUtils() {
    }

    /**
     * Sets the given parameters on the supplied transformer.
     *
     * <p>Entries with a blank name or a {@code null} value are skipped.
     * {@code null} maps are ignored.</p>
     *
     * @param transformer the transformer to configure, may be {@code null}
     * @param parameters  the parameters to set, may be {@code null}
     */
    public static void applyParameters(XsltTransformer transformer, Map<String, String> parameters) {
        applyParameters(transformer, parameters, null);
    }

    /**
     * Sets the given parameters on the supplied transformer, resolving prefixed
     * parameter names against the given namespace map.
     *
     * @param transformer the transformer to configure, may be {@code null}
     * @param parameters  the parameters to set, may be {@code null}
     * @param namespaces  optional mapping of prefixes to namespace URIs, may be {@code null}
     */
    public static void applyParameters(XsltTransformer transformer, Map<String, String> parameters,
                                       Map<String, String> namespaces) {
        if (transformer == null || parameters == null || parameters.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String name = entry.getKey();
            if (name == null || name.trim().isEmpty() || entry.getValue() == null) {
                continue;
            }
            transformer.setParameter(toQName(name, namespaces),
                XdmAtomicValue.makeAtomicValue(entry.getValue()));
        }
    }

    /**
     * Converts a configured parameter name into a {@link QName}.
     *
     * @param name the configured parameter name
     * @return the corresponding QName
     * @throws IllegalArgumentException if the name uses an undeclared prefix
     */
    public static QName toQName(String name) {
        return toQName(name, null);
    }

    /**
     * Converts a configured parameter name into a {@link QName}.
     *
     * <p>Clark ({@code {uri}local}) and EQName ({@code Q{uri}local}) notations are
     * supported in addition to plain and prefixed names. Prefixed names are
     * resolved against the given namespace map.</p>
     *
     * @param name       the configured parameter name
     * @param namespaces optional mapping of prefixes to namespace URIs, may be {@code null}
     * @return the corresponding QName
     * @throws IllegalArgumentException if the name uses an undeclared prefix
     */
    public static QName toQName(String name, Map<String, String> namespaces) {
        String trimmed = name.trim();
        if (trimmed.startsWith("Q{")) {
            return QName.fromEQName(trimmed);
        }
        if (trimmed.startsWith("{")) {
            return QName.fromClarkName(trimmed);
        }
        int colon = trimmed.indexOf(':');
        if (colon < 0) {
            return new QName(trimmed);
        }
        String prefix = trimmed.substring(0, colon);
        String localName = trimmed.substring(colon + 1);
        if (prefix.isEmpty() || localName.isEmpty()) {
            throw new IllegalArgumentException("Invalid XSL parameter name: " + name);
        }
        String uri = namespaces == null ? null : namespaces.get(prefix);
        if (uri == null || uri.isEmpty()) {
            throw new IllegalArgumentException("Undeclared namespace prefix '" + prefix
                + "' in XSL parameter name '" + name
                + "'. Declare it in " + NAMESPACES_PROPERTY
                + " or use the {uri}name notation.");
        }
        return new QName(uri, localName);
    }

    /**
     * Creates a mutable parameter map from the given configuration node.
     *
     * @param node the {@code xslParameters} configuration node, may be {@code null}
     * @return a new, mutable map; never {@code null}
     */
    public static Map<String, String> fromJson(JsonNode node) {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return parameters;
        }
        node.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value != null && !value.isNull()) {
                parameters.put(entry.getKey(), value.isValueNode() ? value.asText() : value.toString());
            }
        });
        return parameters;
    }

    /**
     * Reads the additional template parameters from the given configuration node.
     *
     * @param node      the parent configuration node, may be {@code null}
     * @param key       the name of the property holding the parameters
     * @return a new, mutable map; never {@code null}
     */
    public static Map<String, String> fromJson(JsonNode node, String key) {
        if (node == null || !node.isObject()) {
            return new LinkedHashMap<>();
        }
        return fromJson(node.get(key));
    }

    /**
     * Reads the additional template parameters from the given configuration node.
     *
     * <p>The parameters are looked up under the {@code parameters} property first
     * and, if that property is missing, directly on the given node. This allows
     * the same helper to be used both with a full adapter/exporter settings node
     * and with a node that already contains only the parameters.</p>
     *
     * @param settings the configuration node, may be {@code null}
     * @return a new, mutable map; never {@code null}
     */
    public static Map<String, String> fromSettings(JsonNode settings) {
        if (settings == null || !settings.isObject()) {
            return new LinkedHashMap<>();
        }
        JsonNode nested = settings.get("parameters");
        if (nested != null && nested.isObject()) {
            Map<String, String> parameters = fromJson(nested, PARAMETERS_PROPERTY);
            if (!parameters.isEmpty()) {
                return parameters;
            }
        }
        return fromJson(settings, PARAMETERS_PROPERTY);
    }

    /**
     * Reads the namespace prefix mappings used to resolve prefixed parameter names.
     *
     * @param node the configuration node, may be {@code null}
     * @return a new, mutable map; never {@code null}
     */
    public static Map<String, String> namespacesFromJson(JsonNode node) {
        return fromJson(node, NAMESPACES_PROPERTY);
    }

    /**
     * Reads the namespace prefix mappings from a full settings node, looking them
     * up under {@code parameters} first and then directly on the node.
     *
     * @param settings the configuration node, may be {@code null}
     * @return a new, mutable map; never {@code null}
     */
    public static Map<String, String> namespacesFromSettings(JsonNode settings) {
        if (settings == null || !settings.isObject()) {
            return new LinkedHashMap<>();
        }
        JsonNode nested = settings.get("parameters");
        if (nested != null && nested.isObject()) {
            Map<String, String> namespaces = namespacesFromJson(nested);
            if (!namespaces.isEmpty()) {
                return namespaces;
            }
        }
        return namespacesFromJson(settings);
    }

    /**
     * Merges two parameter maps, the second one overriding values from the first one.
     *
     * @param base      the base parameters, may be {@code null}
     * @param overrides the overriding parameters, may be {@code null}
     * @return a new, unmodifiable merged map
     */
    public static Map<String, String> merge(Map<String, String> base, Map<String, String> overrides) {
        if ((base == null || base.isEmpty()) && (overrides == null || overrides.isEmpty())) {
            return Collections.emptyMap();
        }
        Map<String, String> merged = new LinkedHashMap<>();
        if (base != null) {
            merged.putAll(base);
        }
        if (overrides != null) {
            merged.putAll(overrides);
        }
        return Collections.unmodifiableMap(merged);
    }
}
