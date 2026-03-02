package example.substance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.GsrsModuleSubstanceApplication;
import gov.nih.ncats.common.stream.StreamUtil;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.module.substance.processors.ReferenceProcessor;
import gsrs.module.substance.processors.RelationEventListener;
import gsrs.module.substance.processors.RelationshipProcessor;
import gsrs.module.substance.processors.SubstanceProcessor;
import gsrs.module.substance.services.RelationshipService;
import gsrs.repository.EditRepository;
import gsrs.springUtils.AutowireHelper;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@ActiveProfiles("test")
@RecordApplicationEvents
@Import({SequenceSearchFullStackTest.Configuration.class, RelationEventListener.class})
@WithMockUser(username = "admin", roles="Admin")
@Slf4j
public class SequenceSearchFullStackTest  extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @TestConfiguration
    @EnableAsync
    public static class Configuration implements AsyncConfigurer{

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
            return new Executor() {
                @Override
                public void execute(Runnable arg0) {
                    runs.add(new Runner(arg0));
                }
                
            };
        }


        public void flush() {
            for(Runner r: runs.stream().collect(Collectors.toList())) {
                r.run();
            }
            runs.clear();
        }
        
    }

    
    @Autowired
    private Configuration conf;
    
    @Autowired
    private TestEntityProcessorFactory testEntityProcessorFactory;

    @Autowired
    private TestGsrsValidatorFactory testGsrsValidatorFactory;

    @SpyBean
    private SubstanceProcessor substanceProcessor;
    @SpyBean
    private RelationshipProcessor relationshipProcessor;
    @SpyBean
    private ReferenceProcessor referenceProcessor;

    @Autowired
    private EditRepository editRepository;
    
    @Autowired
    SequenceIndexerService seqIndexer;

    @SpyBean
    private RelationshipService relationshipService;

    
    @Override
    protected Substance assertCreatedAPI(JsonNode json) {
        Substance s= super.assertCreatedAPI(json);
        conf.flush();
        return s;
        
    }

    @Override
    protected Substance assertUpdatedAPI(JsonNode json) {
        Substance s= super.assertUpdatedAPI(json);
        conf.flush();
        return s;
    }

    @Test
    public void addProteinSequenceAndThenSearchShouldGiveExactMatchResult()   throws Exception {

       
       
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        String seq="ACDEFGHIJKLMN";
        
        ProteinSubstance substance2 = new SubstanceBuilder()
                .asProtein()
                .addName("SUB1")
                .setUUID(uuid1)
                .addSubunitWithDefaultReference(seq)
                .andThen(ps->{ps.protein.subunits.get(0).setUuid(uuid2);})
                .build();
        
        TransactionTemplate transactionSearch = new TransactionTemplate(transactionManager);
        transactionSearch.execute(t->{

            assertCreatedAPI(substance2.toFullJsonNode());
            return null;
        });
        
        List<Result> lres=getGlobalResults(seq,"protein");
        
        assertEquals("Should return 1 exact match for protein search",1,lres.size());
        assertEquals(uuid2.toString(),lres.get(0).id);
        
    }
    
        
    @Test
    public void addProteinSequenceAndThenEditSearchShouldNotHonorOldSearch() throws Exception {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        String seq="ACDEFGHIJKLMN";
        String seq2="TTTTTTTTTTATCGHHHH";
        
        ProteinSubstance substance = new SubstanceBuilder()
                .asProtein()
                .addName("SUB1")
                .setUUID(uuid1)
                .addSubunitWithDefaultReference(seq)
                .andThen(ps->{ps.protein.subunits.get(0).setUuid(uuid2);})
                .build();
        
      
        assertCreatedAPI(substance.toFullJsonNode());
        
      
        List<Result> lres=getGlobalResults(seq,"protein");
        
        
        assertEquals("Should return 1 exact match for protein search",1,lres.size());
        assertEquals(uuid2.toString(),lres.get(0).id);
        
        
        ProteinSubstance sup = (ProteinSubstance) substanceEntityService.get(uuid1).get();

        sup.protein.subunits.get(0).sequence=seq2;

        assertUpdatedAPI(sup.toFullJsonNode());
        
        lres=getGlobalResults(seq,"protein");
        assertEquals("Should return 0 exact match for protein search after changed",0,lres.size());
        
//        Thread.sleep(5l);
        System.out.println("STARTING");
        lres=getGlobalResults(seq2,"protein");

        System.out.println("RETURNED");
        assertEquals(1,lres.size());
        
        
    }
    
    
    @Test
    public void addNASequenceAndThenSearchShouldGiveExactMatchResult()   throws Exception {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        String seq="ATCATCATCATCATCGATACAGATACAGTCAGTCAGTCGATCAGTCGTTATATATCGCGATTACG";
        
        NucleicAcidSubstance substance2 = new SubstanceBuilder()
                .asNucleicAcid()
                .addName("SUB1")
                .setUUID(uuid1)
                .addDnaSubunit(seq)
                .andThen(ps->{ps.nucleicAcid.subunits.get(0).setUuid(uuid2);})
                .build();
        assertCreatedAPI(substance2.toFullJsonNode());
        
        List<Result> lres=getGlobalResults(seq,"nucleicAcid");
        
        assertEquals("Should return 1 exact match for na search",1,lres.size());
        assertEquals(uuid2.toString(),lres.get(0).id);
        
    }
    

    @Test
    public void addNASequenceAndThenEditSearchShouldNotHonorOldSearch() throws Exception {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        String seq="ATCATCATCATCATCGATACAGATACAGTCAGTCAGTCGATCAGTCGTTATATATCGCGATTACG";

        String seq2="CCCCCCCCCCCCCCCCCCAAAAAAATTTTTTTAAAACCACAC";
        
        NucleicAcidSubstance substance2 = new SubstanceBuilder()
                .asNucleicAcid()
                .addName("SUB1")
                .setUUID(uuid1)
                .addDnaSubunit(seq)
                .andThen(ps->{ps.nucleicAcid.subunits.get(0).setUuid(uuid2);})
                .build();
        assertCreatedAPI(substance2.toFullJsonNode());
        
        List<Result> lres=getGlobalResults(seq,"nucleicAcid");
        
        assertEquals("Should return 1 exact match for na search",1,lres.size());
        assertEquals(uuid2.toString(),lres.get(0).id);
        
        
        NucleicAcidSubstance sup = (NucleicAcidSubstance) substanceEntityService.get(uuid1).get();
        
        sup.nucleicAcid.subunits.get(0).sequence=seq2;

        assertUpdatedAPI(sup.toFullJsonNode());
        
        lres=getGlobalResults(seq,"nucleicAcid");
        assertEquals("Should return 0 exact match for protein search after changed",0,lres.size());
        
        lres=getGlobalResults(seq2,"nucleicAcid");

        assertEquals(1,lres.size());
        
        
    }
    

    @Test
    public void addNASequenceAndThenEditSearchThenSearchForProteinShouldNotReturnResults() throws Exception {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        String seq="ATCATCATCATCATCGATACAGATACAGTCAGTCAGTCGATCAGTCGTTATATATCGCGATTACG";

        String seq2="CCCCCCCCCCCCCCCCCCAAAAAAATTTTTTTAAAACCACAC";
        
        NucleicAcidSubstance substance2 = new SubstanceBuilder()
                .asNucleicAcid()
                .addName("SUB1")
                .setUUID(uuid1)
                .addDnaSubunit(seq)
                .andThen(ps->{ps.nucleicAcid.subunits.get(0).setUuid(uuid2);})
                .build();
        assertCreatedAPI(substance2.toFullJsonNode());
        
        List<Result> lres=getGlobalResults(seq,"nucleicAcid");
        
        assertEquals("Should return 1 exact match for na search",1,lres.size());
        assertEquals(uuid2.toString(),lres.get(0).id);
        
        
        NucleicAcidSubstance sup = (NucleicAcidSubstance) substanceEntityService.get(uuid1).get();
        
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
        AutowireHelper.getInstance().autowire(hashIndexer);
        testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
        {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(ProteinValidator.class);
            config.setNewObjClass(ProteinSubstance.class);
            factory.addValidator("substances", config);
        }

        ObjectMapper mapper = new ObjectMapper();
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
