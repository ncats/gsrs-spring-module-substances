package ix.ginas.utils.validation;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.definitional.DefinitionalElements;
import gsrs.module.substance.repository.ReferenceRepository;

import ix.core.models.*;
import ix.core.search.SearchOptions;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;

import ix.core.validator.*;

import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.v1.*;
import ix.ginas.models.v1.Substance.SubstanceClass;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategy;
import ix.ginas.utils.NucleicAcidUtils;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jcvi.jillion.internal.core.util.Sneak;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class ValidationUtils {


	private static final String VALIDATION_CONF = "validation.conf";

	public static interface ValidationRule<K>{
		public GinasProcessingMessage validate(K obj);
	}
	
	public static class SubstanceCantBeNull implements ValidationRule<Substance>{
		@Override
		public GinasProcessingMessage validate(Substance s) {
			if (s == null) {
				return GinasProcessingMessage
						.ERROR_MESSAGE("Substance cannot be parsed");
			}
			return GinasProcessingMessage.SUCCESS_MESSAGE("Substance is parsable");
		}
	}
	
	
	public static GinasProcessingMessage.Link createSubstanceLink(SubstanceReference s){
        	GinasProcessingMessage.Link l = new GinasProcessingMessage.Link();
		//TODO: it makes total sense that the REST API should return an API link, but it can't
		//for backwards compatibility

	//        l.href= GsrsLinkUtil.computeSelfLinkFor(entityLinks, Substance.class, s.getLinkingID()).getHref();

		l.href= "/app/substance/" + s.getLinkingID();
		l.text="[" + s.getLinkingID() + "]" + s.getName();
		return l;
	}
	
	public enum ReferenceAction {
		FAIL, WARN, ALLOW
	}

	private static <D extends GinasAccessReferenceControlled> void validatePublicReferenced(Substance s,
                                                                                            D data,
                                                                                            List<GinasProcessingMessage> gpm,
                                                                                            GsrsProcessingStrategy strat,
                                                                                            Function<D,String> namer) {
		//If public
		if(data.getAccess().isEmpty()){
			boolean hasPublicReference = data.getReferences().stream()
			                    .map(r->r.getValue())
			                    .map(r->s.getReferenceByUUID(r))
			                    .filter(r->r.isPublic())
			                    .filter(r->r.isPublicDomain())
			                    .findAny()
			                    .isPresent();

			if(!hasPublicReference){
				GinasProcessingMessage mes = GinasProcessingMessage
						.ERROR_MESSAGE("%s needs an unprotected reference marked \"Public Domain\" in order to be made public.", namer.apply(data));
				gpm.add(mes);
				strat.processMessage(mes);
			}
		}

	}

	private static boolean validateReferenced(Substance s,
                                              GinasAccessReferenceControlled data,
                                              List<GinasProcessingMessage> gpm, GsrsProcessingStrategy strat,
                                              ReferenceAction onemptyref) {

		boolean worked = true;

		Set<Keyword> references = data.getReferences();
		if ((references == null || references.size() <= 0)) {
			if (onemptyref == ReferenceAction.ALLOW) {
				return worked;
			}

			GinasProcessingMessage gpmerr = null;
			if (onemptyref == ReferenceAction.FAIL) {
				gpmerr = GinasProcessingMessage.ERROR_MESSAGE(
						"%s needs at least 1 reference", data.toString())
						.appliableChange(true);
			} else if (onemptyref == ReferenceAction.WARN) {
				gpmerr = GinasProcessingMessage.WARNING_MESSAGE(
						"%s needs at least 1 reference", data.toString())
						.appliableChange(true);
			} else {
				gpmerr = GinasProcessingMessage.WARNING_MESSAGE(
						"%s needs at least 1 reference", data.toString())
						.appliableChange(true);
			}

				strat.processMessage(gpmerr);

			if (gpmerr.actionType == GinasProcessingMessage.ACTION_TYPE.APPLY_CHANGE) {
				gpmerr.appliedChange = true;
				Reference r = Reference.SYSTEM_ASSUMED();
				s.references.add(r);
				data.addReference(r.getOrGenerateUUID().toString());
			} else {
				worked = false;
			}
			gpm.add(gpmerr);
		} else {
			for (Keyword ref : references) {
				Reference r = s.getReferenceByUUID(ref.getValue());
				if (r == null) {
					gpm.add(GinasProcessingMessage.ERROR_MESSAGE("Reference \"%s\" not found on substance.",
							ref.getValue()));
					worked = false;
				}
			}
		}

		return worked;
	}

	public static boolean validateReference(Substance s,
											GinasAccessReferenceControlled data,
											ValidatorCallback callback,
											ReferenceAction onemptyref,
											ReferenceRepository referenceRepository) {

		AtomicBoolean worked = new AtomicBoolean(true);

		Set<Keyword> references = data.getReferences();
		if ((references == null || references.size() <= 0)) {
			if (onemptyref == ReferenceAction.ALLOW) {
				return worked.get();
			}

			GinasProcessingMessage gpmerr = null;
			if (onemptyref == ReferenceAction.FAIL) {
				gpmerr = GinasProcessingMessage.ERROR_MESSAGE(
						"%s needs at least 1 reference", data.toString())
						.appliableChange(true);
				worked.set(false);
			} else if (onemptyref == ReferenceAction.WARN) {
				gpmerr = GinasProcessingMessage.WARNING_MESSAGE(
						"%s needs at least 1 reference", data.toString())
						.appliableChange(true);
				worked.set(false);
			} else {
				gpmerr = GinasProcessingMessage.WARNING_MESSAGE(
						"%s needs at least 1 reference", data.toString())
						.appliableChange(true);
				worked.set(false);
			}

			//this extra reference is so it's effectively final
			//and we can reference it in the lambda
			GinasProcessingMessage gpmerr2 = gpmerr;
			callback.addMessage(gpmerr2, () ->{
				gpmerr2.appliedChange = true;
				Reference r = Reference.SYSTEM_ASSUMED();
				s.references.add(r);
				data.addReference(r.getOrGenerateUUID().toString());
				worked.set(true);
			});

		} else {
			for (Keyword ref : references) {
				Reference r = s.getReferenceByUUID(ref.getValue());
				if (r == null) {
					//GSRS-933 more informative error message if you can find the Reference
					Reference dbReference = referenceRepository.getOne(UUID.fromString(ref.getValue()));
					if(dbReference !=null) {
						callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(
								"Reference type: \"%s\" citation: \"%s\" uuid \"%s\" not found on substance.",
								dbReference.docType, dbReference.citation, ref.getValue()));
					}else{
					callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE("Reference \"%s\" not found on substance.",
							ref.getValue()));
					}
					worked.set(false);
				}
			}
		}

		return worked.get();
	}

       public static CachedSupplier<List<Replacer>> replacers = CachedSupplier.of(()->{
               List<Replacer> repList = new ArrayList<>();
               repList.add(new Replacer("[\\t\\n\\r]", " ")
                               .message("Name \"$0\" has non-space whitespace characters. They will be replaced with spaces."));
               repList.add(new Replacer("\\s\\s\\s*", " ")
                       .message("Name \"$0\" has consecutive whitespace characters. These will be replaced with single spaces."));

               return repList;

       });

       public static class Replacer{
               Pattern p;
               String replace;
               String message = "String \"$0\" matches forbidden pattern";

               public Replacer(String regex, String replace){
                       this.p=Pattern.compile(regex);
                       this.replace=replace;
               }

               public boolean matches(String test){
                       return this.p.matcher(test).find();
               }
              public String fix(String test){
                       return test.replaceAll(p.pattern(), replace);
               }

               public Replacer message(String msg){
                       this.message=msg;
                       return this;
               }

               public String getMessage(String test){
                       return message.replace("$0", test);
               }

       }


	/**
	 * Check if the object is effectively null. That is,
	 * is it literally null, or is the string representation
	 * of the object an empty string.
	 *
	 * @param o
	 * @return
	 */
	public static boolean isEffectivelyNull(Object o) {
		if (o == null) {
			return true;
		}
		return o.toString().isEmpty();
	}

	private static boolean validateRelationships(Substance s,
                                                 List<GinasProcessingMessage> gpm, GsrsProcessingStrategy strat) {
		List<Relationship> remnames = new ArrayList<Relationship>();
		for (Relationship n : s.relationships) {
			if (isEffectivelyNull(n)) {
				GinasProcessingMessage mes = GinasProcessingMessage
						.WARNING_MESSAGE(
								"Null relationship objects are not allowed")
						.appliableChange(true);
				gpm.add(mes);
				strat.processMessage(mes);
				if (mes.actionType == GinasProcessingMessage.ACTION_TYPE.APPLY_CHANGE) {
					remnames.add(n);
					mes.appliedChange = true;
				}
			}
			if (isEffectivelyNull(n.relatedSubstance)) {
				GinasProcessingMessage mes = GinasProcessingMessage
						.ERROR_MESSAGE(
								"Relationships must specify a related substance");
				gpm.add(mes);
				strat.processMessage(mes);
			}
			if(isEffectivelyNull(n.type)){
				GinasProcessingMessage mes = GinasProcessingMessage
						.ERROR_MESSAGE(
								"Relationships must specify a type");
				gpm.add(mes);
				strat.processMessage(mes);
			}
			if (!validateReferenced(s, n, gpm, strat, ReferenceAction.ALLOW)) {
				return false;
			}
		}

		long parentList=s.relationships.stream()
		               .filter(r->"SUBSTANCE->SUB_CONCEPT".equals(r.type))
		               .count();

		if(parentList>1){
			GinasProcessingMessage mes = GinasProcessingMessage
					.ERROR_MESSAGE(
							"Variant concepts may not specify more than one parent record");
			gpm.add(mes);
			strat.processMessage(mes);
		}else if(parentList>=1 && (s.substanceClass != SubstanceClass.concept)){
			GinasProcessingMessage mes = GinasProcessingMessage
					.ERROR_MESSAGE(
							"Non-concepts may not be specified as subconcepts.");
			gpm.add(mes);
			strat.processMessage(mes);
		}



		s.relationships.removeAll(remnames);
		return true;
	}

	private static boolean validateNotes(Substance s,
                                         List<GinasProcessingMessage> gpm, GsrsProcessingStrategy strat) {
		List<Note> remnames = new ArrayList<Note>();
		for (Note n : s.notes) {
			if (n == null) {
				GinasProcessingMessage mes = GinasProcessingMessage
						.WARNING_MESSAGE("Null note objects are not allowed")
						.appliableChange(true);
				gpm.add(mes);
				strat.processMessage(mes);
				if (mes.actionType == GinasProcessingMessage.ACTION_TYPE.APPLY_CHANGE) {
					remnames.add(n);
					mes.appliedChange = true;
				}
			}
			if (!validateReferenced(s, n, gpm, strat, ReferenceAction.ALLOW)) {
				return false;
			}
		}
		s.notes.removeAll(remnames);
		return true;
	}


	public static boolean isNull(GinasChemicalStructure gcs) {
		if (gcs == null || gcs.molfile == null)
			return true;
		return false;
	}

	private static List<? extends GinasProcessingMessage> validateAndPrepareNa(
            NucleicAcidSubstance cs, GsrsProcessingStrategy strat) {
		List<GinasProcessingMessage> gpm = new ArrayList<GinasProcessingMessage>();
		if (cs.nucleicAcid == null) {
			gpm.add(GinasProcessingMessage
					.ERROR_MESSAGE("Nucleic Acid substance must have a nucleicAcid element"));
		} else {
			if (cs.nucleicAcid.getSubunits() == null
					|| cs.nucleicAcid.getSubunits().isEmpty()) {
				gpm.add(GinasProcessingMessage
						.ERROR_MESSAGE("Nucleic Acid substance must have at least 1 subunit"));
			}
			if (cs.nucleicAcid.getSugars() == null
					|| cs.nucleicAcid.getSugars().isEmpty()) {
				gpm.add(GinasProcessingMessage
						.ERROR_MESSAGE("Nucleic Acid substance must have at least 1 specified sugar"));
			}
			if (cs.nucleicAcid.getLinkages() == null
					|| cs.nucleicAcid.getLinkages().isEmpty()) {
				gpm.add(GinasProcessingMessage
						.ERROR_MESSAGE("Nucleic Acid substance must have at least 1 specified linkage"));
			}


				int unspSugars = NucleicAcidUtils
						.getNumberOfUnspecifiedSugarSites(cs);
				if (unspSugars != 0) {
					gpm.add(GinasProcessingMessage
							.ERROR_MESSAGE("Nucleic Acid substance must have every base specify a sugar fragment. Missing %s sites.",
									unspSugars));
				}

				int unspLinkages = NucleicAcidUtils
						.getNumberOfUnspecifiedLinkageSites(cs);
				//This is meant to say you can't be MISSING a link between 2 sugars in an NA
				if (unspLinkages >0) {
					gpm.add(GinasProcessingMessage
							.ERROR_MESSAGE("Nucleic Acid substance must have every linkage specify a linkage fragment. Missing %s sites.",
									unspLinkages));
					//Typically you can't also have an extra link (on the 5' end), but it's allowed
				}else if(unspLinkages < 0 && unspLinkages >= -cs.nucleicAcid.subunits.size()){
					gpm.add(GinasProcessingMessage
							.INFO_MESSAGE("Nucleic Acid Substance specifies more linkages than typically expected. This is typically done to specify a 5' phosphate, but is often sometimes done by accident."));
				}else if(unspLinkages < 0){
					gpm.add(GinasProcessingMessage
							.ERROR_MESSAGE("Nucleic Acid Substance has too many linkage sites specified."));
				}

		}
		return gpm;
	}

    public static List<Substance> findFullDefinitionalDuplicateCandidates(Substance substance, DefHashCalcRequirements defHashCalcRequirements) {
        DefinitionalElements newDefinitionalElements = defHashCalcRequirements.getDefinitionalElementFactory().computeDefinitionalElementsFor(substance);
        int layer = newDefinitionalElements.getDefinitionalHashLayers().size() - 1; // hashes.size()-1;
        log.trace( "findFullDefinitionalDuplicateCandidates  handling layer: " + (layer + 1));
        return findLayerNDefinitionalDuplicateCandidates(substance, layer, defHashCalcRequirements);
    }

    public static List<Substance> findDefinitionaLayer1lDuplicateCandidates(Substance substance, DefHashCalcRequirements defHashCalcRequirements) {
        int layer = 0;
        return findLayerNDefinitionalDuplicateCandidates(substance, layer, defHashCalcRequirements);
    }

    public static List<Substance> findLayerNDefinitionalDuplicateCandidates(Substance substance, int layer, 
            DefHashCalcRequirements defHashCalcRequirements) {
        List<Substance> candidates = new ArrayList<>();
        try {
            DefinitionalElements newDefinitionalElements = defHashCalcRequirements.getDefinitionalElementFactory().computeDefinitionalElementsFor(substance);
            log.trace( "findFullDefinitionalDuplicateCandidates handling layer: " + (layer + 1) 
                    + newDefinitionalElements.getDefinitionalHashLayers().get(layer));
            String searchItem = "root_definitional_hash_layer_" + (layer + 1) + ":"
                    + newDefinitionalElements.getDefinitionalHashLayers().get(layer);
            log.trace("layer query: " + searchItem);

            TransactionTemplate transactionSearch = new TransactionTemplate(defHashCalcRequirements.getPlatformTransactionManager());
            candidates = (List<Substance>) transactionSearch.execute(ts
                    -> {
                List<String> nameValues = new ArrayList<>();
                SearchRequest request = new SearchRequest.Builder()
                        .kind(Substance.class)
                        .fdim(0)
                        .query(searchItem)
                        .top(Integer.MAX_VALUE)
                        .build();
                log.trace("built query: " + request.getQuery());
                try {
                    SearchOptions options = new SearchOptions();
                    SearchResult sr = defHashCalcRequirements.getSubstanceLegacySearchService().search(request.getQuery(), options);
                    
                    //this might not be necessary anymore
                    sr.waitForFinish();
                    
                    List<Object> fut = sr.getMatches();
                    List<Substance> hits = fut.stream()
							.filter(o -> o instanceof Substance)//added 17 February 2023 MAM to prevent ClassCastException
							.map(s ->(Substance)s)
                            .filter(ss->{
                                //filter out exact matches
                                return !substance.getOrGenerateUUID().equals(ss.getOrGenerateUUID());
                            })
                            .collect(Collectors.toList());
                    return hits;
                } catch (Exception ex) {
                    log.error("error during search. query: {}", request.getQuery());
                    ex.printStackTrace();
                }
                return nameValues;
            });
        } catch (Exception ex) {
            log.error( "Error running query", ex);
            ex.printStackTrace();
            Sneak.sneakyThrow(ex);
        }
        return candidates;
    }


	public static List<Substance> findSubstancesByCode(String codeSystem, String codeValue, PlatformTransactionManager transactionManager,
													   SubstanceLegacySearchService searchService) {
		List<Substance> candidates = new ArrayList<>();
		try {
			String codeSystemToSearch = codeSystem.replace(" ", "\\ ");
			String searchItem = "root_codes_" + codeSystemToSearch + ":\""
					+ codeValue + "\"";

			log.trace("In findSubstancesByCode, query: {}", searchItem);
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
				log.trace("built query: " + request.getQuery());
				try {
					SearchOptions options = new SearchOptions();
					SearchResult sr = searchService.search(request.getQuery(), options);

					//this might not be necessary anymore
					sr.waitForFinish();

					List<Object> fut = sr.getMatches();
					List<Substance> hits = fut.stream()
							.filter(o -> o instanceof Substance)//added 17 February 2023 MAM to prevent ClassCastException
							.filter(s->((Substance) s).codes.stream().anyMatch(c->c.code.equalsIgnoreCase(codeValue)&&c.codeSystem.equalsIgnoreCase(codeSystem)))
							.map(s ->(Substance)s)
							.collect(Collectors.toList());
					return hits;
				} catch (Exception ex) {
					log.error("error during search");
					ex.printStackTrace();
				}
				return nameValues;
			});
		} catch (Exception ex) {
			log.error( "Error running query", ex);
			ex.printStackTrace();
			Sneak.sneakyThrow(ex);
		}
		return candidates;
	}

	public static List<Substance> findSubstancesByName(String name, PlatformTransactionManager transactionManager,
													   SubstanceLegacySearchService searchService) {
		List<Substance> candidates = new ArrayList<>();
		try {
			String searchItem = "root_names_name:\""
					+ name + "\"";
			log.trace("In findSubstancesByName, query: {}", searchItem);
			TransactionTemplate transactionSearch = new TransactionTemplate(transactionManager);
			candidates = (List<Substance>) transactionSearch.execute(ts
					-> {
				SearchRequest request = new SearchRequest.Builder()
						.kind(Substance.class)
						.fdim(0)
						.query(searchItem)
						.top(Integer.MAX_VALUE)
						.build();
				log.trace("built query: " + request.getQuery());
				try {
					SearchOptions options = new SearchOptions();
					SearchResult sr = searchService.search(request.getQuery(), options);

					//this might not be necessary anymore
					sr.waitForFinish();

					List<Object> fut = sr.getMatches();
					List<Substance> nameMatches = fut.stream()
							.filter(o -> o instanceof Substance)//added 17 February 2023 MAM to prevent ClassCastException
							.map(s ->(Substance)s)
							.collect(Collectors.toList());
					if(nameMatches.size()>0) {
						List<Substance> displays = nameMatches.stream()
								.filter(s->((Substance) s).names.stream().anyMatch(n->n.name.equalsIgnoreCase(name)&&n.displayName))
								.collect(Collectors.toList());
						if(!displays.isEmpty()){
							return displays;
						}
					}
					return nameMatches;
				} catch (Exception ex) {
					log.error("error during search");
					ex.printStackTrace();
				}
				return Collections.EMPTY_LIST;
			});
		} catch (Exception ex) {
			log.error( "Error running query", ex);
			ex.printStackTrace();
			Sneak.sneakyThrow(ex);
		}
		return candidates;
	}
}
