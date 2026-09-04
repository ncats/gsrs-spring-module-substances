package gsrs.module.substance.services;

import com.fasterxml.jackson.annotation.JsonValue;
import gov.nih.ncats.common.stream.StreamUtil;
import gov.nih.ncats.molwitch.Atom;
import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.molwitch.io.ChemFormat;
import gov.nih.ncats.molwitch.search.MolSearcher;
import gov.nih.ncats.molwitch.search.MolSearcherFactory;
import gov.nih.ncats.structureIndexer.StructureIndexer;
import gsrs.DefaultDataSourceConfig;
import gsrs.cache.GsrsCache;
import gsrs.legacy.structureIndexer.LegacyStructureIndexerService;
import gsrs.legacy.structureIndexer.StandardizedStructureIndexer;
import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.repository.MixtureSubstanceRepository;
import gsrs.module.substance.repository.ModificationRepository;
import gsrs.module.substance.repository.StructureRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.utils.SanitizerUtil;
import gsrs.springUtils.AutowireHelper;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.search.SearchResult;
import ix.core.search.SearchResultContext;
import ix.core.search.SearchResultProcessor;
import ix.core.search.text.IndexerService;
import ix.core.search.text.IndexerServiceFactory;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.*;
import ix.seqaln.SequenceIndexer;
import ix.utils.UUIDUtil;
import ix.utils.Util;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
@Slf4j
public class SubstanceStructureSearchService {
    private static final int MAX_V2000_ATOM_LIST_EXPANSIONS = 64;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchRequest{

        private String q;

        private Integer top;

        private Integer skip;

        private Integer fdim;

        private Double cutoff;

        private StructureSearchType type;

        private String order;

        private String field;
        /*

        String q =      getLastStringOrElse(params.get("q"), null);
        String type =   getLastStringOrElse(params.get("type"), "sub");
        Double co =     getLastDoubleOrElse(params.get("cutoff"), 0.8);
        Integer top =   getLastIntegerOrElse(params.get("top"), 10);
        Integer skip=   getLastIntegerOrElse(params.get("skip"), 0);
        Integer fdim =  getLastIntegerOrElse(params.get("fdim"), 10);
        String field =  getLastStringOrElse(params.get("field"), "");
         */

        public SanitizedSearchRequest sanitize(){
            return new SanitizedSearchRequest(this);
        }
    }

    //TODO right now the key/signature hash key is computed inside the controller
    //maybe we should move it to here but it requires the http request to get facet info
    public enum StructureSearchType{

        SUBSTRUCTURE("sub"),
        SIMILARITY("sim"),
        FLEX("flex"),
        FLEX_PLUS("flexplus"),
        EXACT("exact"),
        EXACT_PLUS("exactplus")
        ;

        private final String value;

        private static Map<String, StructureSearchType> lookup = new ConcurrentHashMap<>();
        static{
            for(StructureSearchType t : values()){
                lookup.put(t.value, t);
            }
        }
        private StructureSearchType(String value){
            this.value = value;
        }

        public static StructureSearchType parseType(String type) {
            if(type ==null){
                return null;
            }
            return lookup.computeIfAbsent(type, t->{
                for(StructureSearchType s : values()){
                    if(type.equalsIgnoreCase(s.value)){
                        return s;
                    }
                }
                for(StructureSearchType s : values()){
                    if(type.toLowerCase().startsWith(s.value)){
                        return s;
                    }
                }
                return null;
            });

        }

        @JsonValue
        public String getValue(){
            return value;
        }


    }
    @Getter
    @EqualsAndHashCode
    public static class SanitizedSearchRequest{
        private static final int DEFAULT_TOP =10;
        private static final int DEFAULT_FDIM =10;
        private static final double DEFAULT_CUTOFF = 0.8D;
        private static final StructureSearchType DEFAULT_TYPE = StructureSearchType.SUBSTRUCTURE;

        private static final String DEFAULT_FIELD= "";
        private String queryStructure;

        private int top;

        private int skip;

        private int fdim;
        private String field;
        private double cutoff;
        private String order;
        private StructureSearchType type;

        private SanitizedSearchRequest(SearchRequest request){
            this.top = SanitizerUtil.sanitizeNumber(request.top, DEFAULT_TOP);
            this.skip = SanitizerUtil.sanitizeNumber(request.skip, 0);
            this.fdim = SanitizerUtil.sanitizeNumber(request.fdim, DEFAULT_FDIM);
            this.cutoff = SanitizerUtil.sanitizeCutOff(request.cutoff, DEFAULT_CUTOFF);
            this.type = request.type ==null? StructureSearchType.SUBSTRUCTURE: request.getType();
            this.queryStructure = request.q ==null? null: request.q;//don't trim it breaks mol format!
            this.order = request.order;
            this.field = request.field ==null? DEFAULT_FIELD: request.field;
        }
        
