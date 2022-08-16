package gsrs.dataexchange.extractors;

import gsrs.holdingarea.model.MatchableKeyValueTuple;
import gsrs.holdingarea.model.MatchableKeyValueTupleExtractor;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.definitional.DefinitionalElement;
import gsrs.module.substance.services.ConfigBasedDefinitionalElementFactory;
import gsrs.module.substance.services.DefinitionalElementFactory;
import gsrs.module.substance.services.DefinitionalElementImplementation;
import gsrs.springUtils.AutowireHelper;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.DefHashCalcRequirements;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class DefinitionalHashMatchableExtractor implements MatchableKeyValueTupleExtractor<Substance> {
    private final String DEFINITIONAL_HASH_KEY = "Definitional Hash";

    @Autowired
    private DefinitionalElementFactory definitionalElementFactory;

    @Autowired
    private SubstanceLegacySearchService substanceSearchService;

    @Autowired
    public PlatformTransactionManager transactionManager;

    private DefinitionalElementImplementation definitionalElementImplementation;
    @Override
    public void extract(Substance substance, Consumer<MatchableKeyValueTuple> c) {
        ConfigBasedDefinitionalElementFactory configBasedDefinitionalElementFactory = new ConfigBasedDefinitionalElementFactory();
        configBasedDefinitionalElementFactory= AutowireHelper.getInstance().autowireAndProxy(configBasedDefinitionalElementFactory);
        /*
        wrapping the call to DefaultHoldingAreaService.getDefinitionalHash in a try/catch prevents an Exception from interrupting
        a daisy-chain of extractors called from a unit test
         */
        try {
            List<DefinitionalElement> definitionalElements = new ArrayList<>();
            configBasedDefinitionalElementFactory.addDefinitionalElementsFor(substance, definitionalElements::add);
            //definitionalElementImplementation.computeDefinitionalElements(substance, definitionalElements::add);
            //List<String> defHashLayers = definitionalElementImplementation. .getDefinitionalHash(substance, defHashCalcRequirements);
            for (int i = 0; i < definitionalElements.size(); i++) {
                MatchableKeyValueTuple tuple =
                        MatchableKeyValueTuple.builder()
                                .key(DEFINITIONAL_HASH_KEY)
                                .value(definitionalElements.get(i).getDefinitionalString())
                                .layer(i + 1)
                                .build();
                c.accept(tuple);

            }

        } catch (NullPointerException ex) {
            log.error("error running def hash calculation in extractor", ex);
        }
    }

    public DefinitionalElementFactory getDefinitionalElementFactory() {
        return definitionalElementFactory;
    }

    public void setDefinitionalElementFactory(DefinitionalElementFactory definitionalElementFactory) {
        this.definitionalElementFactory = definitionalElementFactory;
    }

    public SubstanceLegacySearchService getSubstanceSearchService() {
        return substanceSearchService;
    }

    public void setSubstanceSearchService(SubstanceLegacySearchService substanceSearchService) {
        this.substanceSearchService = substanceSearchService;
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
}
