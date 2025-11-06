package example.imports;

import example.GsrsModuleSubstanceApplication;
import gsrs.dataexchange.model.MappingAction;
import gsrs.importer.DefaultPropertyBasedRecordContext;
import gsrs.importer.PropertyBasedDataRecordContext;
import gsrs.module.substance.importers.importActionFactories.ApprovalIdExtractorActionFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class ApprovalIdExtractorActionFactoryTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Test
    void testParseApprovalId() throws Exception {
        ApprovalIdExtractorActionFactory approvalIdExtractorActionFactory = new ApprovalIdExtractorActionFactory();
        SubstanceBuilder substanceBuilder = new SubstanceBuilder();
        DefaultPropertyBasedRecordContext ctx = new DefaultPropertyBasedRecordContext();

        Map<String, Object> inputParams = new HashMap<>();

        String approvalId = "Approved_201";
        String approver = null;// "Smith";
        inputParams.put("approvalId", approvalId);
        inputParams.put("approver", approver);
        Calendar calendar = Calendar.getInstance(); // current date/time
        calendar.add(Calendar.DAY_OF_YEAR, -1); // subtract one day
        Date yesterday = calendar.getTime();
        inputParams.put("approvalDate", yesterday);

        MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> action= approvalIdExtractorActionFactory.create(inputParams);
        action.act(substanceBuilder, ctx);
        Substance finished = substanceBuilder.build();
        Assertions.assertEquals(approvalId, finished.approvalID);
        Assertions.assertEquals(yesterday, finished.approved);
        //Assertions.assertEquals(approver, finished.approvedBy);
    }

    @Test
    void testParseApprovalIdNoDateGetNow() throws Exception {
        ApprovalIdExtractorActionFactory approvalIdExtractorActionFactory = new ApprovalIdExtractorActionFactory();
        SubstanceBuilder substanceBuilder = new SubstanceBuilder();
        DefaultPropertyBasedRecordContext ctx = new DefaultPropertyBasedRecordContext();

        Map<String, Object> inputParams = new HashMap<>();

        String approvalId = "Substance " + UUID.randomUUID();
        String approver = null;;
        inputParams.put("approvalId", approvalId);
        inputParams.put("approver", approver);

        MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> action= approvalIdExtractorActionFactory.create(inputParams);
        action.act(substanceBuilder, ctx);
        Substance finished = substanceBuilder.build();
        Assertions.assertEquals(approvalId, finished.approvalID);
        Calendar calendar= Calendar.getInstance();
        calendar.get(Calendar.DAY_OF_YEAR);
        Calendar appoved = Calendar.getInstance();
        appoved.setTime(finished.approved);
        Assertions.assertEquals(calendar.get(Calendar.DAY_OF_YEAR), appoved.get(Calendar.DAY_OF_YEAR));
    }

}
