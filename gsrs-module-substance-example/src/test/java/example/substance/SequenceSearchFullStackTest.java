    package example.substance;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import example.GsrsModuleSubstanceApplication;
import gov.nih.ncats.common.stream.StreamUtil;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.module.substance.processors.RelationEventListener;
import gsrs.repository.EditRepository;
import gsrs.startertests.TestEntityProcessorFactory;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.NucleicAcidSubstance;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.ProteinValidator;
import ix.seqaln.SequenceIndexer.CutoffType;
import ix.seqaln.SequenceIndexer.Result;
import ix.seqaln.SequenceIndexer.ResultEnumeration;
import ix.seqaln.service.SequenceIndexerService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@ActiveProfiles("test")
@RecordApplicationEvents
@Import({SequenceSearchFullStackTest.Configuration.class, RelationEventListener.class})
@WithMockUser(username = "admin", roles="Admin")
@Slf4j
public class SequenceSearchFullStackTest  extends AbstractSubstanceJpaFullStackEntityTest {

    @BeforeEach
    void runIndexingTasksInline() {
        Configuration.indexingExecutor = Runnable::run;
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(mockTaskExecutor).execute(any(Runnable.class));
    }

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @Autowired
    private ApplicationContext applicationContext;

    @TestConfiguration
    @EnableAsync
    public static class Configuration implements AsyncConfigurer{

        static volatile Executor indexingExecutor = Runnable::run;

        List<Runner> runs = new ArrayList<Runner>();

        public static class Runner{
            boolean ran=false;
            Runnable r;
            public Runner(Runnable r) {
                this.r=r;
            }
            public void run() {
                if(!this.ran) {
                    r.run();
                    this.ran=true;
                }
            }

        }


        @Override
        public Executor getAsyncExecutor() {
            return task -> indexingExecutor.execute(task);
        }


        public void flush() {
            runs.clear();
        }

    }


    @Autowired
    private TestEntityProcessorFactory testEntityProcessorFactory;

    @Autowired
    private TestGsrsValidatorFactory testGsrsValidatorFactory;


    @Autowired
    private EditRepository editRepository;

    @Autowired
    SequenceIndexerService seqIndexer;



    @Override
    protected Substance assertCreatedAPI(JsonNode json) {
        Substance s= super.assertCreatedAPI(json);
        return s;

    }

    @Override
    protected Substance assertUpdatedAPI(JsonNode json) {
        Substance s= super.assertUpdatedAPI(json);
        return s;
    }

    @Test
    public void proteinSequenceUpdateIsSearchableAfterAsyncCommit() throws Exception {
        ExecutorService workers = Executors.newSingleThreadExecutor();
        AtomicInteger dispatched = new AtomicInteger();
        try {
            Executor asyncExecutor = task -> {
                dispatched.incrementAndGet();
                workers.execute(task);
            };
            Configuration.indexingExecutor = asyncExecutor;
            doAnswer(invocation -> {
                asyncExecutor.execute((Runnable) invocation.getArgument(0));
                return null;
            }).when(mockTaskExecutor).execute(any(Runnable.class));

            String oldSequence = "ACDEFGHIJKLMN";
            String newSequence = "TTTTTTTTTTATCGHHHH";
            ProteinSubstance protein = new SubstanceBuilder().asProtein()
                    .addName("Async sequence update")
                    .addSubunitWithDefaultReference(oldSequence).build();
            ProteinSubstance created = (ProteinSubstance) assertCreatedAPI(protein.toFullJsonNode());
            workers.submit(() -> {}).get(30, TimeUnit.SECONDS);
            assertTrue("The test must dispatch indexing onto another thread", dispatched.get() > 0);
            ProteinSubstance saved = getProteinSubstance(created.getUuid());
            UUID subunitId = firstProteinSubunitUuid(saved);
            assertEquals(1, countResultsForSubunit(getGlobalResults(oldSequence, "protein"), subunitId));

            saved.protein.subunits.get(0).sequence = newSequence;
            assertUpdatedAPI(saved.toFullJsonNode());
            workers.submit(() -> {}).get(30, TimeUnit.SECONDS);

            assertEquals("The sequence edit must be persisted", newSequence,
                    getProteinSubstance(created.getUuid()).protein.subunits.get(0).sequence);
            assertEquals("The old sequence must leave the index", 0,
                    countResultsForSubunit(getGlobalResults(oldSequence, "protein"), subunitId));
            assertEquals("The new sequence must enter the index", 1,
                    countResultsForSubunit(getGlobalResults(newSequence, "protein"), subunitId));
        } finally {
            Configuration.indexingExecutor = Runnable::run;
            workers.shutdown();
            assertTrue("Indexing workers must finish", workers.awaitTermination(30, TimeUnit.SECONDS));
        }
    }

    @Test
    public void addProteinSequenceAndThenSearchShouldGiveExactMatchResult() {


        String seq="ACDEFGHIJKLMN";

        ProteinSubstance substance2 = new SubstanceBuilder()
                .asProtein()
                .addName("SUB1")
                .addSubunitWithDefaultReference(seq)
                .build();

        ProteinSubstance created = (ProteinSubstance) assertCreatedAPI(substance2.toFullJsonNode());
        UUID uuid2 = firstProteinSubunitUuid(getProteinSubstance(created.getUuid()));

        List<Result> lres=getGlobalResults(seq,"protein");

        assertEquals("Should return 1 exact match for this protein subunit", 1, countResultsForSubunit(lres, uuid2));

    }


