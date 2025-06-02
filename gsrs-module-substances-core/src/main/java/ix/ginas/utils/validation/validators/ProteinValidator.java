package ix.ginas.utils.validation.validators;

import gov.nih.ncats.common.Tuple;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.repository.ReferenceRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.utils.MolWeightCalculatorProperties;
import gsrs.service.PayloadService;
import ix.core.models.Payload;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.*;
import ix.ginas.utils.MolecularWeightAndFormulaContribution;
import ix.ginas.utils.ProteinUtils;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.ValidationUtils;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by katzelda on 5/14/18.
 */
@Slf4j
//@Component
//@Data
public class ProteinValidator extends AbstractValidatorPlugin<Substance>
{

    //private CachedSupplier<PayloadPlugin> _payload = CachedSupplier.of(()-> Play.application().plugin(PayloadPlugin.class));
    @Autowired
    private PayloadService payloadService;

    //private CachedSupplier<SequenceIndexerPlugin> _seqIndexer = CachedSupplier.of(() -> Play.application().plugin(SequenceIndexerPlugin.class));

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    private MolWeightCalculatorProperties molWeightCalculatorProperties;

    @Value("${ix.gsrs.protein.mw.percent.tolerance:1}")
    private double tolerance = 1D;

    @Autowired
    private ReferenceRepository referenceRepository;

    @Autowired
    private SubstanceLegacySearchService searchService; 
    
