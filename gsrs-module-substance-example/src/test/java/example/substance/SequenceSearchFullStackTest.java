package example.substance;

import com.fasterxml.jackson.databind.JsonNode;
import example.GsrsModuleSubstanceApplication;
import gov.nih.ncats.common.stream.StreamUtil;
import gsrs.module.substance.processors.ReferenceProcessor;
import gsrs.module.substance.processors.RelationEventListener;
import gsrs.module.substance.processors.RelationshipProcessor;
import gsrs.module.substance.processors.SubstanceProcessor;
import gsrs.module.substance.services.RelationshipService;
import gsrs.repository.EditRepository;
import gsrs.startertests.TestEntityProcessorFactory;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.NucleicAcidSubstance;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Substance;
import ix.seqaln.SequenceIndexer.CutoffType;
import ix.seqaln.SequenceIndexer.Result;
import ix.seqaln.SequenceIndexer.ResultEnumeration;
import ix.seqaln.service.SequenceIndexerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@ActiveProfiles("test")
@RecordApplicationEvents
@Import({SequenceSearchFullStackTest.Configuration.class, RelationEventListener.class})
@WithMockUser(username = "admin", roles="Admin")
public class SequenceSearchFullStackTest  extends AbstractSubstanceJpaFullStackEntityTest {
    
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
//            ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
//exec.
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

    @MockitoSpyBean
    private SubstanceProcessor substanceProcessor;
    @MockitoSpyBean
    private RelationshipProcessor relationshipProcessor;
    @MockitoSpyBean
    private ReferenceProcessor referenceProcessor;

    @Autowired
    private EditRepository editRepository;
    
    @Autowired
    SequenceIndexerService seqIndexer;

    @MockitoSpyBean
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
    

    
}
