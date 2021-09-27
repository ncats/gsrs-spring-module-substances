package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.AutowireHelper;
import ix.ncats.resolvers.Resolver;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class StructureResolverService {
    

    @Autowired
    private StructureResolverServiceConfiguration structureResolverServiceConfiguration;

    private CachedSupplier<List<Resolver>> resolvers = CachedSupplier.of(
            ()-> {
                ObjectMapper mapper = new ObjectMapper();
                return structureResolverServiceConfiguration.getImplementations()
                        .stream().map(c -> {
                            Resolver resolver = null;
                            try {

                                if (c.getParameters() == null) {
                                    resolver= (Resolver) Class.forName(c.getResolverClass()).newInstance();
                                }else {
                                    resolver = (Resolver) mapper.convertValue(c.getParameters(), Class.forName(c.getResolverClass()));
                                }
                            } catch (Throwable t) {
                                t.printStackTrace();
                                return null;
                            }
                            if(resolver !=null){
                                resolver = AutowireHelper.getInstance().autowireAndProxy(resolver);
                            }
                            return resolver;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
    );
    public List<ResolverResult> resolve(String name){
        return resolvers.get().stream().map(r -> {
                   Object value = r.resolve(name);
                   if(value !=null) {
                       return new ResolverResult(r.getName(), value.getClass(), value);
                   }
                   return null;
                })
        .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResolverResult{
        private String source;
        private Class kind;
        private Object value;
    }

}
