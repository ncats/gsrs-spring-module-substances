package example.substance.export;

import example.GsrsModuleSubstanceApplication;
import gov.nih.ncats.common.sneak.Sneak;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.service.GsrsEntityService;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.GsrsFullStackTest;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.ginas.exporters.ExportDir;
import ix.ginas.exporters.ExportMetaData;
import ix.ginas.exporters.ExportProcess;
import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
@GsrsFullStackTest(dirtyMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(AutowireHelper.class)
public class ExportTest extends AbstractSubstanceJpaFullStackEntityTest {

    private static final ClassPathResource REP90 = new ClassPathResource("/testdumps/rep90.ginas");

    @Autowired
    private SubstanceRepository substanceRepository;

    public ExportTest() {
        super(false);
    }

    @BeforeEach
    public void loadRep90Once() throws IOException {
        if (substanceRepository.count() == 0) {
            List<GsrsEntityService.CreationResult<Substance>> results = loadGsrsFile(REP90);
            assertEquals(90, results.size());
            assertEquals(90, results.stream().filter(GsrsEntityService.CreationResult::isCreated).count());
            ensurePass(results);
        }
        assertEquals(90, substanceRepository.count());
    }

    @Test
    public void loadRep90() {
        assertEquals(90, substanceRepository.count());
    }

    @Test
    public void programmaticallyExportRep90() throws Exception {
        ExportMetaData exportMetaData = new ExportMetaData("collection1", "query", "admin", true, "csv");
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute( status ->{
            boolean done=false;
            assertEquals(90, substanceRepository.count());
            try(Stream<Substance> stream = substanceRepository.findAll().stream()) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                try {
                    ExportDir.ExportFile<ExportMetaData> exportFile= new ExportDir<>(tempDir, ExportMetaData.class).createFile("exportFile", exportMetaData);

                    Set<String> ids = new HashSet<>();
                    Exporter<Substance> exporter = new Exporter<Substance>() {
                        @Override
                        public void export(Substance s) throws IOException {
                            ids.add(s.uuid.toString());
                        }

                        @Override
                        public void close() throws IOException {
                        }
                    };
                    new ExportProcess<>(exportFile, ()->stream)
                            .run(executor, o -> exporter);
                    executor.shutdown();
                    assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
                    assertEquals(90, ids.size());
                    done= true;

                } catch (Exception e) {
                    Sneak.sneakyThrow(e);
                } finally {
                    executor.shutdownNow();
                }
                assertTrue(done);
            }
            return status;
        });

    }
}
