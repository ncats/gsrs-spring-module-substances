package example.imports;

import example.GsrsModuleSubstanceApplication;
import gsrs.dataexchange.processingactions.ScrubProcessingAction;
import gsrs.repository.PrincipalRepository;
import gsrs.services.PrincipalService;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.models.Principal;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class ScrubProcessingActionTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    PrincipalService principalService;

    @Autowired
    PrincipalRepository principalRepository;

    private static String registrarUserName = "Reggie Registrar";

    private static boolean processed = false;

    @BeforeEach
    public void beforeAll() {
        if (!processed) {
            Principal registrar = new Principal();
            registrar.username = registrarUserName;
            registrar.email = "registrar@gmail.com";
            principalRepository.save(registrar);
            processed = true;
        }
    }

    @Test
    public void testScrubUuid() throws Exception {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder.setStructureWithDefaultReference("CCCCCCN");
        chemicalSubstanceBuilder.addName("hexane amine");
        UUID startingSubstanceUuid = UUID.randomUUID();
        chemicalSubstanceBuilder.setUUID(startingSubstanceUuid);
        ChemicalSubstance startingSubstance = chemicalSubstanceBuilder.build();
        UUID nameUuid = startingSubstance.names.get(0).uuid;

        ScrubProcessingAction scrubProcessingAction = new ScrubProcessingAction();
        StringBuilder logHolder = new StringBuilder();
        Map<String, Object> scrubberSettings = new HashMap<>();
        scrubberSettings.put("UUIDCleanup", true);
        scrubberSettings.put("regenerateUUIDs", true);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("scrubberSettings", scrubberSettings);
        Substance result = scrubProcessingAction.process(startingSubstance, null, parameters, logHolder::append);
        UUID resultNameUuid = result.names.get(0).uuid;

        Assertions.assertNotEquals(result.uuid, startingSubstanceUuid);
        Assertions.assertEquals(startingSubstance.uuid, startingSubstanceUuid);
        Assertions.assertNotEquals(nameUuid, resultNameUuid);
        System.out.println(logHolder.toString());
    }

    @Test
    public void testRemoveApprovalId() throws Exception {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder.setStructureWithDefaultReference("CCCCCCN");
        chemicalSubstanceBuilder.addName("hexane amine");
        Principal admin = principalRepository.findDistinctByUsernameIgnoreCase("admin");
        String dummyApprovalID = "123456789A";

        chemicalSubstanceBuilder.setApproval(admin, new Date(), dummyApprovalID);

        UUID startingSubstanceUuid = UUID.randomUUID();
        chemicalSubstanceBuilder.setUUID(startingSubstanceUuid);
        ChemicalSubstance startingSubstance = chemicalSubstanceBuilder.build();

        ScrubProcessingAction scrubProcessingAction = new ScrubProcessingAction();
        StringBuilder logHolder = new StringBuilder();
        Map<String, Object> scrubberSettings = new HashMap<>();
        scrubberSettings.put("approvalIdCleanup", true);
        scrubberSettings.put("approvalIdCleanupRemoveApprovalId", true);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("scrubberSettings", scrubberSettings);
        Substance result = scrubProcessingAction.process(startingSubstance, null, parameters, logHolder::append);
        Assertions.assertNull(result.approvalID);
    }

    @Test
    public void testPushApprovalIdToCode() throws Exception {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder.setStructureWithDefaultReference("CCCCCCN");
        chemicalSubstanceBuilder.addName("hexane amine");
        Principal admin = principalRepository.findDistinctByUsernameIgnoreCase("admin");
        String dummyApprovalID = "123456789W";

        chemicalSubstanceBuilder.setApproval(admin, new Date(), dummyApprovalID);

        UUID startingSubstanceUuid = UUID.randomUUID();
        chemicalSubstanceBuilder.setUUID(startingSubstanceUuid);
        ChemicalSubstance startingSubstance = chemicalSubstanceBuilder.build();

        String codeSystem = "ApprovalCode";
        ScrubProcessingAction scrubProcessingAction = new ScrubProcessingAction();
        StringBuilder logHolder = new StringBuilder();
        Map<String, Object> scrubberSettings = new HashMap<>();
        scrubberSettings.put("approvalIdCleanup", true);
        scrubberSettings.put("approvalIdCleanupRemoveApprovalId", true);
        scrubberSettings.put("approvalIdCleanupCopyApprovalIdToCode", true);
        scrubberSettings.put("approvalIdCleanupApprovalIdCodeSystem", codeSystem);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("scrubberSettings", scrubberSettings);

        Substance result = scrubProcessingAction.process(startingSubstance, null, parameters, logHolder::append);
        Assertions.assertNull(result.approvalID);

        String codeValue = result.codes.stream().filter(c -> c.codeSystem.equals(codeSystem)).findFirst().get().code;
        Assertions.assertEquals(dummyApprovalID, codeValue);
    }

    @Test
    public void testReplaceAuditUser() throws Exception {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder.setStructureWithDefaultReference("CCCCCCN");
        chemicalSubstanceBuilder.addName("hexane amine");
        Principal admin = principalRepository.findDistinctByUsernameIgnoreCase("admin");
        chemicalSubstanceBuilder.setCreatedBy(admin);

        UUID startingSubstanceUuid = UUID.randomUUID();
        chemicalSubstanceBuilder.setUUID(startingSubstanceUuid);
        ChemicalSubstance startingSubstance = chemicalSubstanceBuilder.build();

        ScrubProcessingAction scrubProcessingAction = new ScrubProcessingAction();
        StringBuilder logHolder = new StringBuilder();
        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> scrubberSettings = new HashMap<>();
        scrubberSettings.put("auditInformationCleanup", true);
        //scrubberSettings.put("auditInformationCleanupDeidentifyAuditUser", true);
        scrubberSettings.put("auditInformationCleanupNewAuditorValue", registrarUserName);

        parameters.put("scrubberSettings", scrubberSettings);

        Substance result = scrubProcessingAction.process(startingSubstance, null, parameters, logHolder::append);
        Assertions.assertNull(result.approvalID);

        String outputUser = result.createdBy.username;
        Assertions.assertEquals(registrarUserName.toUpperCase(), outputUser.toUpperCase());
    }

}