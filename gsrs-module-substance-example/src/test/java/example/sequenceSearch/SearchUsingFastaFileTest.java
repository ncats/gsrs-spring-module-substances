package example.sequenceSearch;

import example.GsrsModuleSubstanceApplication;
import gov.nih.ncats.common.sneak.Sneak;
import gsrs.service.PayloadService;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.modelBuilders.NucleicAcidSubstanceBuilder;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.models.v1.Reference;
import ix.seqaln.SequenceIndexer;
import ix.seqaln.service.SequenceIndexerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {GsrsModuleSubstanceApplication.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SearchUsingFastaFileTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private SequenceIndexerService sut;

    @Autowired
    private PayloadService payloadService;


    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void indexNucleicAcidFastaFilePayloadAsReference() throws IOException {

        testSequenceFileIsSearchable("ACGTACGTACGT", true);
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void indexProteinFastaFilePayloadAsReference() throws IOException {

        testSequenceFileIsSearchable("VHLTPEEK", false);
    }

    private void testSequenceFileIsSearchable(String sequence, boolean nucleicAcid) {
        UUID uuid = UUID.randomUUID();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        UUID payloadId = transactionTemplate.execute(s -> {
            try {
                return payloadService.createPayload("myFile.fasta", "txt",
                        ">seq1\n" +
                                sequence,
                        PayloadService.PayloadPersistType.PERM
                ).id;
            } catch (Throwable t) {
                return Sneak.sneakyThrow(t);
            }
        });
        transactionTemplate.executeWithoutResult(ignored ->{
            try{
        Reference ref = new Reference();

        ref.addTag("fasta");

        ref.uploadedFile = "http://localhost/api/v1/payload("+payloadId +")";
        AbstractSubstanceBuilder<?,?> builder = nucleicAcid ? new NucleicAcidSubstanceBuilder() : new ProteinSubstanceBuilder();

                    builder
                    .setUUID(uuid)
                    .addName("mySubstance")
                    .addReference(ref)
                    .buildJsonAnd(this::assertCreatedAPI);

        }catch (Throwable e){
            Sneak.sneakyThrow(e);
        }
            });
        transactionTemplate.executeWithoutResult( ignore -> {
            SequenceIndexer.ResultEnumeration results = sut.search(sequence, 1.0, SequenceIndexer.CutoffType.GLOBAL, nucleicAcid ?"nucleicacid": "protein");

            assertTrue(results.hasMoreElements());
            SequenceIndexer.Result result = results.nextElement();
            assertEquals(">" + uuid + "|myFile.fasta|seq1", result.id);
            assertEquals(1.0, result.alignments.get(0).iden);
            assertEquals(sequence, result.alignments.get(0).query);
            assertFalse(results.hasMoreElements());
        });
    }
}
