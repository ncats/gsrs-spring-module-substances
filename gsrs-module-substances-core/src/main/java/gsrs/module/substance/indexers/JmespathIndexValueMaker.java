package gsrs.module.substance.indexers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;

import gsrs.module.substance.utils.SplitFunction;
import gsrs.module.substance.utils.UniqueFunction;

import io.burt.jmespath.JmesPath;
import io.burt.jmespath.Expression;
import io.burt.jmespath.RuntimeConfiguration;
import io.burt.jmespath.function.FunctionRegistry;
import io.burt.jmespath.jackson.JacksonRuntime;

import ix.core.controllers.EntityFactory;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.Substance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Egor Puzanov on 9/10/2021.
 */
@Slf4j
public class JmespathIndexValueMaker implements IndexValueMaker<Substance> {

    private List<IndexExpression> expressions = new ArrayList<IndexExpression>();
    private final ObjectWriter writer = EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().writer();

    private class IndexExpression {
        private final String index;
        private final String type;
        private final List<String> ranges;
        private final Expression<JsonNode> expression;
        private final Pattern regex;
        private final String replacement;
        private final String format;
        private final boolean suggestable;
        private final boolean sortable;

        public IndexExpression(Map<String, String> m) {
            FunctionRegistry customFunctions = FunctionRegistry.defaultRegistry().extend(
                                                           new SplitFunction(),
                                                           new UniqueFunction());
            RuntimeConfiguration configuration = new RuntimeConfiguration.Builder()
                                       .withFunctionRegistry(customFunctions)
                                       .build();
            JmesPath<JsonNode> jmespath = new JacksonRuntime(configuration);
            this.type = m.getOrDefault("type", "String");
            this.ranges = Arrays.asList(m.getOrDefault("ranges", "").split(" "));
            this.expression = (Expression<JsonNode>) jmespath.compile(m.get("expression"));
            this.regex = Pattern.compile(m.getOrDefault("regex", ""));
            this.replacement = m.getOrDefault("replacement", "$1");
            this.format = m.get("format");
            this.suggestable = Boolean.valueOf(m.getOrDefault("suggestable", "false")).booleanValue();
            this.sortable = Boolean.valueOf(m.getOrDefault("sortable", "false")).booleanValue();
            this.index = m.get("index");
        }

        public boolean isValid() {
            if (index == null || expression == null) {
                return false;
            }
            return true;
        }

        public void createIndexableValues(JsonNode tree, Consumer<IndexableValue> consumer) {
            IndexableValue iv = null;
            try {
                JsonNode results = expression.search(tree);
                log.debug("Results: " + results.toString());
                if (!results.isArray()) {
                    results = (JsonNode) new ObjectMapper().createArrayNode().add(results);
                }
                for(JsonNode result: (ArrayNode)results){
                    if (result.isValueNode() && ! result.isNull()) {
                        if (result.isDouble() || "Double".equals(type)) {
                            if (ranges != null && !ranges.isEmpty()) {
                                log.debug("Index: " + index + " FacetDoubleValue: " + result.asText());
                                iv = IndexableValue.simpleFacetDoubleValue(index, result.asDouble(),
                                    ranges.stream().mapToDouble(Double::valueOf).toArray());
                            } else {
                                log.debug("Index: " + index + " DoubleValue: " + result.asText());
                                iv = IndexableValue.simpleDoubleValue(index, result.asDouble());
                            }
                        } else if (result.isNumber() || "Long".equals(type)) {
                            if (ranges != null && !ranges.isEmpty()) {
                                log.debug("Index: " + index + " FacetLongValue: " + result.asText());
                                iv = IndexableValue.simpleFacetLongValue(index, result.asLong(),
                                    ranges.stream().mapToLong(Long::valueOf).toArray());
                            } else {
                                log.debug("Index: " + index + " LongValue: " + result.asText());
                                iv = IndexableValue.simpleLongValue(index, result.asLong());
                            }
                        } else {
                            String value = result.asText(null);
                            if (!regex.pattern().isEmpty()) {
                                log.debug("Index: " + index + " Value before regex: " + value);
                                value = regex.matcher(value).replaceAll(replacement);
                                log.debug("Index: " + index + " Value after regex: " + value);
                            }
                            if (value != null && !value.isEmpty()) {
                                if (index.startsWith("root_")) {
                                    log.debug("Index: " + index + " StringValue: " + value);
                                    iv = IndexableValue.simpleStringValue(index, value);
                                } else {
                                    log.debug("Index: " + index + " FacetStringValue: " + value);
                                    iv = IndexableValue.simpleFacetStringValue(index, value);
                                }
                            }
                        }
                        if (iv != null) {
                            if (format != null && !format.isEmpty()) {
                                iv = iv.setFormat(format);
                            }
                            if (suggestable) {
                                iv = iv.suggestable();
                            }
                            if (sortable) {
                                iv = iv.setSortable();
                            }
                            consumer.accept(iv);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }

    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode tree = mapper.readTree(writer.writeValueAsString(substance));
            updateReferences(tree);
            for (IndexExpression expression: expressions) {
                expression.createIndexableValues(tree, consumer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setExpressions(LinkedHashMap<Integer, Map<String, String>> expressions) {
        this.expressions.clear();
        for (Map<String, String> expression: expressions.values()) {
            IndexExpression expr = new IndexExpression(expression);
            if (expr.isValid()) {
                this.expressions.add(expr);
            }
        }
    }

    private void updateReferences(JsonNode tree) {
        ArrayNode references = (ArrayNode)tree.at("/references");
        Map<String, Integer> refMap = new HashMap<>();
        for (int i = 0; i < references.size(); i++) {
            refMap.put(references.get(i).get("uuid").textValue(), i);
        }
        for (JsonNode refsNode: tree.findValues("references")) {
            if (refsNode.isArray()) {
                ArrayNode refs = (ArrayNode) refsNode;
                for (int i = 0; i < refs.size(); i++) {
                    JsonNode ref = refs.get(i);
                    if (ref.isTextual()) {
                        Integer index = refMap.get(ref.asText());
                        if(index !=null) {
                            refs.set(i, references.get(index));
                        }
                    }
                }
            }
        }
    }
}
