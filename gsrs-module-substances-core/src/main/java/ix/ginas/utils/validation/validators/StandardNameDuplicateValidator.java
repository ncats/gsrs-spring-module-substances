package ix.ginas.utils.validation.validators;

import java.util.*;
import java.util.stream.Collectors;

import gsrs.cache.GsrsCache;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.models.Keyword;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.EntityUtils;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class StandardNameDuplicateValidator extends AbstractValidatorPlugin<Substance> {

    // Please adjust test class if you change these message texts.
    private final String DUPLICATE_IN_SAME_RECORD_MESSAGE = "Standard Name '%s' is a duplicate standard name in the same record.";
    private final String DUPLICATE_IN_OTHER_RECORD_MESSAGE = "Standard Name '%s' collides (possible duplicate) with existing standard name for other substance:";

    private final String DUPLICATE_IN_SAME_RECORD_MESSAGE_TEST_FRAGMENT = "is a duplicate standard name in the same record.";
    private final String DUPLICATE_IN_OTHER_RECORD_MESSAGE_TEST_FRAGMENT = "collides (possible duplicate) with existing standard name for other substance:";

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    private SubstanceLegacySearchService searchService;

    @Autowired
    private TextIndexerFactory textIndexerFactory;

    private TextIndexer indexer;


    @Autowired
    private GsrsCache cache;



    private boolean checkDuplicateInOtherRecord = true;
    private boolean checkDuplicateInSameRecord = true;
    private boolean onDuplicateInOtherRecordShowError = true;
    private boolean onDuplicateInSameRecordShowError = false;

    // User adds/edits standard name in substance record that has a duplicate standard name within
    // the substance record and in the same language.
    // ==> warning (default)

    // User adds/edits standard name in substance record that has a duplicate in another substance
    // record ==> error (default)

    public void setSubstanceRepository(SubstanceRepository substanceRepository) {
        this.substanceRepository = substanceRepository;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public void setSearchService(SubstanceLegacySearchService searchService) {
        this.searchService = searchService;
    }

    public void setTextIndexerFactory(TextIndexerFactory textIndexerFactory) {
        this.textIndexerFactory = textIndexerFactory;
    }

    public void setCache(GsrsCache cache) {
        this.cache = cache;
    }

    public void setOnDuplicateInOtherRecordShowError(boolean onDuplicateInOtherRecordShowError) {
        this.onDuplicateInOtherRecordShowError = onDuplicateInOtherRecordShowError;
    }

    public void setOnDuplicateInSameRecordShowError(boolean onDuplicateInSameRecordShowError) {
        this.onDuplicateInSameRecordShowError = onDuplicateInSameRecordShowError;
    }

    public void setCheckDuplicateInOtherRecord(boolean checkDuplicateInOtherRecord) {
        this.checkDuplicateInOtherRecord = checkDuplicateInOtherRecord;
    }

    public void setCheckDuplicateInSameRecord(boolean checkDuplicateInSameRecord) {
        this.checkDuplicateInSameRecord = checkDuplicateInSameRecord;
    }

    public String getDUPLICATE_IN_OTHER_RECORD_MESSAGE_TEST_FRAGMENT() {
        return DUPLICATE_IN_OTHER_RECORD_MESSAGE_TEST_FRAGMENT;
    }
    public String getDUPLICATE_IN_SAME_RECORD_MESSAGE_TEST_FRAGMENT() {
        return DUPLICATE_IN_SAME_RECORD_MESSAGE_TEST_FRAGMENT;
    }

    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {

        Map<String, Set<String>> stdNameSetByLanguage = new HashMap<>();

        objnew.names.forEach((Name name) -> {

            if (name.stdName != null) {
                // Include language default or trust that it is already done?
                if (name.languages == null || name.languages.isEmpty()) {
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE(
                                    "Must specify a language for each name. Defaults to \"English\"")
                            .appliableChange(true);
                    callback.addMessage(mes, () -> {
                        if (name.languages == null) {
                            name.languages = new EmbeddedKeywordList();
                        }
                        name.languages.add(new Keyword("en"));
                    });
                }

                // Check for duplicates in the record.
                if(checkDuplicateInSameRecord) {
                    Iterator<Keyword> iter = name.languages.iterator();
                    String uppercaseStdName = name.stdName.toUpperCase();
                    while (iter.hasNext()) {
                        String language = iter.next().getValue();
                        Set<String> stdNames = stdNameSetByLanguage.computeIfAbsent(language, k -> new HashSet<>());
                        if (!stdNames.add(uppercaseStdName)) {
                            if (onDuplicateInSameRecordShowError) {
                                System.out.println("=== IF 1a ===");
                                GinasProcessingMessage mes = GinasProcessingMessage.ERROR_MESSAGE(String.format(DUPLICATE_IN_SAME_RECORD_MESSAGE, name.stdName));
                                mes.markPossibleDuplicate();
                                callback.addMessage(mes);
                            } else {
                                System.out.println("=== IF 2a ===");
                                GinasProcessingMessage mes = GinasProcessingMessage.WARNING_MESSAGE(String.format(DUPLICATE_IN_SAME_RECORD_MESSAGE, name.stdName));
                                mes.markPossibleDuplicate();
                                callback.addMessage(mes);
                            }

                        }
                    }
                }
                if(checkDuplicateInOtherRecord) {
                    Substance otherSubstance = checkStdNameForDuplicateInOtherRecordsViaIndexer(objnew, name.stdName);
                    if (otherSubstance != null) {
                        if (onDuplicateInOtherRecordShowError) {
                            System.out.println("=== IF 2b ===");
                            GinasProcessingMessage mes = GinasProcessingMessage.ERROR_MESSAGE(String.format(DUPLICATE_IN_OTHER_RECORD_MESSAGE, name.stdName));
                            mes.addLink(ValidationUtils.createSubstanceLink(SubstanceReference.newReferenceFor(otherSubstance)));
                            callback.addMessage(mes);
                        } else {
                            System.out.println("=== IF 2b ===");
                            GinasProcessingMessage mes = GinasProcessingMessage.WARNING_MESSAGE(String.format(DUPLICATE_IN_OTHER_RECORD_MESSAGE, name.stdName));
                            mes.addLink(ValidationUtils.createSubstanceLink(SubstanceReference.newReferenceFor(otherSubstance)));
                            callback.addMessage(mes);
                        }
                    }
                }
            } // if stdName not null

        });
    }

    public Substance checkStdNameForDuplicateInOtherRecordsViaIndexer(Substance s, String stdName) {
        List<Substance> substances = findIndexedSubstancesByStdName(stdName);
        try {
            if (substances!=null && !substances.isEmpty()) {
                Substance s2 = null;
                Iterator<Substance> it = substances.iterator();
                while (it.hasNext()) {
                    s2 = it.next();
                    if (!s2.getUuid().equals(s.getOrGenerateUUID())) {
                        return s2;
                    }
                }
            }
        } catch (Exception e) {
            // Should we be throwing an error?
            System.out.println("Problem checking for duplicate standard name.");
            e.printStackTrace();
            log.warn("Problem checking for duplicate standard name.", e);
        }

        return null;
    }

    public List<Substance> findIndexedSubstancesByStdName(String stdName) {
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                .query("root_names_stdName:\"" + stdName + "\"")
                .top(Integer.MAX_VALUE)
                .build();
        List<Substance> substances = getSearchList(request);
        return substances;
    }

    /**
     * Return a list of substances based on the {@link SearchRequest}. This
     * takes care of some tricky transaction issues.
     *
     * @param sr
     * @return
     */

    private List<Substance> getSearchList(SearchRequest sr) {
        TransactionTemplate transactionSearch = new TransactionTemplate(transactionManager);
        List<Substance> substances = transactionSearch.execute(ts -> {
            try {
                SearchResult sresult = searchService.search(sr.getQuery(), sr.getOptions());
                List<Substance> first = sresult.getMatches();
                return first.stream()
                        //force fetching
                        .peek(ss -> EntityUtils.EntityWrapper.of(ss).toInternalJson())
                        .collect(Collectors.toList());
            } catch (Exception e) {
                throw new RuntimeException(e);

            }
        });
        return substances;
    }


    // Don't use this
    public SubstanceRepository.SubstanceSummary checkStdNameForDuplicateInOtherRecords(Substance s, String stdName) {
        try {
            List<SubstanceRepository.SubstanceSummary> sr = substanceRepository.findByNames_StdNameIgnoreCase(stdName);
            if (sr!=null && !sr.isEmpty()) {
                SubstanceRepository.SubstanceSummary s2 = null;
                Iterator<SubstanceRepository.SubstanceSummary> it = sr.iterator();
                while(it.hasNext()){
                    s2 = it.next();
                    if (!s2.getUuid().equals(s.getOrGenerateUUID())) {
                        return s2;
                    }
                }
            }
        } catch (Exception e) {
            // Should we be throwing an error?
            System.out.println("Problem checking for duplicate standard name.");
            e.printStackTrace();
            log.warn("Problem checking for duplicate standard name.", e);
        }
        return null;
    }

}
