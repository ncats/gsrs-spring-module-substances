package gsrs.module.substance.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.Code;
import java.util.Map;

import java.util.Optional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Data
public class CodeProcessor implements EntityProcessor<Code> {

	public CodeSystemUrlGenerator codeSystemData;
	
	public CodeProcessor(Map with){
		try{
			// It used to be that the codeSystem.json file was found by
			// looking for the root config JSON found under ix.codeSystemUrlGenerator
			// Which had both a "class" specified (for the kind of CodeSystemUrlGenerator
			// to use) and a "json" property which contained the raw JSON to deserialize
			// the CodeSystemUrlGenerator with. This gave some flexibility (perhaps too much flexibility)
			// to how a CodeSystemUrlGenerator could be made. In practice it was always instantiating
			// a DefaultCodeSystemUrlGenerator which really just used a filename.
			
			//String key = "ix.codeSystemUrlGenerator.class";
			//String classname = Play.application().configuration().getString(key);
			//if(classname ==null){
			//	System.out.println("config =\n" + Play.application().configuration().asMap());
			//	throw new IllegalStateException("could not find " + key + " in config file");
			//}
			//Class<?> cls = IOUtil.getGinasClassLoader().loadClass(classname);
			//codeSystemData = (CodeSystemUrlGenerator) ConfigHelper.readFromJson("ix.codeSystemUrlGenerator.json", cls);

			//Now, instead, just read these properties from the "with" part of then entity processor initializer.
			//It may still be a bit too abstract for practical purposes, but it's not too bad
			
			String key = "class";
			String classname = (String)with.get(key);
			if(classname ==null){
				System.out.println("config =\n" + with);
				throw new IllegalStateException("could not find " + key + " in codesystem initialization file");
			}
			Class<?> cls = CodeProcessor.class.getClassLoader().loadClass(classname);
			if(with.get("json")!=null){
				Object jsonObj=with.get("json");
				ObjectMapper mapper = new ObjectMapper();
				//convert from the 
        			codeSystemData = (CodeSystemUrlGenerator) mapper.convertValue(jsonObj, cls);
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	
	public void generateURL(Code code){
		if(codeSystemData==null){
			return;
		}
		if(code.url==null || code.url.trim().isEmpty()){
			Optional<String> csm=codeSystemData.generateUrlFor(code);
			if(csm.isPresent()){
				code.url = csm.get();
			}
		}
	}
	
	@Override
	public void prePersist(Code obj) throws ix.core.EntityProcessor.FailProcessingException {
		generateURL(obj);
	}

	@Override
	public void preUpdate(Code obj) throws ix.core.EntityProcessor.FailProcessingException {
		generateURL(obj);
	}

    @Override
    public Class<Code> getEntityClass() {
        return Code.class;
    }

}
