package ix.core;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import gsrs.module.substance.repository.ValueRepository;
import ix.core.models.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class AbstractValueDeserializer extends ValueDeserializer<Value> {
	@Autowired
	private ValueRepository valueRepository;

	private final static JsonMapper mapper = JsonMapper.builderWithJackson2Defaults()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build();

	public static List<Class<? extends Value>> classes = new ArrayList<>();

	static {
		classes.add(Keyword.class);
		classes.add(Text.class);
		classes.add(VBin.class);
		classes.add(VInt.class);
		classes.add(VNum.class);
		classes.add(VRange.class);
		classes.add(VStr.class);
		classes.add(Mesh.class);
		classes.add(Value.class);
	}

	public Value deserialize(JsonParser parser, DeserializationContext ctx) {
		ObjectNode objectNode = parser.readValueAsTree();
		JsonNode idNode = objectNode.at("/id");
		Long l = idNode.isMissingNode()? null : idNode.longValue();
		Value v = null;

		for (Class<? extends Value> c : classes) {
			v = mapper.treeToValue(objectNode, c);
			break;
		}
		if (v == null && l != null && valueRepository !=null) {
			v = valueRepository.findById(l).orElse(null);
		}

		return v;
	}

}
