package gsrs.module.substance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.IdHelpers;

import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.repository.GroupRepository;
import gsrs.service.AbstractGsrsEntityService;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import ix.core.validator.ValidationResponseBuilder;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.GinasProcessingStrategy;
import ix.ginas.utils.JsonSubstanceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import ix.utils.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Service
public class SubstanceEntityService extends AbstractGsrsEntityService<Substance, UUID> {
    public static final String  CONTEXT = "substances";


    public SubstanceEntityService() {
        super(CONTEXT,  IdHelpers.UUID);
    }

    @Autowired
    private SubstanceRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GroupRepository groupRepository;

//    @Autowired
//    private CvSearchService searchService;


    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }

    @Override
    public UUID parseIdFromString(String idAsString) {
        return UUID.fromString(idAsString);
    }

    private  GinasProcessingStrategy createAcceptApplyAllStrategy() {
        return new GinasProcessingStrategy(groupRepository) {
            @Override
            public void processMessage(GinasProcessingMessage gpm) {
                if (gpm.suggestedChange) {
                    gpm.actionType = GinasProcessingMessage.ACTION_TYPE.APPLY_CHANGE;
                } else {
                    if (gpm.isError()) {
                        gpm.actionType = GinasProcessingMessage.ACTION_TYPE.FAIL;
                    } else {
                        gpm.actionType = GinasProcessingMessage.ACTION_TYPE.IGNORE;
                    }
                }
            }
        };
    }

    @Override
    protected <T> ValidatorCallback createCallbackFor(T object, ValidationResponse<T> response, ValidatorConfig.METHOD_TYPE type) {
        GinasProcessingStrategy strategy = createAcceptApplyAllStrategy();
        ValidationResponseBuilder<T> builder = new ValidationResponseBuilder<T>(object, strategy){
            @Override
            public void complete() {
                if(object instanceof Substance) {
                    ValidationResponse<T> resp = buildResponse();

                    List<GinasProcessingMessage> messages = resp.getValidationMessages()
                            .stream()
                            .filter(m -> m instanceof GinasProcessingMessage)
                            .map(m -> (GinasProcessingMessage) m)
                            .collect(Collectors.toList());
                    messages.stream().forEach(strategy::processMessage);
                    if (strategy.handleMessages((Substance) object, messages)) {
                        resp.setValid(true);
                    }
                    strategy.addProblems((Substance) object, messages);


                    if (GinasProcessingMessage.ALL_VALID(messages)) {
                        resp.addValidationMessage(GinasProcessingMessage.SUCCESS_MESSAGE("Substance is valid"));
                    }
                }
            }
        };
        if(type == ValidatorConfig.METHOD_TYPE.BATCH){
            builder.allowPossibleDuplicates(true);
        }

        return builder;
    }

    @Override
    protected Substance fromNewJson(JsonNode json) throws IOException {
        return JsonSubstanceFactory.makeSubstance(json);

    }

    @Override
    public Page page(Pageable pageable) {

        return repository.findAll(pageable);
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    @Override
    protected Substance update(Substance substance) {
//        controlledVocabulary.
        //first bump version?
        substance.forceUpdate();
        return repository.saveAndFlush(substance);
    }

    @Override
    public UUID getIdFrom(Substance entity) {
        return entity.getUuid();
    }

    @Override
    protected List<Substance> fromNewJsonList(JsonNode list) throws IOException {
        List<Substance> substances = new ArrayList<>(list.size());
        for(JsonNode n : list){
            substances.add(fromNewJson(n));
        }
        return substances;
    }

    @Override
    protected Substance fromUpdatedJson(JsonNode json) throws IOException {
        //TODO should we make any edits to remove fields?
        return JsonSubstanceFactory.makeSubstance(json);
    }

    @Override
    protected List<Substance> fromUpdatedJsonList(JsonNode list) throws IOException {
        List<Substance> substances = new ArrayList<>(list.size());
        for(JsonNode n : list){
            substances.add(fromUpdatedJson(n));
        }
        return substances;
    }


    @Override
    protected JsonNode toJson(Substance controlledVocabulary) throws IOException {
        return objectMapper.valueToTree(controlledVocabulary);
    }

    @Override
    protected Substance create(Substance substance) {
        try {
            try {
                String s = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(substance);
                System.out.println("about to save substance" + s);
                System.out.println("names = " + substance.names);
            }catch(Throwable t){};
            return repository.saveAndFlush(substance);
        }catch(Throwable t){
            t.printStackTrace();
            throw t;
        }
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public Optional<Substance> get(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Substance> flexLookup(String someKindOfId) {
        if(someKindOfId==null){
            return Optional.empty();
        }
        if(Util.isUUID(someKindOfId)){
            return get(UUID.fromString(someKindOfId));
        }
        //old versions of GSRS only used the first 8 chars of the uuid
        if (someKindOfId.length() == 8) { // might be uuid
            List<Substance> list = repository.findByUuidStartingWith(someKindOfId);
            if(!list.isEmpty()){
                return Optional.of(list.get(0));
            }
        }

        Substance result = repository.findByApprovalID(someKindOfId);
        if(result !=null){
            return Optional.of(result);
        }
        List<SubstanceRepository.SubstanceSummary> summaries = repository.findByNames_NameIgnoreCase(someKindOfId);
        if(summaries !=null && !summaries.isEmpty()){

            //get the first?
            return repository.findById(summaries.get(0).getUuid());
        }
        summaries = repository.findByCodes_CodeIgnoreCase(someKindOfId);
        if(summaries !=null && !summaries.isEmpty()){

            //get the first?
            return repository.findById(summaries.get(0).getUuid());
        }
        return Optional.empty();
        //from Play version
        /*
        if (name == null) {
			return new ArrayList<Substance>();
		}
		if(UUIDUtil.isUUID(name)) {

			try {
				Substance s = finder.get().byId(UUID.fromString(name));
				if (s != null) {
					List<Substance> retlist = new ArrayList<Substance>();
					retlist.add(s);
					return retlist;
				}
			} catch (Exception e) {
			}
		}
		List<Substance> values = new ArrayList<Substance>();
		if (name.length() == 8) { // might be uuid
			values = finder.get().where().istartsWith("uuid", name).findList();
		}

		if (values.isEmpty()) {
			values = finder.get().where().ieq("approvalID", name).findList();
			if (values.isEmpty()) {
				values = finder.get().where().ieq("names.name", name).findList();
				if (values.isEmpty()) // last resort..
					values = finder.get().where().ieq("codes.code", name).findList();
			}
		}

         */
    }

    @Override
    protected Optional<UUID> flexLookupIdOnly(String someKindOfId) {
        //easiest way to avoid deduping data is to just do a full flex lookup and then return id
        Optional<Substance> found = flexLookup(someKindOfId);
        if(found.isPresent()){
            return Optional.of(found.get().uuid);
        }
        return Optional.empty();
    }


//    private SearchResult<ControlledVocabulary> parseQueryIntoMatch(String query, SearchSession session) {
//        Pattern pattern = Pattern.compile("(\\S+):(\\S+)");
//        Matcher matcher = pattern.matcher(query);
//
//        Map<String, List<String>> map = new LinkedHashMap<>();
//        while (matcher.find()) {
//            map.computeIfAbsent(matcher.group(1), k -> new ArrayList<>()).add(matcher.group(2));
//
//        }
//        if (map.isEmpty()) {
//            return session.search(ControlledVocabulary.class).where(f -> f.matchAll()).fetchAll();
//        }
//        if (map.size() == 1) {
//            Map.Entry<String, List<String>> entry = map.entrySet().iterator().next();
//            if (entry.getValue().size() == 1) {
//                //simpliest case
//                return session.search(ControlledVocabulary.class)
//                        .where(f -> f.match().field(entry.getKey())
//                                .matching(entry.getValue().get(0))
//
//                        ).fetchAll();
//            }
//
//            return session.search(ControlledVocabulary.class).where(f -> {
//                        BooleanPredicateClausesStep<?> step = f.bool();
//                        Iterator<String> values = entry.getValue().iterator();
//                        while (values.hasNext()) {
//                            step = step.should(f.match().field(entry.getKey())
//                                    .matching(values.next()));
//                        }
//
//                        return step;
//                    }
//            ).fetchAll();
//
//        }else{
//            //more complicated version probably need to make an AST
//            return null;
//        }
//
//
//
//    }

//    @Override
//    protected List<ControlledVocabulary> indexSearchV2(LuceneSearchRequestOp op, Optional<Integer> top, Optional<Integer> skip, Optional<Integer> fdim) {
//        SearchSession session = searchService.createSearchSession();
//
//        return session.search(ControlledVocabulary.class)
//                .where(f-> op.doIt(f))
//                .fetchHits(skip.orElse(null),top.orElse(null));
//
//    }

//    @Override
//    protected SearchResult indexSearchV1(SearchRequest searchRequest) throws Exception{
//
//            return getlegacyGsrsSearchService().search(searchRequest.getQuery(), searchRequest.getOptions() );
//

    //        SearchSession session = searchService.createSearchSession();
//        List<ControlledVocabulary> dslHits = parseQueryIntoMatch(query , session).hits();
//
//
//       System.out.println("dslHits = " + dslHits);
//        return dslHits;
//        String[] fields = parseFieldsFrom(query);
//        QueryParser parser;
//        if(fields.length==1){
//            parser = new QueryParser(fields[0], new KeywordAnalyzer());
//        }else{
//            parser = new MultiFieldQueryParser(fields, new KeywordAnalyzer());
//        }
//        System.out.println("parsed fields =" + Arrays.toString(fields));

//        QueryParser parser = new IxQueryParser(query);
//        List<ControlledVocabulary> hits = session.search( ControlledVocabulary.class )
//                .extension( LuceneExtension.get() )
//                .where( f -> {
//                    try {
//                        return f.fromLuceneQuery( parser.parse(query) );
//                    } catch (ParseException e) {
//                        return Sneak.sneakyThrow(new RuntimeException(e));
//                    }
//                })
//                .fetchHits(skip.orElse(null), top.orElse(null));

//        System.out.println("found # hits = " + hits.size());
//        return hits;

//    }



}
