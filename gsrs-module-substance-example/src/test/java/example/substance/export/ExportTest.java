package example.substance.export;

import example.GsrsModuleSubstanceApplication;
import gov.nih.ncats.common.sneak.Sneak;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.service.GsrsEntityService;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.ginas.exporters.ExportDir;
import ix.ginas.exporters.ExportMetaData;
import ix.ginas.exporters.ExportProcess;
import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
public class ExportTest extends AbstractSubstanceJpaFullStackEntityTest {




    @Autowired
    private SubstanceRepository substanceRepository;


    @Test
    public void loadRep90() throws IOException {
        List<GsrsEntityService.CreationResult<Substance>> results = loadGsrsFile(new ClassPathResource("/testdumps/rep90.ginas"));

        assertEquals(90, results.size());
        assertEquals(90, results.stream().map(GsrsEntityService.CreationResult::isCreated).count());

        ensurePass(results);
    }

    @Test
    public void programmaticallyExportRep90() throws Exception {
        ensurePass(loadGsrsFile(new ClassPathResource("/testdumps/rep90.ginas")));

        ExportMetaData exportMetaData = new ExportMetaData("collection1", "query", "admin", true, "csv");
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute( status ->{
            boolean done=false;
            try(Stream<Substance> stream = substanceRepository.findAll().stream()) {
                try {
                    ExportDir.ExportFile<ExportMetaData> exportFile= new ExportDir<>(tempDir, ExportMetaData.class).createFile("exportFile", exportMetaData);

                    CountDownLatch latch = new CountDownLatch(90);
                    Set<String> ids = new HashSet<>();
                    Exporter<Substance> exporter = new Exporter<Substance>() {
                        @Override
                        public void export(Substance s) throws IOException {
                            latch.countDown();
                            ids.add(s.uuid.toString());
                        }

                        @Override
                        public void close() throws IOException {
                            while(latch.getCount() >0){
                                latch.countDown();
                            }
                        }
                    };
                    new ExportProcess<>(exportFile, ()->stream)
                            .run(Executors.newSingleThreadExecutor(), o -> exporter);
                    latch.await();
                    assertEquals(90, ids.size());
                    done= true;

                } catch (Exception e) {
                    Sneak.sneakyThrow(e);
                }
                assertTrue(done);
            }
            return status;
        });

    }
}
