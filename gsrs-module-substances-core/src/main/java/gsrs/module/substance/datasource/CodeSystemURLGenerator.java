package gsrs.module.substance.datasource;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;


public class CodeSystemURLGenerator implements DataSet<CodeSystemMeta>{
	
	
	private final Map<String,CodeSystemMeta> controlledList = new LinkedHashMap<>();
	
	public CodeSystemURLGenerator(Map with) throws JsonProcessingException, IOException{
        
		//InputStream is=new ClassPathResource(filename).getInputStream();
		//ObjectMapper mapper = new ObjectMapper();
        Map<String, String> codeMaps = (Map<String, String> ) with.get("codeSystems");
        codeMaps.keySet().stream().map((codeSystem) -> {
            String url = codeMaps.get(codeSystem);
            CodeSystemMeta csmap=new CodeSystemMeta(codeSystem,url);
            return csmap;
        }).forEachOrdered((csmap) -> {
            controlledList.put(csmap.codeSystem, csmap);
        });

//		JsonNode tree = mapper.readTree(is);
//		
//		for(JsonNode jsn:tree){
//			String cs=jsn.at("/codeSystem").asText();
//			String url=jsn.at("/url").asText();
//			CodeSystemMeta csmap=new CodeSystemMeta(cs,url);
//			controlledList.put(csmap.codeSystem, csmap);
//		}
//		is.close();
	}
	
	//@Override
	public Iterator<CodeSystemMeta> iterator() {
        return controlledList.values().iterator();
	}

	//@Override
	public boolean contains(CodeSystemMeta k) {
		return controlledList.containsKey(k.codeSystem);
	}
	
	public CodeSystemMeta fetch(String cs){
		return this.controlledList.get(cs);
	}
	
	

}
