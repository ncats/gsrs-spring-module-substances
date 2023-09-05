package example.substance.chemical;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.service.GsrsEntityService;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.EntityFetcher;
import ix.core.models.Keyword;
import ix.core.models.Structure;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.springframework.security.test.context.support.WithMockUser;
import java.util.UUID;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles="Admin")
class ChemicalChangesTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    protected SubstanceEntityService substanceEntityService;

    @BeforeEach
    public void setup(){
        AutowireHelper.getInstance().autowire(this);
    }

    @Test
    void testStructureChange() throws Exception {
        String molfileBefore ="\n" +
                "  ACCLDraw09052318012D\n" +
                "\n" +
                "  9  9  0  0  1  0  0  0  0  0999 V2000\n" +
                "    2.6022  -10.0969    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    3.6250   -9.5064    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.6478  -10.0969    0.0000 C   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "    4.6478  -11.2781    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    3.6250  -11.8686    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    2.6022  -11.2781    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    5.6704   -9.5066    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
                "    5.6704   -8.3259    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    6.6929  -10.0969    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  1  2  1  0  0  0  0\n" +
                "  3  2  1  0  0  0  0\n" +
                "  4  3  1  0  0  0  0\n" +
                "  5  4  1  0  0  0  0\n" +
                "  1  6  1  0  0  0  0\n" +
                "  6  5  1  0  0  0  0\n" +
                "  3  7  1  0  0  0  0\n" +
                "  7  8  1  1  0  0  0\n" +
                "  7  9  1  0  0  0  0\n" +
                "M  END\n";


        String molfileAfter = "\n" +
                "  ACCLDraw09052318022D\n" +
                "\n" +
                " 10 10  0  0  1  0  0  0  0  0999 V2000\n" +
                "    2.6022  -10.0969    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    3.6250   -9.5064    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.6478  -10.0969    0.0000 C   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "    4.6478  -11.2781    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    3.6250  -11.8686    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    2.6022  -11.2781    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    5.6704   -9.5066    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
                "    5.6704   -8.3259    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    6.6929  -10.0969    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.7154   -9.5066    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  1  2  1  0  0  0  0\n" +
                "  3  2  1  0  0  0  0\n" +
                "  4  3  1  0  0  0  0\n" +
                "  5  4  1  0  0  0  0\n" +
                "  1  6  1  0  0  0  0\n" +
                "  6  5  1  0  0  0  0\n" +
                "  3  7  1  0  0  0  0\n" +
                "  7  8  1  1  0  0  0\n" +
                "  7  9  1  0  0  0  0\n" +
                "  9 10  1  0  0  0  0\n" +
                "M  END\n";

        UUID substanceId= UUID.randomUUID();
        Reference substanceOneReference = new Reference();
        substanceOneReference.docType="CATALOG";
        substanceOneReference.citation="Value specific to substance 1";
        ChemicalSubstanceBuilder builder= new ChemicalSubstanceBuilder();
        builder.setUUID(substanceId);
        GinasChemicalStructure initial = new GinasChemicalStructure();
        initial.molfile=molfileBefore;
        initial.addReference(substanceOneReference);
        builder.setStructure(initial);

        Name name1 = new Name();
        name1.name="(1S)-1-cyclohexylethanol";
        name1.languages.add(new Keyword("en"));
        name1.displayName=true;

        name1.addReference(substanceOneReference);
        builder.addName(name1);
        ChemicalSubstance chemicalSubstance = builder.build();
        GsrsEntityService.CreationResult<Substance> result= substanceEntityService.createEntity(chemicalSubstance.toFullJsonNode());
        Assertions.assertTrue(result.isCreated());

        //retrieve substance
        ChemicalSubstance retrieved = (ChemicalSubstance) EntityFetcher.of(EntityUtils.Key.of(Substance.class, UUID.fromString(substanceId.toString()))).call();
        GinasChemicalStructure retrievedStructure=retrieved.getStructure();
        //update molfile
        retrievedStructure.molfile=molfileAfter;
        retrievedStructure.opticalActivity= Structure.Optical.MINUS;
        retrieved.setStructure(retrievedStructure);
        GsrsEntityService.UpdateResult<Substance> updateResult = substanceEntityService.updateEntity(retrieved.toFullJsonNode());
        Assertions.assertNull(updateResult.getThrowable());
    }

}
