package ix.ginas.utils.validation;

import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.services.DefinitionalElementFactory;
import org.springframework.transaction.PlatformTransactionManager;

/**
*
* @author mitch
*/
public class DefHashCalcRequirements {

    private DefinitionalElementFactory definitionalElementFactory;
    private SubstanceLegacySearchService substanceLegacySearchService;
    private PlatformTransactionManager platformTransactionManager;

    public DefHashCalcRequirements(DefinitionalElementFactory definitionalElementFactory, SubstanceLegacySearchService substanceLegacySearchService, PlatformTransactionManager platformTransactionManager) {
        this.definitionalElementFactory = definitionalElementFactory;
        this.substanceLegacySearchService = substanceLegacySearchService;
        this.platformTransactionManager = platformTransactionManager;
    }

    public DefinitionalElementFactory getDefinitionalElementFactory() {
        return definitionalElementFactory;
    }

    public void setDefinitionalElementFactory(DefinitionalElementFactory definitionalElementFactory) {
        this.definitionalElementFactory = definitionalElementFactory;
    }

    public SubstanceLegacySearchService getSubstanceLegacySearchService() {
        return substanceLegacySearchService;
    }

    public void setSubstanceLegacySearchService(SubstanceLegacySearchService substanceLegacySearchService) {
        this.substanceLegacySearchService = substanceLegacySearchService;
    }

    public PlatformTransactionManager getPlatformTransactionManager() {
        return platformTransactionManager;
    }

    public void setPlatformTransactionManager(PlatformTransactionManager platformTransactionManager) {
        this.platformTransactionManager = platformTransactionManager;
    }
    
}

