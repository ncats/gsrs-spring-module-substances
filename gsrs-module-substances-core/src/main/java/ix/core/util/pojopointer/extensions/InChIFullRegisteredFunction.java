package ix.core.util.pojopointer.extensions;

import ix.core.models.Structure;
import ix.core.util.pojopointer.LambdaArgumentParser;
import ix.core.util.pojopointer.LambdaPath;
import ix.core.util.pojopointer.extensions.InChIFullRegisteredFunction.InChIFullPath;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.BiFunction;
@Slf4j
public class InChIFullRegisteredFunction implements RegisteredFunction<InChIFullPath, Structure, String> {
    public static String name = "$inchi";

    public static class InChIFullPath extends LambdaPath{
        @Override
        protected String thisURIPath() {
            return name + "()";
        }
    }

    @Override
    public Class<InChIFullPath> getFunctionClass() {
        return InChIFullPath.class;
    }

    @Override
    public LambdaArgumentParser<InChIFullPath> getFunctionURIParser() {
        return LambdaArgumentParser.NO_ARGUMENT_PARSER(name, ()->new InChIFullPath());
    }

    @Override
    public BiFunction<InChIFullPath, Structure, Optional<String>> getOperation() {
        return (fp, s)->{
            try{
				return Optional.ofNullable(s.getInChIAndThrow());
            }catch(Exception e){
                log.error("error computing inchi of structure ID " + s.id, e);
				throw new RuntimeException("error computing inchi key of structure ID " + s.id, e);
            }
        };
    }
}
