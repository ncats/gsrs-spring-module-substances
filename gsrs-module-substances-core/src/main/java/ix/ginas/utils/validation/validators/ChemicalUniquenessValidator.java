package ix.ginas.utils.validation.validators;

import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidatorCallback;
import ix.core.validator.ValidatorCategory;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class ChemicalUniquenessValidator extends AbstractValidatorPlugin<Substance> {
    @Autowired
    private StructureProcessor structureProcessor;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private SubstanceLegacySearchService legacySearchService;

    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {
        log.trace("starting in validate");
        ChemicalSubstance cs = (ChemicalSubstance) objnew;

        if (cs.getStructure() == null) {
            return;
        }

        handleDuplcateCheck(cs).forEach(callback::addMessage);
    }

    @Override
    public boolean supportsCategory(Substance substance, Substance olds, ValidatorCategory c) {
        return substance instanceof ChemicalSubstance && (ValidatorCategory.CATEGORY_DEFINITION().equals(c) || ValidatorCategory.CATEGORY_ALL().equals(c)
                || ValidatorCategory.CATEGORY_DUPLICATE_CHECK().equals(c));
    }

    private List<ValidationMessage> handleDuplcateCheck(ChemicalSubstance chemicalSubstance) {
        String molfile = chemicalSubstance.getStructure().molfile;
        Structure structure = structureProcessor.instrument(molfile);
        if (structure.toChemical().getAtomCount() == 0) {
            return Collections.singletonList(GinasProcessingMessage.ERROR_MESSAGE("Please provide a structure"));
        }

        int defaultTop = 10;
        int skipZero = 0;

        String sins = structure.getStereoInsensitiveHash();
        log.trace("StereoInsensitiveHash: {}", sins);
        String hash = "( root_structure_properties_STEREO_INSENSITIVE_HASH:" + sins + " OR " + "root_moieties_properties_STEREO_INSENSITIVE_HASH:" + sins + " )";
        log.trace("query: {}", hash);
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .query(hash)
                .kind(Substance.class);
        builder.top(defaultTop);
        builder.skip(skipZero);
        SearchRequest searchRequest = builder.build();

        SearchResult result;
        try {
            result = legacySearchService.search(searchRequest.getQuery(), searchRequest.getOptions());
        } catch (Exception e) {
            log.error("Error running search for duplicates", e);
            return new ArrayList<>();
        }
        SearchResult fresult = result;

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setReadOnly(true);
        List results = transactionTemplate.execute(stauts -> {
            //the top and skip settings  look wrong, because we're not skipping
            //anything, but it's actually right,
            //because the original request did the skipping.
            //This mechanism should probably be worked out
            //better, as it's not consistent.

            //Note that the SearchResult uses a LazyList,
            //but this is copying to a real list, this will
            //trigger direct fetches from the lazylist.
            //With proper caching there should be no further
            //triggered fetching after this.

            List tlist = new ArrayList<>(defaultTop);
            fresult.copyTo(tlist, 0, defaultTop, true);
            return tlist;
        });

        List<ValidationMessage> messages = new ArrayList<>();
        results.forEach(r -> {
            Substance duplicate = (Substance) r;
            GinasProcessingMessage message = GinasProcessingMessage.WARNING_MESSAGE(
                    String.format("Record %s appears to be a duplicate", duplicate.getName()));
            message.addLink(ValidationUtils.createSubstanceLink(duplicate.asSubstanceReference()));
            messages.add(message);
        });
        if (messages.isEmpty()) {
            messages.add(GinasProcessingMessage.SUCCESS_MESSAGE("Structure is unique"));
        }
        return messages;
    }

}
