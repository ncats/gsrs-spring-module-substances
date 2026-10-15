package gsrs.module.substance.processors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.Code;
import java.util.HashMap;
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

	/**
	 * Builds the processor from this entity processor's "parameters" block.  The block has exactly
	 * two attributes:
	 *
	 * <ul>
	 *   <li>{@code class} - the name of the {@link CodeSystemUrlGenerator} implementation to use.</li>
	 *   <li>{@code codeSystems} - the code system data handed to that implementation.  It is a
	 *     map of code system name to URL template, for example
	 *     {@code "MERCK INDEX" -> "https://merckindex.rsc.org/monographs/$CODE$"}.</li>
	 * </ul>
	 *
	 * The data no longer comes from an external configuration file; it is declared directly in the
	 * HOCON configuration.  For example:
	 *
	 * <pre>
	 * gsrs.entityProcessors.list.CodeProcessor = {
	 *   "entityClassName" : "ix.ginas.models.v1.Code",
	 *   "processor" : "gsrs.module.substance.processors.CodeProcessor",
	 *   "order" : 1800,
	 *   "parameters" : {
	 *     "class" : "gsrs.module.substance.datasource.DefaultCodeSystemUrlGenerator",
	 *     "codeSystems" : {
	 *       "XYZ" : "http://some.url.com/$CODE$"
	 *     }
	 *   }
	 * }
	 * </pre>
	 *
	 * The delegating {@link JsonCreator} is required because entity processors declared with
	 * "parameters" are built by Jackson ({@code ObjectMapper.convertValue}) rather than by a direct
	 * constructor call.  A delegating creator hands the whole parameter map to this constructor and
	 * bypasses Jackson's unknown-property handling.
	 */
	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public CodeProcessor(Map with){
        log.debug("starting in CodeProcessor constructor");
		try{
			String key = "class";
			String classname = (String)with.get(key);
            log.trace("classname: " + classname);
			if(classname ==null){
				log.error("error loading class from config =\n" + with);
				throw new IllegalStateException("could not find " + key + " in codesystem initialization file");
			}
			Class<?> cls = CodeProcessor.class.getClassLoader().loadClass(classname);
            log.trace("loaded class named " + classname + ": " + cls);
			if(with.get("codeSystems")!=null){
				// The generator takes the code system data under a single "codeSystems" property, so
				// wrap it to keep the "class" attribute out of the deserialization.
				Map<String, Object> generatorParameters = new HashMap<>();
				generatorParameters.put("codeSystems", with.get("codeSystems"));
				ObjectMapper mapper = new ObjectMapper();
        		codeSystemData = (CodeSystemUrlGenerator) mapper.convertValue(generatorParameters, cls);
			}
		}catch(Exception e){
            log.error("Error in CodeProcessor constructor: ", e);
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
