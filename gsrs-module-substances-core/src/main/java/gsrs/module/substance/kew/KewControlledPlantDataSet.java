package gsrs.module.substance.kew;

import tools.jackson.databind.JsonNode;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class KewControlledPlantDataSet implements DataSet<String>{
	private final LinkedHashSet<String> controlledList = new LinkedHashSet<>();

	private final JsonMapper mapper = JsonMapper.builderWithJackson2Defaults().build();

	public KewControlledPlantDataSet(Resource kewJson) throws  IOException {
		try(InputStream in = kewJson.getInputStream()){
			JsonNode tree = mapper.readTree(in);
			parseControlledListFrom(tree);
		}
	}
	public KewControlledPlantDataSet(File kewJson) {
		JsonNode tree = mapper.readTree(kewJson);

		parseControlledListFrom(tree);

	}

	private void parseControlledListFrom(JsonNode tree) {
		for(JsonNode jsn:tree.at("/substanceNames")){
			String unii=jsn.at("/externalIdentifier").asString();
			controlledList.add(unii);
		}
	}


	@Override
	public Iterator<String> iterator() {
		return controlledList.iterator();
	}



	@Override
	public boolean contains(String k) {
		return controlledList.contains(k);
	}

}
