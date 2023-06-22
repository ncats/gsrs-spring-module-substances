package gsrs.dataexchange.extractors;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.GsrsFactoryConfiguration;
import gsrs.stagingarea.model.MatchableKeyValueTupleExtractor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;

@Slf4j
public class ExplicitMatchableExtractorFactory implements MatchableExtractorFactory {

    final String substancesContext = "substances";

    public GsrsFactoryConfiguration getGsrsFactoryConfiguration() {
        return gsrsFactoryConfiguration;
    }

    public void setGsrsFactoryConfiguration(GsrsFactoryConfiguration gsrsFactoryConfiguration) {
        this.gsrsFactoryConfiguration = gsrsFactoryConfiguration;
    }

    private GsrsFactoryConfiguration gsrsFactoryConfiguration;

    private CachedSupplier<MatchableKeyValueTupleExtractor> matchableExtractors =CachedSupplier.of(()->{
        log.trace("starting createExtractorFor");
        final MatchableKeyValueTupleExtractor<T>[] returnExtractor = new MatchableKeyValueTupleExtractor[]{(t, c) -> {
        }};
        assert gsrsFactoryConfiguration!=null;
        gsrsFactoryConfiguration.getMatchableCalculationConfig(substancesContext).forEach(c->{
            try {
                log.trace("adding extractor of class {}", c.getMatchableCalculationClass().getName());
                MatchableKeyValueTupleExtractor extractor;
                if( hasConstructorWithConfigParm( c.getMatchableCalculationClass())) {
                    //We have a constructor that accepts a config object
                    extractor = (MatchableKeyValueTupleExtractor) c.getMatchableCalculationClass().getConstructor(JsonNode.class).newInstance(c.getConfig());
                } else{
                    //use default constructor
                    extractor = (MatchableKeyValueTupleExtractor) c.getMatchableCalculationClass().getConstructor().newInstance();
                }
                returnExtractor[0] = returnExtractor[0].combine(extractor);
            } catch (Exception e) {
                log.error("Error instantiating matchable calculator ", e);
            }
        });
        return returnExtractor[0];
    });

    @Override
    public <T> MatchableKeyValueTupleExtractor<T> createExtractorFor(Class<T> cls) {
        log.trace("starting createExtractorFor");
        return matchableExtractors.get();
    }

    private boolean hasConstructorWithConfigParm(Class classToTest) {
        for(Constructor constructor : classToTest.getConstructors()) {
            if(constructor.getParameterTypes().length==1&& constructor.getParameterTypes()[0].equals(JsonNode.class) ){
                return true;
            }
        }
        return false;
    }

}
