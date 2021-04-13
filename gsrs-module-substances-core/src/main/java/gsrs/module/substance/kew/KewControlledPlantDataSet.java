package gsrs.module.substance.kew;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class KewControlledPlantDataSet implements DataSet<String>{
	private LinkedHashSet<String> controlledList = new LinkedHashSet<String>();

	public KewControlledPlantDataSet(Resource kewJson) throws  IOException {
		ObjectMapper mapper = new ObjectMapper();

		try(InputStream in = kewJson.getInputStream()){
			JsonNode tree = mapper.readTree(in);
			parseControlledListFrom(tree);
		}
	}
	public KewControlledPlantDataSet(File kewJson) throws  IOException{

		ObjectMapper mapper = new ObjectMapper();
		JsonNode tree = mapper.readTree(kewJson);

		parseControlledListFrom(tree);

	}

	private void parseControlledListFrom(JsonNode tree) {
		for(JsonNode jsn:tree.at("/substanceNames")){
			String unii=jsn.at("/externalIdentifier").asText();
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