    @Test
    public void addProteinSequenceAndThenEditSearchShouldNotHonorOldSearch() {
        String seq="ACDEFGHIJKLMN";
        String seq2="TTTTTTTTTTATCGHHHH";

        ProteinSubstance substance = new SubstanceBuilder()
                .asProtein()
                .addName("SUB1")
                .addSubunitWithDefaultReference(seq)
                .build();


        ProteinSubstance created = (ProteinSubstance) assertCreatedAPI(substance.toFullJsonNode());
        UUID uuid1 = created.getUuid();
        ProteinSubstance sup = getProteinSubstance(uuid1);
        UUID uuid2 = firstProteinSubunitUuid(sup);


        List<Result> lres=getGlobalResults(seq,"protein");


        assertEquals("Should return 1 exact match for this protein subunit", 1, countResultsForSubunit(lres, uuid2));


        sup.protein.subunits.get(0).sequence=seq2;

        assertUpdatedAPI(sup.toFullJsonNode());

        lres=getGlobalResults(seq,"protein");
        assertEquals("Old protein sequence should not match the edited subunit", 0, countResultsForSubunit(lres, uuid2));

//        Thread.sleep(5l);
        System.out.println("STARTING");
        lres=getGlobalResults(seq2,"protein");

        System.out.println("RETURNED");
        assertEquals(1, countResultsForSubunit(lres, uuid2));


    }


    @Test
    public void addNASequenceAndThenSearchShouldGiveExactMatchResult()   throws Exception {
        String seq="ATCATCATCATCATCGATACAGATACAGTCAGTCAGTCGATCAGTCGTTATATATCGCGATTACG";

        NucleicAcidSubstance substance2 = new SubstanceBuilder()
                .asNucleicAcid()
                .addName("SUB1")
                .addDnaSubunit(seq)
                .build();
        NucleicAcidSubstance created = (NucleicAcidSubstance) assertCreatedAPI(substance2.toFullJsonNode());
        UUID uuid2 = firstNucleicAcidSubunitUuid(getNucleicAcidSubstance(created.getUuid()));

        List<Result> lres=getGlobalResults(seq,"nucleicAcid");

        assertEquals("Should return 1 exact match for na search",1,lres.size());
        assertEquals(uuid2.toString(),lres.get(0).id);

    }


    @Test
    public void addNASequenceAndThenEditSearchShouldNotHonorOldSearch() throws Exception {
        String seq="ATCATCATCATCATCGATACAGATACAGTCAGTCAGTCGATCAGTCGTTATATATCGCGATTACG";

        String seq2="CCCCCCCCCCCCCCCCCCAAAAAAATTTTTTTAAAACCACAC";

        NucleicAcidSubstance substance2 = new SubstanceBuilder()
                .asNucleicAcid()
                .addName("SUB1")
                .addDnaSubunit(seq)
                .build();
        NucleicAcidSubstance created = (NucleicAcidSubstance) assertCreatedAPI(substance2.toFullJsonNode());
        UUID uuid1 = created.getUuid();
        NucleicAcidSubstance sup = getNucleicAcidSubstance(uuid1);
        UUID uuid2 = firstNucleicAcidSubunitUuid(sup);

        List<Result> lres=getGlobalResults(seq,"nucleicAcid");

        assertEquals("Should return 1 exact match for na search",1,lres.size());
        assertEquals(uuid2.toString(),lres.get(0).id);


        sup.nucleicAcid.subunits.get(0).sequence=seq2;

        assertUpdatedAPI(sup.toFullJsonNode());

        lres=getGlobalResults(seq,"nucleicAcid");
        assertEquals("Should return 0 exact match for protein search after changed",0,lres.size());

        lres=getGlobalResults(seq2,"nucleicAcid");

        assertEquals(1,lres.size());


    }


    @Test
    public void addNASequenceAndThenEditSearchThenSearchForProteinShouldNotReturnResults() throws Exception {
        String seq="ATCATCATCATCATCGATACAGATACAGTCAGTCAGTCGATCAGTCGTTATATATCGCGATTACG";

        String seq2="CCCCCCCCCCCCCCCCCCAAAAAAATTTTTTTAAAACCACAC";

        NucleicAcidSubstance substance2 = new SubstanceBuilder()
                .asNucleicAcid()
                .addName("SUB1")
                .addDnaSubunit(seq)
                .build();
        NucleicAcidSubstance created = (NucleicAcidSubstance) assertCreatedAPI(substance2.toFullJsonNode());
        UUID uuid1 = created.getUuid();
        NucleicAcidSubstance sup = getNucleicAcidSubstance(uuid1);
        UUID uuid2 = firstNucleicAcidSubunitUuid(sup);

        List<Result> lres=getGlobalResults(seq,"nucleicAcid");

        assertEquals("Should return 1 exact match for na search",1,lres.size());
        assertEquals(uuid2.toString(),lres.get(0).id);


        sup.nucleicAcid.subunits.get(0).sequence=seq2;

        assertUpdatedAPI(sup.toFullJsonNode());

        lres=getGlobalResults(seq,"nucleicAcid");
        assertEquals("Should return 0 exact match for protein search after changed",0,lres.size());

        lres=getGlobalResults(seq2,"nucleicAcid");

        assertEquals(1,lres.size());

        lres=getGlobalResults(seq2,"protein");

        assertEquals(0,lres.size());

    }


