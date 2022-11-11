package ix.ginas.utils.validation.validators;

import gov.nih.ncats.common.Tuple;
import gov.nih.ncats.common.stream.StreamUtil;
import gsrs.legacy.GsrsSearchService;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.repository.NucleicAcidSubstanceRepository;
import gsrs.module.substance.repository.ReferenceRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.services.SubstanceSequenceSearchService;
import ix.core.models.Payload;

import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.SearchResultContext;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.*;
import ix.ginas.utils.NucleicAcidUtils;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.ValidationUtils;
import ix.seqaln.SequenceIndexer.CutoffType;

import lombok.extern.slf4j.Slf4j;
import org.jcvi.jillion.core.residue.nt.NucleotideSequence;
import org.springframework.beans.factory.annotation.Autowired;


import java.io.IOException;
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
public class NucleicAcidValidator extends AbstractValidatorPlugin<Substance> {

    @Autowired
    private SubstanceSequenceSearchService sequenceSearchService;

    @Autowired
    private SubstanceLegacySearchService searchService;

    @Autowired
    private NucleicAcidSubstanceRepository substanceRepository;

    @Autowired
    private ReferenceRepository referenceRepository;

    private final String NO_SUBUNIT_MESSAGE = "Warning - Nucleic Acid substances usually have at least 1 subunit, but zero subunits were found here.  This is discouraged and is only allowed for records labelled as incomplete";
    private final String NO_SUGAR_MESSAGE = "Nucleic Acid substance must have at least 1 specified sugar";
    private final String NO_LINKAGE_MESSAGE ="Nucleic Acid substance must have at least 1 specified linkage";

    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {

        NucleicAcidSubstance cs = (NucleicAcidSubstance)s;
        if (cs.nucleicAcid == null) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE("Nucleic Acid substance must have a nucleicAcid element"));
            return;
        }

        List<Subunit> subunits = cs.nucleicAcid.getSubunits();
        if (subunits == null
                || subunits.isEmpty()) {
            if(Substance.SubstanceDefinitionLevel.INCOMPLETE.equals(cs.definitionLevel)) {
                callback.addMessage(GinasProcessingMessage
                        //warning text changed 13 Oct 2021 https://cnigsllc.atlassian.net/browse/GSRS-1884
                        .WARNING_MESSAGE(NO_SUBUNIT_MESSAGE));
            }else {
                callback.addMessage(GinasProcessingMessage
                        .ERROR_MESSAGE(NO_SUBUNIT_MESSAGE));
            }
            //to make it easier for validation below set the subunits to an empty list to avoid other errors
            subunits = Collections.emptyList();
        }
        if (cs.nucleicAcid.getSugars() == null
                || cs.nucleicAcid.getSugars().isEmpty()) {
                callback.addMessage(GinasProcessingMessage
                        .ERROR_MESSAGE(NO_SUGAR_MESSAGE));
        }
        if (cs.nucleicAcid.getLinkages() == null
                || cs.nucleicAcid.getLinkages().isEmpty()) {
                callback.addMessage(GinasProcessingMessage
                        .ERROR_MESSAGE(NO_LINKAGE_MESSAGE));
        }
        for (int i=0; i< subunits.size(); i++) {
            Subunit su = subunits.get(i);
            //GSRS-735 and GSRS-1373 add sequence validation
            //for now just null/blank need to confer with stakeholders if validation is needed or not
            if(su.sequence == null || su.sequence.trim().isEmpty()){
                if(Substance.SubstanceDefinitionLevel.INCOMPLETE.equals(cs.definitionLevel)){
                    callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE("subunit at position " + (i +1) + " is blank. This is allowed but discouraged for incomplete nucleic acid records."));
                }else {
                    callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE("subunit at position " + (i +1) + " is blank"));
                }
            }
        }


        int unspSugars = NucleicAcidUtils
                .getNumberOfUnspecifiedSugarSites(cs);
        if (unspSugars != 0) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE("Nucleic Acid substance must have every base specify a sugar fragment. Missing "
                            + unspSugars + " sites."));
        }


        int unspLinkages = NucleicAcidUtils
                .getNumberOfUnspecifiedLinkageSites(cs);
        if (unspLinkages > 0) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE("Nucleic Acid substance must have every linkage specify a linkage fragment. Missing "
                            + unspLinkages + " sites."));
        }else{
            if (unspLinkages < - (subunits.size())) {
                callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE("Nucleic Acid substance must have every linkage specify a linkage fragment, but sites are over-specified. Found "
                            + (-1*unspLinkages) + " more sites than expected."));
            }
        }

        if(sequenceHasChanged(cs, objold)){
            validateSequence(cs, callback);
        }

        if(!subunits.isEmpty()) {
            ValidationUtils.validateReference(cs, cs.nucleicAcid, callback, ValidationUtils.ReferenceAction.FAIL, referenceRepository);
        }
    }
    public List<Tuple<NucleicAcidSubstance, Subunit>> executeSimpleExactNucleicAcidSubunitSearch(Subunit su) throws InterruptedException, ExecutionException, TimeoutException, IOException {
        String q = "root_nucleicAcid_subunits_sequence:" + su.sequence;
        SearchRequest request = new SearchRequest.Builder()
                .kind(NucleicAcidSubstance.class)
                .fdim(0)
                .query(q)
                .top(Integer.MAX_VALUE)
                .build();

        SearchResult sr=searchService.search(request.getQuery(), request.getOptions());
        Future<List> fut=sr.getMatchesFuture();


        Stream<Tuple<NucleicAcidSubstance, Subunit>> presults =	fut.get(10_000, TimeUnit.MILLISECONDS)
                .stream()
                .map(s->(NucleicAcidSubstance)s)
                .flatMap(sub->{
                    NucleicAcidSubstance ps = (NucleicAcidSubstance)sub;
                    return ps.nucleicAcid.getSubunits()
                            .stream()
                            .filter(sur->sur.sequence.equalsIgnoreCase(su.sequence))
                            .map(sur->Tuple.of(sub,sur));
                });
        presults=presults.map(t->Tuple.of(t.v().uuid,t).withKEquality())
                .distinct()
                .map(t->t.v());

        return presults.collect(Collectors.toList());

    }

    private void validateSequence(NucleicAcidSubstance s, ValidatorCallback callback){
        List<Subunit> subunits = s.nucleicAcid.getSubunits();

        Set<String> seen = new HashSet<>();
        for(Subunit subunit : subunits){
            if(subunit.sequence ==null || subunit.sequence.trim().isEmpty()){
               //skip here we do the real check above
                continue;
            }
            if(!seen.add(subunit.sequence)){
                //should we remove as applicable change?
            	//TP: I'm not sure we should even warn about duplicates within a record, tbh. At least with proteins,
            	//it's quite common to have subunits that are identical in sequence within the same record.
                callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE("Duplicate subunit at index " + subunit.subunitIndex));
            }

            try {
                NucleotideSequence.of(subunit.sequence);
            }catch(Exception e){
                //invalid bases
                callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
                        "invalid nucleic acid sequence base in subunit " + subunit.subunitIndex + "  " + e.getMessage()));

            }
        }
        validateSequenceDuplicates(s, callback);


    }

