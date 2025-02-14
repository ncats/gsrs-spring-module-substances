package ix.core.util.pojopointer.extensions;

import ix.core.models.Structure;
import ix.core.util.pojopointer.LambdaArgumentParser;
import ix.core.util.pojopointer.LambdaPath;
import ix.core.util.pojopointer.extensions.InChIRegisteredFunction.InChIPath;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
@Slf4j
public class InChIRegisteredFunction implements RegisteredFunction<InChIPath, Structure, String> {
	public static String name = "$inchikey";
	public final static String MULTIPLE_VALUE_DELIMITER = ";";

	public static class InChIPath extends LambdaPath{
		@Override
		protected String thisURIPath() {
			return name + "()";
		}
	}
	
	@Override
	public Class<InChIPath> getFunctionClass() {
		return InChIPath.class;
	}
	
	@Override
	public LambdaArgumentParser<InChIPath> getFunctionURIParser() {
		return LambdaArgumentParser.NO_ARGUMENT_PARSER(name, ()->new InChIPath());
	}
	
	@Override
	public BiFunction<InChIPath, Structure, Optional<String>> getOperation() {
		log.trace("getOperation");
		return (fp, s)->{

			try{
				List<String> firstGo = s.getInChIKeysAndThrow();
				log.trace("firstGo: {}", firstGo);
				if(firstGo != null && firstGo.size() > 0){
					log.trace("firstGot produced data");
					return Optional.of(String.join(MULTIPLE_VALUE_DELIMITER, firstGo));
				}
				log.trace("firstGot produced NO data");
				return Optional.ofNullable(s.getInChIKey());
			}catch(Exception e){
                log.error("error computing inchi key of structure ID " + s.id, e);
				throw new RuntimeException("error computing inchi key of structure ID " + s.id, e);
			}
		};
	}
}