        public boolean isHashSearch() {
            if(type==null)return false;
            return this.type.equals(StructureSearchType.EXACT) || this.type.equals(StructureSearchType.FLEX)
                    || this.type.equals(StructureSearchType.FLEX_PLUS)
                    || this.type.equals(StructureSearchType.EXACT_PLUS);
        }

        public static String getDefaultField() {
            return DEFAULT_FIELD;
        }

        public static int getDefaultTop() {
            return DEFAULT_TOP;
        }

        public static int getDefaultFdim() {
            return DEFAULT_FDIM;
        }

        public static double getDefaultCutoff() {
            return DEFAULT_CUTOFF;
        }

        public static StructureSearchType getDefaultType() {
            return DEFAULT_TYPE;
        }

        public Map<String,Object> getParameterMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("top", top);
            map.put("skip", skip);
            map.put("fdim", fdim);
            map.put("type", type.value);
            if(order !=null) {
                map.put("order", order);
            }
            if(!field.trim().isEmpty()){
                map.put("field", field.trim());
            }
            map.put("cutoff", cutoff);
            return map;
        }
    }
    @Autowired
    private StructureIndexerService structureIndexerService;
    @Autowired
    private StructureSearchConfiguration structureSearchConfiguration;
    @Autowired
    private ModificationRepository modificationRepository;
    @Autowired
    private SubstanceRepository substanceRepository;
    @Autowired
    private MixtureSubstanceRepository mixtureSubstanceRepository;
    @Autowired
    private GsrsCache gsrsCache;
    @Autowired
    private StructureProcessor structureProcessor;
    @Autowired
    private SubstanceLegacySearchService legacySearchService;
    
    @PersistenceContext(unitName =  DefaultDataSourceConfig.NAME_ENTITY_MANAGER)
