package gsrs.dataexchange.extractors;


import com.fasterxml.jackson.databind.JsonNode;
import gsrs.GsrsFactoryConfiguration;
import gsrs.stagingarea.model.MatchableKeyValueTupleExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class ExplicitMatchableExtractorFactory implements MatchableExtractorFactory {

    @Autowired
    private GsrsFactoryConfiguration gsrsFactoryConfiguration;

    final String substancesContext = "substances";
    @Override
    public <T> MatchableKeyValueTupleExtractor<T> createExtractorFor(Class<T> cls) {
        log.trace("starting createExtractorFor");
        final MatchableKeyValueTupleExtractor<T>[] returnExtractor = new MatchableKeyValueTupleExtractor[]{(t, c) -> {
        }};
        assert gsrsFactoryConfiguration!=null;
        gsrsFactoryConfiguration.getMatchableCalculationConfig(substancesContext).forEach(c->{
            try {
                log.trace("adding extractor of class {}", c.getMatchableCalculationClass().getName());
                MatchableKeyValueTupleExtractor extractor= (MatchableKeyValueTupleExtractor) c.getMatchableCalculationClass().getConstructor(JsonNode.class).newInstance(c.getConfig());
                returnExtractor[0] = returnExtractor[0].combine(extractor);
            } catch (Exception e) {
                log.error("Error instantiating matchable calculator ", e);
            }
        });
        return returnExtractor[0];
    }
}
