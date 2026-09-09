package ix.ginas.utils.validation.validators;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

import gsrs.module.substance.utils.HtmlUtil;
import gsrs.validator.ValidatorConfig;
import gsrs.module.substance.utils.Jackson3Runtime;
import gsrs.module.substance.utils.SplitFunction;
import gsrs.module.substance.utils.UniqueFunction;

import io.burt.jmespath.JmesPath;
import io.burt.jmespath.Expression;
import io.burt.jmespath.RuntimeConfiguration;
import io.burt.jmespath.function.FunctionRegistry;

import ix.core.controllers.EntityFactory;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import lombok.extern.slf4j.Slf4j;


/**
 * Created by epuzanov on 2/26/24.
 */
@Slf4j
public class JmespathValidator extends AbstractValidatorPlugin<Substance>{

    private List<ValidatorExpression> expressions = new ArrayList<>();
    private final EntityFactory.EntityMapper.EntityWriter writer = EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().writer();

    private final static String CHARSET = "UTF-8";

    private final static JsonMapper mapper = JsonMapper.builderWithJackson2Defaults().build();

    private class ValidatorExpression {
        private final GinasProcessingMessage.MESSAGE_TYPE messageType;
        private final String messageId;
        private final String messageTemplate;
        private final String rawExpression;
        private final Expression<JsonNode> expression;

        public ValidatorExpression(Map<String, String> m) {
            FunctionRegistry customFunctions = FunctionRegistry.defaultRegistry().extend(
                    new SplitFunction(),
                    new UniqueFunction());
            RuntimeConfiguration configuration = new RuntimeConfiguration.Builder()
                    .withFunctionRegistry(customFunctions)
                    .build();
            JmesPath<JsonNode> jmespath = new Jackson3Runtime(configuration);
            this.messageType = GinasProcessingMessage.MESSAGE_TYPE.valueOf(m.getOrDefault("messageType", "NOTICE"));
            this.messageTemplate = m.get("messageTemplate");
            this.messageId = m.getOrDefault("messageId", this.messageType.toString().substring(0,1)
                    + String.valueOf("JmespathValidator".hashCode()).substring(2,5)
                    + String.valueOf(this.messageTemplate.hashCode()).substring(1,5));
            this.rawExpression = m.get("expression");
            this.expression = jmespath.compile(this.rawExpression);
        }

        public boolean isValid() {
            if (messageTemplate == null || expression == null) {
                return false;
            }
            return true;
        }

        public void validate(JsonNode tree, ValidatorCallback callback) {
            log.debug("Validation Expression: " + rawExpression);
            JsonNode results = expression.search(tree);
            log.debug("Validation Results: " + results.toString());
            if (results != null && !results.isNull() && !(results.isArray() && results.size() < 1) && !(results.isBoolean() && !results.asBoolean())) {
                if (!results.isArray()) {
                    results = mapper.createArrayNode().add(results);
                }
                Object[] args = StreamSupport.stream(results.spliterator(), false)
                        .map(JsonNode::asString)
                        .map(s->HtmlUtil.clean(s, CHARSET))
                        .toArray(Object[]::new);
                String msg = String.format(messageTemplate, args);
                log.debug("Validation Message: " + messageId + " " + msg);
                callback.addMessage(new GinasProcessingMessage(messageType, msg, messageId));
            }
        }
    }

    @Override
    public boolean supports(Substance newValue, Substance oldValue, ValidatorConfig.METHOD_TYPE methodType) {
        return methodType != ValidatorConfig.METHOD_TYPE.BATCH;
    }

    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {
        try {
            JsonNode tree = mapper.readTree("{\"new\":" + writer.writeValueAsString(objnew) + ",\"old\":" + writer.writeValueAsString(objold) + "}");
            log.debug("Validation Tree: " + tree.toString());
            updateReferences(tree.get("new"));
            updateReferences(tree.get("old"));
            for (ValidatorExpression expression: expressions) {
                expression.validate(tree, callback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setExpressions(LinkedHashMap<Integer, Map<String, String>> expressions) {
        this.expressions.clear();
        for (Map<String, String> expression: expressions.values()) {
            ValidatorExpression expr = new ValidatorExpression(expression);
            if (expr.isValid()) {
                this.expressions.add(expr);
            }
        }
    }

    private void updateReferences(JsonNode tree) {
        if (tree == null || tree.isNull()) return;
        ArrayNode references = (ArrayNode)tree.at("/references");
        Map<String, Integer> refMap = new HashMap<>();
        for (int i = 0; i < references.size(); i++) {
            refMap.put(references.get(i).get("uuid").stringValue(), i);
        }
        for (JsonNode refsNode: tree.findValues("references")) {
            if (refsNode.isArray()) {
                ArrayNode refs = (ArrayNode) refsNode;
                for (int i = 0; i < refs.size(); i++) {
                    JsonNode ref = refs.get(i);
                    if (ref.isString()) {
                        Integer index = refMap.get(ref.asString());
                        if(index !=null) {
                            refs.set(i, references.get(index));
                        }
                    }
                }
            }
        }
    }
}