    private List<Result> getGlobalResults(String seq, String type){

        ResultEnumeration re=seqIndexer.search(seq, 0.95, CutoffType.GLOBAL, type);

        List<Result> lres=StreamUtil.forEnumeration(re)
                .collect(Collectors.toList());
        return lres;
    }

    private long countResultsForSubunit(List<Result> results, UUID subunitUuid) {
        String id = subunitUuid.toString();
        return results.stream()
                .filter(result -> id.equals(result.id))
                .count();
    }

    private ProteinSubstance getProteinSubstance(UUID uuid) {
        return (ProteinSubstance) substanceEntityService.get(uuid).get();
    }

    private NucleicAcidSubstance getNucleicAcidSubstance(UUID uuid) {
        return (NucleicAcidSubstance) substanceEntityService.get(uuid).get();
    }

    private UUID firstProteinSubunitUuid(ProteinSubstance substance) {
        return substance.protein.subunits.get(0).uuid;
    }

    private UUID firstNucleicAcidSubunitUuid(NucleicAcidSubstance substance) {
        return substance.nucleicAcid.subunits.get(0).uuid;
    }

    @Test
    public void addProteinSequencesAndThenSearchShouldMatchItself() throws Exception {

        loadData();
        String remigromigSeq1 = "EVQLVESGGGLVQPGGSLRLSCAASGFDFTAYAMHWVRQAPGKGLEWVASIYPSGGYTAYADSVKGRFTISADTSKNTAYLQMNSLRAEDTAVYYCARRSYYFALDYWGQGTLVTVSSGGGGSDIQMTQSPSSLSASVGDRVTITCRASQSVSSAVAWYQQKPGKAPKLLIYSASSLYSGVPSRFSGSRSGTDFTLTISSLQPEDFATYYCQQYWAYYSPITFGQGTKVEIKGGGGSGGGGSEPKSSDKTHTCPPCPAPEAAGGPSVFLFPPKPKDTLMISRTPEVTCVVVDVSHEDPEVKFNWYVDGVEVHNAKTKPREEQYNSTYRVVSVLTVLHQDWLNGKEYKCKVSNKALPASIEKTISKAKGQPREPMVFDLPPSREEMTKNQVSLWCMVKGFYPSDIAVEWESNGQPENNYKTTPPVLDSDGSFFLYSKLTVDKSRWQQGNVFSCSVMHEALHNHYTQKSLSLSPGKGGGSGGGSGGGSGGGSGSTGEVQLVESGGGLVQPGGSLRLSCAASGFTLSSYSMHWVRQAPGKGLEWVAYISSYDSITDYADSVKGRFTISADTSKNTAYLQMNSLRAEDTAVYYCARPAVGHMAFDYWGQGTLVTVSSASTKGPSVFPLAPSSKSTSGGTAALGCLVKDYFPEPVTVSWNSGALTSGVHTFPAVLQSSGLYSLSSVVTVPSSSLGTQTYICNVNHKPSNTKVDKKVEPKSCDKTHT";
        List<Result> lres=getGlobalResults(remigromigSeq1,"protein");

        assertEquals("Should return 2 matches for protein search",2, lres.size());
        AtomicBoolean found100PercentMatch = new AtomicBoolean(false);
        lres.forEach(r ->{
            log.info("looking at result {}", r.id);
            AtomicInteger matchNumber = new AtomicInteger(0);
            r.alignments.forEach(a->{
                log.info("score {}; iden: {}", a.score, a.iden);
                if( matchNumber.get()==0 && r.score == 1.00) {
                    found100PercentMatch.set(true);
                }
                matchNumber.incrementAndGet();
            });
        } );
        assertTrue(found100PercentMatch.get());
    }

    private void loadData() throws IOException {
        SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(hashIndexer);
        testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
        {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(ProteinValidator.class);
            config.setNewObjClass(ProteinSubstance.class);
            factory.addValidator("substances", config);
        }

        JsonMapper mapper = JsonMapper.builderWithJackson2Defaults()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        Resource dataFile = new ClassPathResource("testJSON/XLR461MD3M.json");
        String recordJson1 = Files.readString(dataFile.getFile().toPath());
        JsonNode json = mapper.readTree(recordJson1);
        assertCreatedAPI(json);
        Resource dataFile2 = new ClassPathResource("testJSON/C3TZ3X6VNV.json");
        String recordJson2 = Files.readString(dataFile2.getFile().toPath());
        JsonNode json2 = mapper.readTree(recordJson2);
        assertCreatedAPI(json2);
        System.out.println("loaded data");
    }
}
