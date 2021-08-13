package gsrs.module.substance.processors;

import ix.core.EntityProcessor;
import ix.core.controllers.EntityFactory;
//import ix.core.util.ConfigHelper;
import ix.core.util.EntityUtils;
//import ix.core.util.IOUtil;
import ix.ginas.models.v1.Code;
import java.util.Map;

import java.util.Optional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Data
public class CodeProcessor implements EntityProcessor<Code> {

	public CodeSystemUrlGenerator codeSystemData;

    @Value("${ix.codeSystemUrlGenerator.class}")
    private String className;

    @Value("${ix.codeSystemUrlGenerator.json}")
    private String json;
    
    @Autowired
    private ApplicationContext applicationContext; 
    
    private String key = "class";
	public CodeProcessor(Map with){
		try{
            
            if( className==null && with !=null && with.get(key) !=null) {
                className= (String) with.get(key);
            }
			if(className ==null){
				log.error("no value for class");
				throw new IllegalStateException("could not find " + key + " in config file");
			}
			Class<?> cls = applicationContext.getClassLoader().loadClass(className);
            EntityUtils utils = new EntityUtils();
            codeSystemData = (CodeSystemUrlGenerator) EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().convertValue(json, cls);
            
			//codeSystemData = (CodeSystemUrlGenerator) ConfigHelper.readFromJson("ix.codeSystemUrlGenerator.json", cls);

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
	public void postPersist(Code obj) throws ix.core.EntityProcessor.FailProcessingException {
		// TODO Auto-generated method stub

	}

	@Override
	public void preRemove(Code obj) throws ix.core.EntityProcessor.FailProcessingException {
		// TODO Auto-generated method stub

	}

	@Override
	public void postRemove(Code obj) throws ix.core.EntityProcessor.FailProcessingException {
		// TODO Auto-generated method stub

	}

	@Override
	public void preUpdate(Code obj) throws ix.core.EntityProcessor.FailProcessingException {
		generateURL(obj);
	}

	@Override
	public void postUpdate(Code obj) throws ix.core.EntityProcessor.FailProcessingException {
		// TODO Auto-generated method stub

	}

	@Override
	public void postLoad(Code obj) throws ix.core.EntityProcessor.FailProcessingException {
		// TODO Auto-generated method stub
		
	}

    @Override
    public Class<Code> getEntityClass() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