//    private Stream<Search> searchForMatches(String seq){
//        try {
//            SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sanitizedRequest = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
//                    .seqType("NucleicAcid")
//                    .type(CutoffType.GLOBAL)
//                    .q(seq)
//                    .build().sanitize();
//            SearchResultContext resultContext= sequenceSearchService
//                    .search(sanitizedRequest);
//            resultContext.getDeterminedFuture().get();
//            SearchRequest request = new SearchRequest.Builder()
//                    .subset(resultContext.getResults())
//                    .options(sanitizedRequest.)
//                    .skip(0)
//                    .top(resultContext.getResults().size())
//                    .query(seq)
//                    .build();
//             resultContext.getFocused(sanitizedRequest.getTop(), sanitizedRequest.getSkip(), sanitizedRequest.getFdim(), sanitizedRequest.getField())
//                     .getAdapted(request).;
//        }catch(Exception e){
//            e.printStackTrace();
//            return Stream.empty();
//        }
//    }
    
    private void validateSequenceDuplicates(
            NucleicAcidSubstance nucleicAcidSubstance, ValidatorCallback callback) {

        try {
        	nucleicAcidSubstance.nucleicAcid.subunits
            .stream()
            .collect(Collectors.groupingBy(su->su.sequence))
            .entrySet()
            .stream()
            .map(Tuple::of)
            .map(t->t.v())
            .map(l->l.stream().map(su->Tuple.of(su.subunitIndex,su).withKSortOrder())
         		   			 .sorted()
         		   			 .map(t->t.v())
         		   			 .collect(Collectors.toList()))
            .forEach(subs->{
         	   try{
         		   Subunit su=subs.get(0);
         		   String suSet = subs.stream().map(su1->su1.subunitIndex+"").collect(Collectors.joining(","));
         		   
//         	   Payload payload = _payload.get()
//         			                     .createPayload("Sequence Search","text/plain", su.sequence);
//
                String msgOne = "There is 1 substance with a similar sequence to subunit ["
                        + suSet + "]:";
                
                String msgMult = "There are ? substances with a similar sequence to subunit ["
                        + suSet + "]:";
                
                List<Function<String,List<Tuple<Double,Tuple<NucleicAcidSubstance, Subunit>>>>> searchers = new ArrayList<>();
                
                //Simplified searcher, using lucene direct index
                searchers.add(seq->{
                	try{
                	List<Tuple<NucleicAcidSubstance, Subunit>> simpleResults=executeSimpleExactNucleicAcidSubunitSearch(su);
                	
                	return simpleResults.stream()
				                	  .map(t->{
				                		  return Tuple.of(1.0,t);
				                	  })
				                      .filter(t->!t.v().k().getOrGenerateUUID().equals(nucleicAcidSubstance.getOrGenerateUUID()))
				                      .collect(Collectors.toList());
                	}catch(Exception e){
                	    e.printStackTrace();
                		log.warn("Problem performing sequence search on lucene index", e);
                		return new ArrayList<>();
                	}
                });
                /*
                //Traditional searcher using sequence indexer
                searchers.add(seq->{
                    return searchForMatches(seq)
                            //katzelda May 2021: this commented out block should be handled by the search service
//                     .map(suResult->{
//                         if(suResult ==null){
//                             return Tuple.of("ignore", Optional.empty());
//                         }
//                    	 return Tuple.of(suResult.score,substanceRepository.findNucleicAcidSubstanceByNucleicAcid_Subunits_Uuid(suResult.id));
//                     })
//                     .filter(op->op.v().isPresent())
//                     .map(Tuple.vmap(opT->opT.get()))
//                     .filter(t->!t.v().k().getOrGenerateUUID().equals(nucleicAcidSubstance.getOrGenerateUUID()))
//                     .filter(t->(t.v().k() instanceof NucleicAcidSubstance))
//                     .map(t->{
//                    	 //TODO: could easily be cleaned up.
//                    	 NucleicAcidSubstance ps=(NucleicAcidSubstance)t.v().k();
//                    	 return Tuple.of(t.k(), Tuple.of(ps, t.v().v()));
//                     })
                     //TODO: maybe sort by the similarity?
                     .collect(Collectors.toList());
                }
                );
*/
                searchers.stream()
                         .map(searcher->searcher.apply(su.sequence))
                         .filter(suResults->!suResults.isEmpty())
                         .map(res->res.stream().map(t->t.withKSortOrder(k->k)).sorted().collect(Collectors.toList()))
                         .findFirst()
                         .ifPresent(suResults->{
                             List<GinasProcessingMessage.Link> links = new ArrayList<>();
//                             GinasProcessingMessage.Link l = new GinasProcessingMessage.Link();
//                             /*
//                             Call call = ix.ginas.controllers.routes.GinasApp
//                                     .substances(payload.id.toString(), 16,1);
//                             l.href = call.url() + "&type=sequence&identity=" + SubstanceFactory.SEQUENCE_IDENTITY_CUTOFF + "&identityType=SUB&seqType=NucleicAcid";
//                             */
//                             l.text = "(Perform similarity search on subunit ["
//                                     + su.subunitIndex + "])";

                             String warnMessage=msgOne;
                             
                             if(suResults.size()>1){
                            	 warnMessage = msgMult.replace("?", suResults.size() +"");
                             }
                             
                             GinasProcessingMessage dupMessage = GinasProcessingMessage
                                     .WARNING_MESSAGE(warnMessage);
//                             dupMessage.addLink(l);
                             
                             
                             
                             suResults.stream()
                                      .map(t->t.withKSortOrder(d->d))
                                      .sorted()
                                      .forEach(tupTotal->{
                                     	 Tuple<NucleicAcidSubstance, Subunit> tup=tupTotal.v();
                                     	 double globalScore = tupTotal.k();
                                     	 String globalScoreString = (int)Math.round(globalScore*100) + "%";
                                     	 
                                     	 SubstanceReference sr = tup.k().asSubstanceReference();
                                         GinasProcessingMessage.Link l2 = ValidationUtils.createSubstanceLink(sr);
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
         	   }catch(Exception e){
         		   e.printStackTrace();
         	   }
            });
            
        } catch (Exception e) {
        	log.error("Problem executing duplicate search function", e);
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE("Error performing seqeunce search on Nucleic Acid:"
                            + e.getMessage()));
        }
    }


    private boolean sequenceHasChanged(NucleicAcidSubstance s, Substance objold) {
        if(objold ==null || !(objold instanceof NucleicAcidSubstance)){
            //new substance or converted from different type like a concept
            return true;
        }
        NucleicAcid newNa = s.nucleicAcid;
        NucleicAcid oldNa = ((NucleicAcidSubstance)objold).nucleicAcid;

        List<Subunit> newSubunits = newNa.getSubunits();
        List<Subunit> oldSubunits = oldNa.getSubunits();

        if(newSubunits.size() != oldSubunits.size()){
            return true;
        }
        //I guess it's possible someone edits the nucleic acid and adds a subunit
        //anywhere in the list so we have to check everywhere.
        //also only really care about similar sequences not really order
        //so an edit can rearrange the order too...

        Set<String> newSeqs = newSubunits.stream().map(sub-> sub.sequence).collect(Collectors.toSet());
        Set<String> oldSeqs = oldSubunits.stream().map(sub-> sub.sequence).collect(Collectors.toSet());

        return !newSeqs.equals(oldSeqs);
    }
}
