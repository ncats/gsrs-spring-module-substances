package ix.ginas.utils.validation.validators;

import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.definitional.DefinitionalElements;
import gsrs.module.substance.services.DefinitionalElementFactory;
import gsrs.springUtils.AutowireHelper;
import ix.core.search.SearchOptions;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Amount;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Moiety;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author Mitch Miller
 */
@Slf4j
public class SaltValidator extends AbstractValidatorPlugin<Substance> {

    public SaltValidator() {
        if (definitionalElementFactory == null) {
            AutowireHelper.getInstance().autowire(this);
        }
    }

    @Autowired(required = true)
    private DefinitionalElementFactory definitionalElementFactory;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    private SubstanceLegacySearchService searchService;

    @Override
    public void validate(Substance substanceORIG, Substance oldSubstanceORIG, ValidatorCallback callback) {
        if (!(substanceORIG instanceof ChemicalSubstance)) {
            return;
        }
        ChemicalSubstance substance = (ChemicalSubstance) substanceORIG;

        log.trace("starting in SaltValidator.validate");
        //is this a salt?
        log.trace("starting in SaltValidator.validate. substance.moieties.size() " + substance.moieties.size());
        if (substance.moieties.size() > 1) {
            List<String> smilesWithoutMatch = new ArrayList();
            List<String> smilesWithPartialMatch = new ArrayList();

            for (Moiety moiety : substance.moieties) {
                log.trace("Looking up moiety with SMILES " + moiety.structure.smiles);

                ChemicalSubstance moietyChemical = new ChemicalSubstance();
                moietyChemical.setStructure(moiety.structure);
                //clone the moiety and set fixed values so that we avoid flagging a moiety as not matching
                Moiety clone = new Moiety();
                clone.structure = moiety.structure;
                Amount cloneAmount = new Amount();
                cloneAmount.average = 1.0d;
                cloneAmount.units = "MOL RATIO";
                cloneAmount.type = "MOL RATIO";
                clone.setCountAmount(cloneAmount);
                moietyChemical.moieties.add(clone);

                List<Substance> layer1Matches = findDefinitionaLayer1lDuplicateCandidates(moietyChemical);
                log.trace("(SaltValidator) total layer1 matches: " + layer1Matches.size());

                //skip the look-up of full matches when there are no layer1 matches
                List<Substance> fullMatches = (layer1Matches.isEmpty()) ? new ArrayList()
                        : findFullDefinitionalDuplicateCandidates(moietyChemical);
                log.trace("(SaltValidator) total full matches: " + fullMatches.size());

                if (fullMatches.isEmpty()) {
                    if (layer1Matches.isEmpty()) {
                        smilesWithoutMatch.add(moiety.structure.smiles);
                    }
                    else {
                        smilesWithPartialMatch.add(moiety.structure.smiles);
                    }
                }
            }

            if (!smilesWithoutMatch.isEmpty()) {
                smilesWithoutMatch.forEach(s -> {
                    callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE("Each fragment should be present as a separate record in the database. Please register: " + s));
                });
            }
            if (!smilesWithPartialMatch.isEmpty()) {
                smilesWithPartialMatch.forEach(s -> {
                    callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE("This fragment is present as a separate record in the database but in a different form. Please register: "
                            + s + " as an individual substance"));

                });
            }

            if (smilesWithoutMatch.isEmpty() && smilesWithPartialMatch.isEmpty()) {
                log.trace("all moieties are present in the database");
            }
        }
    }

    public List<Substance> findFullDefinitionalDuplicateCandidates(Substance substance) {
        DefinitionalElements newDefinitionalElements = definitionalElementFactory.computeDefinitionalElementsFor(substance);
        int layer = newDefinitionalElements.getDefinitionalHashLayers().size() - 1; // hashes.size()-1;
        log.debug("handling layer: " + (layer + 1));
        return findLayerNDefinitionalDuplicateCandidates(substance, layer);
    }

    public List<Substance> findDefinitionaLayer1lDuplicateCandidates(Substance substance) {
//        if (definitionalElementFactory == null) {
//            AutowireHelper.getInstance().autowire(this);
//        }
        int layer = 0;
        return findLayerNDefinitionalDuplicateCandidates(substance, layer);
    }

    public List<Substance> findLayerNDefinitionalDuplicateCandidates(Substance substance, int layer) {
        List<Substance> candidates = new ArrayList<>();
        try {
            //SearchRequest.Builder searchBuilder = new SearchRequest.Builder();
            DefinitionalElements newDefinitionalElements = definitionalElementFactory.computeDefinitionalElementsFor(substance);
            //List<String> hashes= substance.getDefinitionalElements().getDefinitionalHashLayers();
            log.debug("handling layer: " + (layer + 1));
            //System.out.println("findFullDefinitionalDuplicateCandidates handling layer: " + (layer + 1));
            String searchItem = "root_definitional_hash_layer_" + (layer + 1) + ":"
                    + newDefinitionalElements.getDefinitionalHashLayers().get(layer);
            log.debug("layer query: " + searchItem);
            
            TransactionTemplate transactionSearch = new TransactionTemplate(transactionManager);
            candidates = (List<Substance>) transactionSearch.execute(ts
                    -> {
                List<String> nameValues = new ArrayList<>();
                SearchRequest request = new SearchRequest.Builder()
                        .kind(Substance.class)
                        .fdim(0)
                        .query(searchItem)
                        .top(Integer.MAX_VALUE)
                        .build();
                //System.out.println("built query: " + request.getQuery());
                try {
                    SearchOptions options = new SearchOptions();
                    SearchResult sr = searchService.search(request.getQuery(), options);
                    sr.waitForFinish();
                    List fut = sr.getMatches();
                    List<Substance> hits = (List<Substance>) fut.stream()
                            .map(s -> (Substance) s)
                            .collect(Collectors.toList());
                    return hits;
                } catch (Exception ex) {
                    System.err.println("error during search");
                    ex.printStackTrace();
                }
                return nameValues;
            });
            //nameValues.forEach(n -> System.out.println(n));
            //        });
            //
            //			SearchResult sres = searchBuilder
            //							.kind(Substance.class)
            //							.fdim(0)
            //							.build()
            //                    .
            //							.execute();
            //			sres.waitForFinish();
            /*List<Substance> submatches = (List<Substance>) sres.getMatches();
			Logger.getLogger(this.getClass().getName()).log(Level.FINE, "total submatches: " + submatches.size());

			for (int i = 0; i < submatches.size(); i++)	{
				Substance s = submatches.get(i);
				if (!s.getUuid().equals(substance.getUuid()))	{
					candidates.add(s);
				}
			}*/
        } catch (Exception ex) {
            log.error("Error running query", ex);
        }
        return candidates;
    }

}
