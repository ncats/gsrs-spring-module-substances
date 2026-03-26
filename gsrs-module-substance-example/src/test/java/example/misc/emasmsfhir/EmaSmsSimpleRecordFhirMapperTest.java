package example.misc.emasmsfhir;

import ca.uhn.fhir.context.FhirContext;
import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.misc.emasmsfhir.EmaSmsSimpleRecord;
import gsrs.module.substance.misc.emasmsfhir.EmaSmsSimpleRecordFhirMapper;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import static org.junit.Assert.assertEquals;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@Import(ApplicationContext.class)
public class EmaSmsSimpleRecordFhirMapperTest {
@Autowired
private ApplicationContext applicationContext;
    @Test
    public void fhir1Test() {

        SubstanceBuilder sb = new SubstanceBuilder();
        Code code1 = new Code();
        code1.codeSystem="EVMPD";
        code1.setCode("XYZ");

        Substance gsrsSubstance = new SubstanceBuilder()
                .asChemical()
                .setStructureWithDefaultReference("CCC1CCCC1")
                .addName("Test Guy")
                .addReflexiveActiveMoietyRelationship()
                .addCode("EVMPD", "XYZ")
                .build();

        FhirContext ctx = FhirContext.forR5();
        EmaSmsSimpleRecordFhirMapper emaSmsSimpleRecordFhirMapper =  new EmaSmsSimpleRecordFhirMapper();
        EmaSmsSimpleRecord emaSmsRecord = emaSmsSimpleRecordFhirMapper.generateEmaSmsSimpleRecordFromSubstance(sb.build());
        System.out.println(emaSmsRecord.toString());

        // Failing the problem seems to be in substance builder addCode
        // but works when I run through app/controller
        assertEquals(emaSmsRecord.getEvCode().getValue(), "XYZ");
    }
}