    private final String ProteinValidatorElementError = "ProteinValidatorElementError";
    private final String ProteinValidatorSubunitError1 = "ProteinValidatorSubunitError1";
    private final String ProteinValidatorSubunitError2 = "ProteinValidatorSubunitError2";
    private final String ProteinValidatorSubunitWarning1 = "ProteinValidatorSubunitWarning1";
    private final String ProteinValidatorSubunitWarning2 = "ProteinValidatorSubunitWarning2";
    private final String ProteinValidatorSubunitWarning3 = "ProteinValidatorSubunitWarning3";
    private final String ProteinValidatorDisulfideError = "ProteinValidatorDisulfideError";
    private final String ProteinValidatorSiteError1 = "ProteinValidatorSiteError1";
    private final String ProteinValidatorSiteError2 = "ProteinValidatorSiteError2";
    private final String ProteinValidatorWarning = "ProteinValidatorWarning";
    private final String ProteinValidatorWeightWarning1 = "ProteinValidatorWeightWarning1";
    private final String ProteinValidatorWeightWarning2 = "ProteinValidatorWeightWarning2";
    private final String ProteinValidatorWeightWarning3 = "ProteinValidatorWeightWarning3";
    private final String ProteinValidatorFormulaWarning1 = "ProteinValidatorFormulaWarning1";
    private final String ProteinValidatorFormulaWarning2 = "ProteinValidatorFormulaWarning2";
    private final String ProteinValidatorSequenceWarning = "ProteinValidatorSequenceWarning";
    private final String ProteinValidatorSequenceError = "ProteinValidatorSequenceError";

    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {

        ProteinSubstance cs = (ProteinSubstance) objnew;

        List<GinasProcessingMessage> gpm = new ArrayList<GinasProcessingMessage>();
        if (cs.protein == null) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE(ProteinValidatorElementError, "Protein substance must have a protein element"));
        }
        else {
            if (cs.protein.subunits.isEmpty()) {
                if (Substance.SubstanceDefinitionLevel.INCOMPLETE.equals(cs.definitionLevel)) {
                    callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(ProteinValidatorSubunitWarning1, 
                    		"Having no subunits is allowed but discouraged for incomplete protein records."));
                }
                else {
                    callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(ProteinValidatorSubunitError1, 
                    		"Complete protein substance must have at least one Subunit element. Please add a subunit, or mark as incomplete."));
                }
            }
            for (int i = 0; i < cs.protein.subunits.size(); i++) {
                Subunit su = cs.protein.subunits.get(i);
                if (su.subunitIndex == null) {
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE(ProteinValidatorSubunitWarning2,
                            		"Protein subunit (at " + (i + 1) + " position) has no subunit index, defaulting to:" + (i + 1))
                                    .appliableChange(true);
                    Integer newValue = i + 1;
                    callback.addMessage(mes, () -> su.subunitIndex = newValue);

                }

                //GSRS-735 add sequence validation
                //for now just null/blank need to confer with stakeholders if amino acid validation is needed or not
                if (su.sequence == null || su.sequence.trim().isEmpty()) {
                    if (Substance.SubstanceDefinitionLevel.INCOMPLETE.equals(cs.definitionLevel)) {
                        callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(ProteinValidatorSubunitWarning3, "Subunit at position " + (i + 1) + " is blank. This is allowed but discouraged for incomplete protein records."));
                    }
                    else {
                        callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(ProteinValidatorSubunitError2, "Subunit at position " + (i + 1) + " is blank"));
                    }
                }
            }
        }

        for (DisulfideLink l : cs.protein.getDisulfideLinks()) {

            List<Site> sites = l.getSites();
            if (sites.size() != 2) {
                GinasProcessingMessage mes = GinasProcessingMessage
                        .ERROR_MESSAGE(ProteinValidatorDisulfideError, "Disulfide Link \"" + sites.toString() + "\" has " + sites.size() + " sites, should have 2");
                callback.addMessage(mes);
            }
            else {
                for (Site s : sites) {
                    String res = cs.protein.getResidueAt(s);
                    if (res == null) {
                        GinasProcessingMessage mes = GinasProcessingMessage
                                .ERROR_MESSAGE(ProteinValidatorSiteError1, "Site \"" + s.toString() + "\" does not exist");
                        callback.addMessage(mes);
                    }
                    else {
                        if (!res.equalsIgnoreCase("C")) {
                            GinasProcessingMessage mes = GinasProcessingMessage
                                    .ERROR_MESSAGE(ProteinValidatorSiteError2, "Site \"" + s.toString() + "\" in disulfide link is not a Cysteine, found: \"" + res + "\"");
                            callback.addMessage(mes);
                        }
                    }
                }
            }

        }

        Set<String> unknownRes = new HashSet<String>();
        //double tot = ProteinUtils.generateProteinWeight(cs, unknownRes);
        MolecularWeightAndFormulaContribution mwFormulaContribution = ProteinUtils.generateProteinWeightAndFormula(substanceRepository, cs, unknownRes);
        double tot = mwFormulaContribution.getMw();
        double low = mwFormulaContribution.getMwLow();
        double high = mwFormulaContribution.getMwHigh();
        double lowLimit = mwFormulaContribution.getMwLowLimit();
        double highLimit = mwFormulaContribution.getMwHighLimit();
        log.debug(String.format("in validate, low: %.2f; high: %.2f; lowLimit: %.2f, highLimit: %.2f",
                low, high, lowLimit, highLimit));

        double valueTolerance = tolerance / 100.0d;//convert from percentage to decimal
        String msg = String.format("valueTolerance: %.4f", valueTolerance);
        log.debug(msg);

        if (unknownRes.size() > 0) {
            GinasProcessingMessage mes = GinasProcessingMessage
                    .WARNING_MESSAGE(ProteinValidatorWarning, "Protein has unknown amino acid residues: %s" + unknownRes.toString());
            callback.addMessage(mes);
        }
        mwFormulaContribution.getMessages().forEach(m -> callback.addMessage(m));

        List<Property> molprops = ProteinUtils.getMolWeightProperties(cs);
        if (molprops.isEmpty()) {
            Property calculatedMolWeight = molWeightCalculatorProperties.calculateMolWeightProperty(tot, low, high, lowLimit, highLimit);
            GinasProcessingMessage mes = GinasProcessingMessage
                    .WARNING_MESSAGE(ProteinValidatorWeightWarning1,
                            "Protein has no molecular weight, defaulting to calculated value of: %s" + calculatedMolWeight.getValue())
                    .appliableChange(true);
            callback.addMessage(mes, () -> {

                cs.properties.add(calculatedMolWeight);
                if (!unknownRes.isEmpty()) {
                    GinasProcessingMessage mes2 = GinasProcessingMessage
                            .WARNING_MESSAGE(ProteinValidatorWeightWarning2,
                            		"Calculated protein weight questionable, due to unknown amino acid residues: %s"+ unknownRes.toString());
                    callback.addMessage(mes2);
                }
            });

        }
        else {
            log.trace(String.format("protein has existing %d mw property", molprops.size()));
            for (Property p : molprops) {
                if (p.getValue() != null) {
                    log.trace("has value " + p.getValue());
                    Double avg = p.getValue().average;
                    if (avg == null) {
                        continue;
                    }
                    double delta = tot - avg;
                    double pdiff = delta / (avg);

                    msg = String.format("evaluating existing avg: %.2f vs. %.2f. pdiff: %.2f", avg, tot, pdiff);
                    log.trace(msg);
                    /*int len = 0;
                    for (Subunit su : cs.protein.subunits) {
                        len += su.sequence.length();
                    }
                    double avgoff = delta / len;  commented out per discussion with Tyler 29 June 2020 */
                    if (Math.abs(pdiff) > valueTolerance) {
                        callback.addMessage(GinasProcessingMessage
                                .WARNING_MESSAGE(ProteinValidatorWeightWarning3, "Calculated weight [" + String.format("%.2f", tot) + "] is greater than " 
                                		+ String.format("%.2f", tolerance) + "%" 
                                		+ " off of given weight [" + String.format("%.2f", p.getValue().average) + "]"));
                        //katzelda May 2018 - turn off appliable change since there isn't anything to change it to.
//                                    .appliableChange(true));
                    }
                    //if it gets this far, break out of the properties
                    break;
                }
                else {
                    log.trace("no value");
                }
            }
        }

        List<Property> formulaProperties = ProteinUtils.getMolFormulaProperties(cs);
        if (formulaProperties.size() <= 0) {

            GinasProcessingMessage mes = GinasProcessingMessage
                    .WARNING_MESSAGE(ProteinValidatorFormulaWarning1,
                            "Protein has no molecular formula property, defaulting to calculated value of: " + 
                            mwFormulaContribution.getFormula())
                    .appliableChange(true);
            callback.addMessage(mes, () -> {

                cs.properties.add(ProteinUtils.makeMolFormulaProperty(mwFormulaContribution.getFormula()));
                if (!unknownRes.isEmpty()) {
                    GinasProcessingMessage mes2 = GinasProcessingMessage
                            .WARNING_MESSAGE(ProteinValidatorFormulaWarning2,
                            		"Calculated protein formula questionable due to unknown amino acid residues: " +
                                    unknownRes.toString());
                    callback.addMessage(mes2);
                }
            });
        }

        boolean sequenceHasChanged = sequenceHasChanged(cs, objold);

        if (sequenceHasChanged) {
            validateSequenceDuplicates(cs, callback);
        }

        if (!cs.protein.getSubunits().isEmpty()) {
            ValidationUtils.validateReference(cs, cs.protein, callback, ValidationUtils.ReferenceAction.FAIL,
                    referenceRepository);
        }
    }

    private static boolean sequenceHasChanged(ProteinSubstance cs, Substance o) {
        if (o == null || !(o instanceof ProteinSubstance)) {
            return true;
        }
        ProteinSubstance old = (ProteinSubstance) o;
        Protein newProtein = cs.protein;
        Protein oldProtein = old.protein;

        if (oldProtein == null) {
            return newProtein != null;
        }
        List<Subunit> newSubs = newProtein.getSubunits();
        List<Subunit> oldSubs = oldProtein.getSubunits();
        if (newSubs.size() != oldSubs.size()) {
            return true;
        }
        int size = newSubs.size();
        for (int i = 0; i < size; i++) {
            Subunit newSub = newSubs.get(i);
            Subunit oldSub = oldSubs.get(i);

            //handles null
            if (!Objects.equals(newSub.sequence, oldSub.sequence)) {
                return true;
            }
        }
        return false;
    }

    private void validateSequenceDuplicates(
            ProteinSubstance proteinsubstance, ValidatorCallback callback) {

        try {
            proteinsubstance.protein.subunits
                    .stream()
                    .filter(su-> su !=null && su.sequence !=null)
                    .collect(Collectors.groupingBy(su -> su.sequence))
                    .entrySet()
                    .stream()
                    .map(Tuple::of)
                    .map(t -> t.v())
                    .map(l -> l.stream().map(su -> Tuple.of(su.subunitIndex, su).withKSortOrder())
                    .sorted()
                    .map(t -> t.v())
                    .collect(Collectors.toList()))
                    .forEach(subs -> {
                        try {
                            Subunit su = subs.get(0);
                            String suSet = subs.stream().map(su1 -> su1.subunitIndex + "").collect(Collectors.joining(","));

                            Payload payload = new Payload();
                            /*payload = _payload.get()
                                    .createPayload("Sequence Search", "text/plain", su.sequence);*/

                            List<Function<String, List<Tuple<Double, Tuple<ProteinSubstance, Subunit>>>>> searchers = new ArrayList<>();

                            //Simplified searcher, using lucene direct index
                            searchers.add(seq -> {
                                try {
                                    List<Tuple<ProteinSubstance, Subunit>> simpleResults = executeSimpleExactProteinSubunitSearch(su);

                                    return simpleResults.stream()
                                            .map(t -> {
                                                return Tuple.of(1.0, t);
                                            })
                                            .filter(t -> !t.v().k().getOrGenerateUUID().equals(proteinsubstance.getOrGenerateUUID()))
                                            .collect(Collectors.toList());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    log.warn("Problem performing sequence search on lucene index", e);
                                    return new ArrayList<>();
                                }
                            });

                            //Traditional searcher using sequence indexer
//                            searchers.add(seq -> {
//                                return StreamUtil.forEnumeration(_seqIndexer.get()
//                                        .getIndexer()
//                                        .search(seq, SubstanceFactory.SEQUENCE_IDENTITY_CUTOFF, CutoffType.GLOBAL, "Protein"))
//                                        .map(suResult -> {
//                                            return Tuple.of(suResult.score, SubstanceFactory.getSubstanceAndSubunitFromSubunitID(suResult.id));
//                                        })
//                                        .filter(op -> op.v().isPresent())
//                                        .map(Tuple.vmap(opT -> opT.get()))
//                                        .filter(t -> !t.v().k().getOrGenerateUUID().equals(proteinsubstance.getOrGenerateUUID()))
//                                        .filter(t -> (t.v().k() instanceof ProteinSubstance))
//                                        .map(t -> {
//                                            //TODO: could easily be cleaned up.
//                                            ProteinSubstance ps = (ProteinSubstance) t.v().k();
//                                            return Tuple.of(t.k(), Tuple.of(ps, t.v().v()));
//                                        })
//                                        //TODO: maybe sort by the similarity?
//                                        .collect(Collectors.toList());
//                            });

                            searchers.stream()
                                    .map(searcher -> searcher.apply(su.sequence))
                                    .filter(suResults -> !suResults.isEmpty())
                                    .map(res -> res.stream().map(t -> t.withKSortOrder(k -> k)).sorted().collect(Collectors.toList()))
                                    .findFirst()
                                    .ifPresent(suResults -> {
                                        List<GinasProcessingMessage.Link> links = new ArrayList<>();
//                                        GinasProcessingMessage.Link l = new GinasProcessingMessage.Link();
////                                        Call call = ix.ginas.controllers.routes.GinasApp
////                                                .substances(payload.id.toString(), 16, 1);
////                                        l.href = call.url() + "&type=sequence&identity=" + SubstanceFactory.SEQUENCE_IDENTITY_CUTOFF + "&identityType=SUB&seqType=Protein";
//                                        l.text = "(Perform similarity search on subunit ["
//                                                + su.subunitIndex + "])";

                                        String msgMod = "is 1 substance";
                                        if(suResults.size()>1){
                                            msgMod = "are " + suResults.size() + " substances";
                                        }

                                        GinasProcessingMessage dupMessage = GinasProcessingMessage
                                                .WARNING_MESSAGE(ProteinValidatorSequenceWarning, "There " + msgMod + " with a similar sequence to subunit [" + suSet + "]:");
//                                        dupMessage.addLink(l);

                                        suResults.stream()
                                                .map(t -> t.withKSortOrder(d -> d))
                                                .sorted()
                                                .forEach(tupTotal -> {
                                                    Tuple<ProteinSubstance, Subunit> tup = tupTotal.v();
                                                    double globalScore = tupTotal.k();
                                                    String globalScoreString = (int) Math.round(globalScore * 100) + "%";
                                                    SubstanceReference sr = tup.k().asSubstanceReference();
                                                    GinasProcessingMessage.Link l2 = ValidationUtils.createSubstanceLink(sr);
//                                                    Call call2 = ix.ginas.controllers.routes.GinasApp.substance(tup.k().uuid.toString());
//                                                    l2.href = call2.url();
                                                    if (globalScore == 1) {
                                                        l2.text = "found exact duplicate (" + globalScoreString + ") sequence in "
                                                                + "Subunit [" + tup.v().subunitIndex + "] of \"" + tup.k().getApprovalIDDisplay() + "\" "
                                                                + "(\"" + tup.k().getName() + "\")";
                                                    }
                                                    else {
                                                        l2.text = "found approximate duplicate (" + globalScoreString + ") sequence in "
                                                                + "Subunit [" + tup.v().subunitIndex + "] of \"" + tup.k().getApprovalIDDisplay() + "\" "
                                                                + "(\"" + tup.k().getName() + "\")";
                                                    }
                                                    links.add(l2);
                                                });

                                        dupMessage.addLinks(links);
                                        callback.addMessage(dupMessage);
                                    });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            log.error("Problem executing duplicate search function", e);
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE(ProteinValidatorSequenceError, "Error performing seqeunce search on protein:" + e.getMessage()));
        }
    }

    /*
    Copied from a corresponding method within NucleicAcidValidator
     */
    public List<Tuple<ProteinSubstance, Subunit>> executeSimpleExactProteinSubunitSearch(Subunit su) throws InterruptedException, ExecutionException,
            TimeoutException, IOException {
        String q = "root_protein_subunits_sequence:" + su.sequence;
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                .query(q)
                .top(Integer.MAX_VALUE)
                .build();

        SearchResult sr = searchService.search(request.getQuery(), request.getOptions());
        Future<List> fut = sr.getMatchesFuture();

        Stream<Tuple<ProteinSubstance, Subunit>> presults = fut.get(10_000, TimeUnit.MILLISECONDS)
                .stream()
                .map(s -> (ProteinSubstance) s)
                .flatMap(sub -> {
                    ProteinSubstance ps = (ProteinSubstance) sub;
                    return ps.protein.getSubunits()
                            .stream()
                            .filter(sur -> sur.sequence.equalsIgnoreCase(su.sequence))
                            .map(sur -> Tuple.of(sub, sur));
                });
        presults = presults.map(t -> Tuple.of(t.v().uuid, t).withKEquality())
                .distinct()
                .map(t -> t.v());

        return presults.collect(Collectors.toList());

    }

}
