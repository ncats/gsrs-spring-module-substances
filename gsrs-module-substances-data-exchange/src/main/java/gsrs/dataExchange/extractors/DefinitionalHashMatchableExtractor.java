package gsrs.dataexchange.extractors;

import gsrs.holdingarea.model.MatchableKeyValueTuple;
import gsrs.holdingarea.model.MatchableKeyValueTupleExtractor;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.definitional.DefinitionalElements;
import gsrs.module.substance.services.ConfigBasedDefinitionalElementFactory;
import gsrs.module.substance.services.DefinitionalElementFactory;
import gsrs.springUtils.AutowireHelper;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class DefinitionalHashMatchableExtractor implements MatchableKeyValueTupleExtractor<Substance> {
    private final String DEFINITIONAL_HASH_KEY = "Definitional Hash - Layer ";

    @Autowired
    private DefinitionalElementFactory definitionalElementFactory;

    @Autowired
    private SubstanceLegacySearchService substanceSearchService;

    @Autowired
    public PlatformTransactionManager transactionManager;

    @Override
    public void extract(Substance substance, Consumer<MatchableKeyValueTuple> c) {
        log.trace("starting in DefinitionalHashMatchableExtractor.extract");
        /*
        wrapping the call to DefaultHoldingAreaService.getDefinitionalHash in a try/catch prevents an Exception from interrupting
        a daisy-chain of extractors called from a unit test
         */
        try {
            ConfigBasedDefinitionalElementFactory configBasedDefinitionalElementFactory = new ConfigBasedDefinitionalElementFactory();
            configBasedDefinitionalElementFactory= AutowireHelper.getInstance().autowireAndProxy(configBasedDefinitionalElementFactory);
            DefinitionalElements elements =  configBasedDefinitionalElementFactory.computeDefinitionalElementsFor(substance);
            List<String> layerHashes = elements.getDefinitionalHashLayers();
            log.trace(String.format(" %d layers", layerHashes.size()));
            for (int layer = 1; layer <= layerHashes.size(); layer++){
                String layerName = "root_definitional_hash_layer_" + layer;
                log.trace("layerName: " + layerName + ":" + layerHashes.get(layer - 1));
                MatchableKeyValueTuple tuple =
                        MatchableKeyValueTuple.builder()
                                .key(DEFINITIONAL_HASH_KEY +layer)
                                .value(layerHashes.get(layer - 1))
                                .layer(layer)
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