//    @Autowired
    private EntityManager entityManager;

    public SearchResultContext search(SanitizedSearchRequest request, String hashKey) throws Exception {

        return gsrsCache.getOrElse(structureIndexerService.lastModified(), hashKey, ()->{
            SearchResultProcessor<StructureIndexer.Result, Substance> processor = new StructureSearchResultProcessor(
                    structureSearchConfiguration,
                    modificationRepository,
                    substanceRepository,
                    mixtureSubstanceRepository,
                    gsrsCache,
                    entityManager);

            processor = AutowireHelper.getInstance().autowireAndProxy(processor);
            Enumeration<StructureIndexer.Result> resultEnumeration=null;
            if(request.getType() == StructureSearchType.SUBSTRUCTURE) {
                resultEnumeration = substructure(request.getQueryStructure());
            }else if(request.getType() == StructureSearchType.SIMILARITY){
                resultEnumeration = structureIndexerService.similarity(request.getQueryStructure(), request.cutoff);
            }

            if(resultEnumeration ==null){
                throw new Exception("invalid request type "+ request.getType());
            }
            SearchResultContext ctx = processor.getContext();
            if(request.getType() == StructureSearchType.SUBSTRUCTURE) {
                if(!resultEnumeration.hasMoreElements()) {
                    completeWithExactStructureFallback(ctx, request.getQueryStructure());
                } else {
                    processor.setResults(1, resultEnumeration);
                    addExactStructureFallbackResultsIfEmpty(ctx, request.getQueryStructure());
                }
            } else {
                processor.setResults(1, resultEnumeration);
            }
            ctx.setKey(hashKey);

            return ctx;

        });





    }

    private Enumeration<StructureIndexer.Result> substructure(String queryStructure) throws Exception {
        Chemical query = Chemical.parse(queryStructure);
        List<QueryBondState> queryBonds = queryBondStatesFromV2000(queryStructure);
        List<V2000AtomListState> atomLists = atomListStatesFromV2000(queryStructure);
        boolean hasV2000AtomList = !atomLists.isEmpty();
        boolean needsQueryAtomPostFilter = query.hasQueryAtoms()
                || query.hasPseudoAtoms()
                || hasSmartsQueryAtomExpression(queryStructure)
                || hasV2000AtomList;
        if(queryBonds.isEmpty() && !hasV2000AtomList && !StructureProcessor.hasQueryBonds(query)) {
            return filterQueryAtomSubstructureResults(
                    structureIndexerService.substructure(queryStructure),
                    queryStructure,
                    query,
                    needsQueryAtomPostFilter);
        }

        Optional<StructureIndexer> rawIndexer = getRawStructureIndexerDelegate();
        if(rawIndexer.isPresent()) {
            if(!atomLists.isEmpty()) {
                List<Chemical> concreteQueries = prepareV2000AtomListSubstructureQueries(
                        queryStructure,
                        atomLists,
                        queryBonds);
                if(!concreteQueries.isEmpty()) {
                    List<Enumeration<StructureIndexer.Result>> resultEnumerations = new ArrayList<>();
                    for(Chemical concreteQuery : concreteQueries) {
                        resultEnumerations.add(rawIndexer.get().substructure(concreteQuery));
                    }
                    return deduplicateSubstructureResults(resultEnumerations);
                }
            }
            if(!queryBonds.isEmpty()) {
                query = prepareQueryBondSubstructureQuery(queryStructure, query, queryBonds);
            }
            return rawIndexer.get().substructure(query);
        }

        log.debug("Unable to locate raw structure indexer delegate; using standard substructure path for query-bond search");
        return filterQueryAtomSubstructureResults(
                structureIndexerService.substructure(queryStructure),
                queryStructure,
                query,
                needsQueryAtomPostFilter);
    }

    private Enumeration<StructureIndexer.Result> filterQueryAtomSubstructureResults(
            Enumeration<StructureIndexer.Result> results,
            String queryStructure,
            Chemical query,
            boolean needsQueryAtomPostFilter) {
        if(!needsQueryAtomPostFilter) {
            return results;
        }

        Optional<MolSearcher> searcher = createQueryAtomMolSearcher(queryStructure, query);
        if(!searcher.isPresent()) {
            return results;
        }

        MolSearcher finalSearcher = searcher.get();
        Iterator<StructureIndexer.Result> filteredIterator = StreamUtil.forEnumeration(results)
                .filter(result -> {
                    try {
                        Optional<int[]> hit = finalSearcher.search(result.getMol());
                        return hit.isPresent() && hit.get().length > 0;
                    } catch (Exception e) {
                        log.debug("Unable to post-filter query-atom substructure result", e);
                        return false;
                    }
                })
                .iterator();

        return new Enumeration<StructureIndexer.Result>() {
            @Override
            public boolean hasMoreElements() {
                return filteredIterator.hasNext();
            }

            @Override
            public StructureIndexer.Result nextElement() {
                return filteredIterator.next();
            }
        };
    }

    private Optional<MolSearcher> createQueryAtomMolSearcher(String queryStructure, Chemical query) {
        if(hasSmartsQueryAtomExpression(queryStructure)) {
            try {
                Optional<MolSearcher> searcher = MolSearcherFactory.create(queryStructure);
                if(searcher.isPresent()) {
                    return searcher;
                }
            } catch (RuntimeException e) {
                log.debug("Unable to create SMARTS query-atom searcher from query string", e);
            }
        }

        try {
            return MolSearcherFactory.create(query);
        } catch (RuntimeException e) {
            log.debug("Unable to create query-atom searcher from parsed query structure", e);
            return Optional.empty();
        }
    }

    private boolean hasSmartsQueryAtomExpression(String queryStructure) {
        if(queryStructure == null) {
            return false;
        }

        int start = -1;
        while((start = queryStructure.indexOf('[', start + 1)) >= 0) {
            int end = queryStructure.indexOf(']', start + 1);
            if(end < 0) {
                return false;
            }

            String atomExpression = queryStructure.substring(start + 1, end);
            if(atomExpression.indexOf(',') >= 0
                    || atomExpression.indexOf(';') >= 0
                    || atomExpression.indexOf('!') >= 0
                    || atomExpression.indexOf('#') >= 0
                    || atomExpression.indexOf('$') >= 0
                    || atomExpression.indexOf('*') >= 0) {
                return true;
            }
        }

        return false;
    }

    private Chemical prepareQueryBondSubstructureQuery(String queryStructure, Chemical query, List<QueryBondState> queryBonds) {
        if(query.hasQueryAtoms() || query.hasPseudoAtoms()) {
            return query;
        }

        if(queryBonds.isEmpty()) {
            return query;
        }

        try {
            Optional<String> concreteQueryStructure = concretizeV2000QueryBondTypes(queryStructure, queryBonds);
            if(!concreteQueryStructure.isPresent()) {
                return query;
            }

            Chemical standardizedConcreteQuery = StandardizedStructureIndexer.getSSSStandardized(
                    Chemical.parse(concreteQueryStructure.get()));
            String standardizedMolfile = standardizedConcreteQuery.toMol(
                    new ChemFormat.MolFormatSpecification()
                            .setKekulization(ChemFormat.KekulizationEncoding.FORCE_AROMATIC));
            Optional<String> standardizedQueryStructure = restoreV2000QueryBondTypes(standardizedMolfile, queryBonds);
            if(standardizedQueryStructure.isPresent()) {
                return Chemical.parse(standardizedQueryStructure.get());
            }
        } catch (Exception e) {
            log.debug("Unable to standardize query-bond substructure query", e);
        }

        return query;
    }

    private List<Chemical> prepareV2000AtomListSubstructureQueries(
            String queryStructure,
            List<V2000AtomListState> atomLists,
            List<QueryBondState> queryBonds) {
        Optional<List<String>> concreteStructures = concretizeV2000AtomLists(queryStructure, atomLists);
        if(!concreteStructures.isPresent()) {
            return Collections.emptyList();
        }

        List<Chemical> concreteQueries = new ArrayList<>();
        for(String concreteStructure : concreteStructures.get()) {
            try {
                Chemical concreteQuery = Chemical.parse(concreteStructure);
                if(!queryBonds.isEmpty()) {
                    concreteQuery = prepareQueryBondSubstructureQuery(concreteStructure, concreteQuery, queryBonds);
                }
                concreteQueries.add(concreteQuery);
            } catch (Exception e) {
                log.debug("Unable to prepare V2000 atom-list substructure query expansion", e);
                return Collections.emptyList();
            }
        }

        return concreteQueries;
    }

    private Optional<List<String>> concretizeV2000AtomLists(String queryStructure, List<V2000AtomListState> atomLists) {
        if(atomLists.isEmpty()) {
            return Optional.empty();
        }

        int expansionCount = 1;
        for(V2000AtomListState atomList : atomLists) {
            if(!atomList.isExpandable()) {
                return Optional.empty();
            }
            expansionCount *= atomList.symbols.size();
            if(expansionCount > MAX_V2000_ATOM_LIST_EXPANSIONS) {
                log.debug("Skipping V2000 atom-list expansion because it would create {} concrete queries", expansionCount);
                return Optional.empty();
            }
        }

        String[] lines = queryStructure.split("\\R", -1);
        List<String> structures = new ArrayList<>();
        buildV2000AtomListExpansions(lines, atomLists, 0, structures);
        return structures.isEmpty() ? Optional.empty() : Optional.of(structures);
    }

    private void buildV2000AtomListExpansions(
            String[] sourceLines,
            List<V2000AtomListState> atomLists,
            int atomListIndex,
            List<String> structures) {
        if(atomListIndex == atomLists.size()) {
            Set<Integer> atomListLineIndexes = atomLists.stream()
                    .map(atomList -> atomList.lineIndex)
                    .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
            List<String> concreteLines = new ArrayList<>();
            for(int i = 0; i < sourceLines.length; i++) {
                if(!atomListLineIndexes.contains(i)) {
                    concreteLines.add(sourceLines[i]);
                }
            }
            structures.add(String.join("\n", concreteLines));
            return;
        }

        V2000AtomListState atomList = atomLists.get(atomListIndex);
        int atomLineIndex = 4 + atomList.atomIndex - 1;
        if(atomLineIndex < 0 || atomLineIndex >= sourceLines.length) {
            return;
        }

        String originalAtomLine = sourceLines[atomLineIndex];
        for(String symbol : atomList.symbols) {
            Optional<String> replacement = replaceV2000AtomSymbol(originalAtomLine, symbol);
            if(!replacement.isPresent()) {
                continue;
            }

            sourceLines[atomLineIndex] = replacement.get();
            buildV2000AtomListExpansions(sourceLines, atomLists, atomListIndex + 1, structures);
        }
        sourceLines[atomLineIndex] = originalAtomLine;
    }

    private Optional<String> replaceV2000AtomSymbol(String atomLine, String symbol) {
        if(atomLine.length() < 34 || symbol == null || symbol.isEmpty() || symbol.length() > 3) {
            return Optional.empty();
        }

        return Optional.of(atomLine.substring(0, 31)
                + String.format(Locale.ROOT, "%-3s", symbol)
                + atomLine.substring(34));
    }

    private Enumeration<StructureIndexer.Result> deduplicateSubstructureResults(
            List<Enumeration<StructureIndexer.Result>> resultEnumerations) {
        Iterator<Enumeration<StructureIndexer.Result>> sourceIterator = resultEnumerations.iterator();
        Set<String> seenIds = new LinkedHashSet<>();

        return new Enumeration<StructureIndexer.Result>() {
            private Enumeration<StructureIndexer.Result> current = Collections.emptyEnumeration();
            private StructureIndexer.Result next;

            @Override
            public boolean hasMoreElements() {
                if(next != null) {
                    return true;
                }

                while(true) {
                    while(!current.hasMoreElements()) {
                        if(!sourceIterator.hasNext()) {
                            return false;
                        }
                        current = sourceIterator.next();
                    }

                    StructureIndexer.Result candidate = current.nextElement();
                    if(seenIds.add(candidate.getId())) {
                        next = candidate;
                        return true;
                    }
                }
            }

            @Override
            public StructureIndexer.Result nextElement() {
                if(!hasMoreElements()) {
                    throw new NoSuchElementException();
                }

                StructureIndexer.Result result = next;
                next = null;
                return result;
            }
        };
    }

    private List<V2000AtomListState> atomListStatesFromV2000(String queryStructure) {
        if(queryStructure == null) {
            return Collections.emptyList();
        }

        String[] lines = queryStructure.split("\\R", -1);
        if(lines.length < 4 || !lines[3].contains("V2000")) {
            return Collections.emptyList();
        }

        OptionalInt atomCount = parseV2000Count(lines[3], 0, 3, 0);
        OptionalInt bondCount = parseV2000Count(lines[3], 3, 6, 1);
        if(!atomCount.isPresent() || !bondCount.isPresent()) {
            return Collections.emptyList();
        }

        int firstPropertyLine = 4 + atomCount.getAsInt() + bondCount.getAsInt();
        if(firstPropertyLine >= lines.length) {
            return Collections.emptyList();
        }

        List<V2000AtomListState> atomLists = new ArrayList<>();
        for(int i = firstPropertyLine; i < lines.length; i++) {
            parseV2000AtomListLine(i, lines[i], atomCount.getAsInt())
                    .ifPresent(atomLists::add);
        }
        return atomLists;
    }

    private Optional<V2000AtomListState> parseV2000AtomListLine(int lineIndex, String line, int atomCount) {
        String[] tokens = line.trim().split("\\s+");
        if(tokens.length < 6 || !"M".equals(tokens[0]) || !"ALS".equals(tokens[1])) {
            return Optional.empty();
        }

        try {
            int atomIndex = Integer.parseInt(tokens[2]);
            int symbolCount = Integer.parseInt(tokens[3]);
            if(atomIndex < 1 || atomIndex > atomCount || symbolCount < 1 || tokens.length < 5 + symbolCount) {
                return Optional.empty();
            }

            boolean notList = "T".equalsIgnoreCase(tokens[4]);
            Set<String> symbols = new LinkedHashSet<>();
            for(int i = 0; i < symbolCount; i++) {
                symbols.add(tokens[5 + i]);
            }

            return Optional.of(new V2000AtomListState(lineIndex, atomIndex, notList, new ArrayList<>(symbols)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private List<QueryBondState> queryBondStatesFromV2000(String queryStructure) {
        String[] lines = queryStructure.split("\\R", -1);
        if(lines.length < 4 || !lines[3].contains("V2000")) {
            return Collections.emptyList();
        }

        OptionalInt atomCount = parseV2000Count(lines[3], 0, 3, 0);
        OptionalInt bondCount = parseV2000Count(lines[3], 3, 6, 1);
        if(!atomCount.isPresent() || !bondCount.isPresent()) {
            return Collections.emptyList();
        }

        int firstBondLine = 4 + atomCount.getAsInt();
        if(firstBondLine + bondCount.getAsInt() > lines.length) {
            return Collections.emptyList();
        }

        List<QueryBondState> queryBonds = new ArrayList<>();
        for(int i = 0; i < bondCount.getAsInt(); i++) {
            int lineIndex = firstBondLine + i;
            if(lines[lineIndex].length() < 9) {
                continue;
            }
            OptionalInt atom1 = parseV2000Count(lines[lineIndex], 0, 3, 0);
            OptionalInt atom2 = parseV2000Count(lines[lineIndex], 3, 6, 1);
            OptionalInt bondType = parseV2000Count(lines[lineIndex], 6, 9, 2);
            if(atom1.isPresent() && atom2.isPresent() && bondType.isPresent() && isV2000QueryBondType(bondType.getAsInt())) {
                queryBonds.add(new QueryBondState(i, atom1.getAsInt(), atom2.getAsInt(), lines[lineIndex].substring(6, 9)));
            }
        }

        return queryBonds;
    }

    private boolean isV2000QueryBondType(int bondType) {
        return bondType >= 4 && bondType <= 8;
    }

    private Optional<String> concretizeV2000QueryBondTypes(String queryStructure, List<QueryBondState> queryBonds) {
        String[] lines = queryStructure.split("\\R", -1);
        if(lines.length < 4 || !lines[3].contains("V2000")) {
            return Optional.empty();
        }

        OptionalInt atomCount = parseV2000Count(lines[3], 0, 3, 0);
        OptionalInt bondCount = parseV2000Count(lines[3], 3, 6, 1);
        if(!atomCount.isPresent() || !bondCount.isPresent()) {
            return Optional.empty();
        }

        int firstBondLine = 4 + atomCount.getAsInt();
        if(firstBondLine + bondCount.getAsInt() > lines.length) {
            return Optional.empty();
        }

        boolean updated = false;
        for(QueryBondState queryBond : queryBonds) {
            OptionalInt lineIndex = findV2000BondLine(lines, firstBondLine, bondCount.getAsInt(), queryBond);
            if(lineIndex.isPresent() && lines[lineIndex.getAsInt()].length() >= 9) {
                lines[lineIndex.getAsInt()] = replaceV2000BondType(lines[lineIndex.getAsInt()], "  1");
                updated = true;
            }
        }

        return updated ? Optional.of(String.join("\n", lines)) : Optional.empty();
    }

    private Optional<String> restoreV2000QueryBondTypes(String molfile, List<QueryBondState> queryBonds) {
        String[] lines = molfile.split("\\R", -1);
        if(lines.length < 4 || !lines[3].contains("V2000")) {
            return Optional.empty();
        }

        OptionalInt atomCount = parseV2000Count(lines[3], 0, 3, 0);
        OptionalInt bondCount = parseV2000Count(lines[3], 3, 6, 1);
        if(!atomCount.isPresent() || !bondCount.isPresent()) {
            return Optional.empty();
        }

        int firstBondLine = 4 + atomCount.getAsInt();
        if(firstBondLine + bondCount.getAsInt() > lines.length) {
            return Optional.empty();
        }

        boolean restored = false;
        for(QueryBondState queryBond : queryBonds) {
            OptionalInt lineIndex = findV2000BondLine(lines, firstBondLine, bondCount.getAsInt(), queryBond);
            if(queryBond.v2000BondType != null && lineIndex.isPresent() && lines[lineIndex.getAsInt()].length() >= 9) {
                lines[lineIndex.getAsInt()] = replaceV2000BondType(lines[lineIndex.getAsInt()], queryBond.v2000BondType);
                restored = true;
            }
        }

        return restored ? Optional.of(String.join("\n", lines)) : Optional.empty();
    }

    private OptionalInt findV2000BondLine(String[] lines, int firstBondLine, int bondCount, QueryBondState queryBond) {
        for(int i = 0; i < bondCount; i++) {
            int lineIndex = firstBondLine + i;
            if(lines[lineIndex].length() < 6) {
                continue;
            }
            OptionalInt atom1 = parseV2000Count(lines[lineIndex], 0, 3, 0);
            OptionalInt atom2 = parseV2000Count(lines[lineIndex], 3, 6, 1);
            if(atom1.isPresent() && atom2.isPresent() && queryBond.matches(atom1.getAsInt(), atom2.getAsInt())) {
                return OptionalInt.of(lineIndex);
            }
        }

        int lineIndex = firstBondLine + queryBond.bondIndex;
        return queryBond.bondIndex < bondCount && lineIndex < lines.length
                ? OptionalInt.of(lineIndex)
                : OptionalInt.empty();
    }

    private String replaceV2000BondType(String bondLine, String bondType) {
        return bondLine.substring(0, 6) + bondType + bondLine.substring(9);
    }

    private OptionalInt parseV2000Count(String countsLine, int start, int end, int tokenIndex) {
        if(countsLine.length() >= end) {
            try {
                return OptionalInt.of(Integer.parseInt(countsLine.substring(start, end).trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        String[] tokens = countsLine.trim().split("\\s+");
        if(tokens.length > tokenIndex) {
            try {
                return OptionalInt.of(Integer.parseInt(tokens[tokenIndex]));
            } catch (NumberFormatException ignored) {
            }
        }

        return OptionalInt.empty();
    }

    private static class V2000AtomListState {
        private final int lineIndex;
        private final int atomIndex;
        private final boolean notList;
        private final List<String> symbols;

        private V2000AtomListState(int lineIndex, int atomIndex, boolean notList, List<String> symbols) {
            this.lineIndex = lineIndex;
            this.atomIndex = atomIndex;
            this.notList = notList;
            this.symbols = symbols;
        }

        private boolean isExpandable() {
            return !notList && !symbols.isEmpty();
        }
    }

    private static class QueryBondState {
        private final int bondIndex;
        private final int atom1Index;
        private final int atom2Index;
        private final String v2000BondType;

        private QueryBondState(int bondIndex, int atom1Index, int atom2Index, String v2000BondType) {
            this.bondIndex = bondIndex;
            this.atom1Index = atom1Index;
            this.atom2Index = atom2Index;
            this.v2000BondType = v2000BondType;
        }

        private boolean matches(int atom1, int atom2) {
            return (atom1Index == atom1 && atom2Index == atom2)
                    || (atom1Index == atom2 && atom2Index == atom1);
        }
    }

    private Optional<StructureIndexer> getRawStructureIndexerDelegate() {
        if(!(structureIndexerService instanceof LegacyStructureIndexerService)) {
            return Optional.empty();
        }

        try {
            Field indexerField = LegacyStructureIndexerService.class.getDeclaredField("indexer");
            indexerField.setAccessible(true);
            Object indexer = indexerField.get(structureIndexerService);
            if(indexer instanceof StandardizedStructureIndexer) {
                return Optional.of(((StandardizedStructureIndexer) indexer).getDelegate());
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            log.debug("Unable to access legacy structure indexer delegate", e);
        }

        return Optional.empty();
    }

    private void completeWithExactStructureFallback(SearchResultContext ctx, String queryStructure) {
        long now = System.currentTimeMillis();
        ctx.setStart(now);

        addExactStructureFallbackResultsIfEmpty(ctx, queryStructure);

        ctx.setTotal(ctx.getCount());
        ctx.setStatus(SearchResultContext.Status.Done);
        ctx.setStop(System.currentTimeMillis());
    }

    private void addExactStructureFallbackResultsIfEmpty(SearchResultContext ctx, String queryStructure) {
        if(ctx.getCount() > 0) {
            return;
        }

        exactHashForPlainChemicalQuery(queryStructure)
                .ifPresent(exactHash -> addExactStructureFallbackResults(ctx, exactHash));
    }

    private Optional<String> exactHashForPlainChemicalQuery(String queryStructure) {
        if(queryStructure == null || queryStructure.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            Chemical query = Chemical.parse(queryStructure);
            if(StructureProcessor.hasQueryFeatures(query)) {
                return Optional.empty();
            }

            Structure structure = structureProcessor.instrument(queryStructure);
            return Optional.ofNullable(structure.getExactHash())
                    .filter(hash -> !hash.trim().isEmpty());
        } catch (Exception e) {
            log.debug("Unable to compute exact structure fallback hash", e);
            return Optional.empty();
        }
    }

    private void addExactStructureFallbackResults(SearchResultContext ctx, String exactHash) {
        try {
            ix.core.search.SearchRequest exactSearchRequest = new ix.core.search.SearchRequest.Builder()
                    .kind(Substance.class)
                    .query("root_structure_properties_EXACT_HASH:" + exactHash)
                    .build();
            SearchResult exactSearchResult = legacySearchService.search(
                    exactSearchRequest.getQuery(),
                    exactSearchRequest.getOptions());
            exactSearchResult.waitForFinish();

            Set<UUID> seen = new LinkedHashSet<>();
            for(Object match : exactSearchResult.getMatches()) {
                if(match instanceof Substance) {
                    addIfNotSeen(ctx, seen, (Substance) match);
                }
            }
        } catch (Exception e) {
            log.debug("Unable to run exact structure fallback search", e);
        }
    }

    private void addIfNotSeen(SearchResultContext ctx, Set<UUID> seen, Substance substance) {
        if(substance.uuid == null || seen.add(substance.uuid)) {
            ctx.add(substance);
        }
    }

    @Slf4j
    public static class StructureSearchResultProcessor
            extends SearchResultProcessor<StructureIndexer.Result, Substance> {
        int index;
        public static EntityUtils.EntityInfo<ChemicalSubstance> chemMeta = EntityUtils.getEntityInfoFor(ChemicalSubstance.class);
        public static EntityUtils.EntityInfo<MixtureSubstance> mixMeta = EntityUtils.getEntityInfoFor(MixtureSubstance.class);
        public static EntityUtils.EntityInfo<Substance> subMeta = EntityUtils.getEntityInfoFor(Substance.class);
        public static EntityUtils.EntityInfo<Modifications> modMeta = EntityUtils.getEntityInfoFor(Modifications.class);

        private StructureSearchConfiguration structureSearchConfiguration;
        private ModificationRepository modificationRepository;
        private SubstanceRepository substanceRepository;
        private MixtureSubstanceRepository mixtureSubstanceRepository;
        private GsrsCache gsrsCache;
        private EntityManager entityManager;

        public StructureSearchResultProcessor(StructureSearchConfiguration structureSearchConfiguration,
                                              ModificationRepository modificationRepository,
                                               SubstanceRepository substanceRepository,
                                              MixtureSubstanceRepository mixtureSubstanceRepository,
                                              GsrsCache gsrsCache,
                                              EntityManager entityManager
                                              ) {
            this.structureSearchConfiguration  = Objects.requireNonNull(structureSearchConfiguration);
            this.modificationRepository = Objects.requireNonNull(modificationRepository);
            this.substanceRepository = Objects.requireNonNull(substanceRepository);
            this.mixtureSubstanceRepository = Objects.requireNonNull(mixtureSubstanceRepository);
            this.gsrsCache = Objects.requireNonNull(gsrsCache);
            this.entityManager = Objects.requireNonNull(entityManager);
        }

        @Override
        public Stream<? extends Substance> map(StructureIndexer.Result result) {
            try{
                Substance r=instrument(result);
                if(r==null)return Stream.empty();

                StreamUtil.StreamConcatter< Substance> sstream = StreamUtil.with(Stream.of(r));

                if(!structureSearchConfiguration.isIncludePolymers()){
                    if(r instanceof PolymerSubstance){
                        return Stream.empty();
                    }
                }

                if(structureSearchConfiguration.isIncludeModifications()){
                    //add modifications results as well
                    //This is likely to be a source of slow-down
                    //due to possibly missing indexes
                    List<Modifications> modlist = modificationRepository.findByStructuralModifications_molecularFragment_refuuid(result.getId());


                    Stream<Substance> rStream = modlist.stream().map(m -> {
                                Substance ff = substanceRepository.findByModifications_Uuid(m.uuid);
                                //TODO shouldn't this return empty stream if null?
                                return ff;
                            }
                    );
                    sstream = sstream.and(rStream);
                }


                if(structureSearchConfiguration.isIncludeMixtures()){
                    //add mixture results as well
                    List<Substance> mixlist = new ArrayList<>(mixtureSubstanceRepository.findByMixture_Components_Substance_Refuuid(result.getId()));
                    sstream = sstream.and(mixlist.stream());
                }
                return sstream.stream();
            }catch(Exception e){
                log.error("error processing record", e);
                return Stream.empty();
            }
        }

        protected Substance instrument(StructureIndexer.Result r) throws Exception {

            EntityUtils.Key k = EntityUtils.Key.of(subMeta, UUID.fromString(r.getId()));

            Optional<EntityUtils.EntityWrapper<Substance>> efetch = k.fetch(substanceRepository);


            if (efetch.isPresent()) {
                Substance chem = (Substance) efetch.get().getValue();
                Map<String, Object> matchingContext = new HashMap<>();

                double similarity = r.getSimilarity();
                log.debug(String.format("%1$ 5d: matched %2$s %3$.3f", ++index, r.getId(), r.getSimilarity()));
                Chemical mol = r.getMol();


//                int[] amap = r.getHits();
                int[] amap = new int[mol.getAtomCount()];
                int i = 0, nmaps = 0;
                for (Atom ma : mol.getAtoms()) {
                    amap[i] = ma.getAtomToAtomMap().orElse(0);
                    if (amap[i] > 0) {
                        ++nmaps;
                    }
                    ++i;
                }
                if (nmaps > 0) {
                    matchingContext.put("atomMaps", amap);
                }
                matchingContext.put("similarity", similarity);
                EntityUtils.EntityWrapper<?> ew = EntityUtils.EntityWrapper.of(chem);
                
                gsrsCache.setMatchingContext(this.getContext().getId(), ew.getKey().toRootKey(), matchingContext);
                return chem;
            }
            return null;
        }
    }

    public boolean isFlexSearchNoStereo() {
        return this.structureSearchConfiguration.isFlexSearchNoStereo();
    }
}
