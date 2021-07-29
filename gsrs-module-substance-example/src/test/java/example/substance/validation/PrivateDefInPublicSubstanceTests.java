package example.substance.validation;

import ix.core.models.Group;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import static org.junit.Assert.*;
import com.fasterxml.jackson.databind.JsonNode;
import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationMessage.MESSAGE_TYPE;
import ix.ginas.models.v1.Protein;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Subunit;
import ix.ginas.models.v1.StructurallyDiverse;
import ix.ginas.models.v1.StructurallyDiverseSubstance;
import ix.ginas.utils.validation.validators.DefinitionalReferenceValidator;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Stream;

/**
 *
 * @author mitch
 */
public class PrivateDefInPublicSubstanceTests extends AbstractSubstanceJpaEntityTest
{

    private static boolean addedValidator = false;
    @Autowired
    private TestGsrsValidatorFactory factory;

    public PrivateDefInPublicSubstanceTests() {
    }

    @BeforeClass
    public static void setUpClass() {

    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @BeforeEach
    public void setup() {
        //make sure validation happens only once per substance
        if (!addedValidator) {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(DefinitionalReferenceValidator.class);
            config.setNewObjClass(Substance.class);
            factory.addValidator("substances", config);
            addedValidator = true;
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void testPublicSubstanceWithPrivateChemical() throws Exception {
        String molfile = "  ACCLDraw07022110302D\n"
                + "\n"
                + "  3  2  0  0  0  0  0  0  0  0999 V2000\n"
                + "    2.9688   -5.0625    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "    3.9916   -4.4719    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "    3.9916   -3.2905    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "  1  2  1  0  0  0  0\n"
                + "  2  3  1  0  0  0  0\n"
                + "M  END";
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        GinasChemicalStructure structure = new GinasChemicalStructure();
        Reference defRef = new Reference();
        defRef.publicDomain = false;
        structure.addReference(defRef);
        structure.molfile = molfile;
        structure.setAccess(Collections.singleton(new Group("registrars")));
        ChemicalSubstance ethanol = builder.setStructure(structure)
                .addName("Ethanol")
                .addCode("CAS", "64-17-5")
                .setStructure(structure)
                .build();
        ethanol.uuid = UUID.randomUUID();
        JsonNode ethanolNode = ethanol.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(ethanolNode);
        List<ValidationMessage> messages = response.getValidationMessages();
        
        
        assertEquals("expecting no errors when private structure is attached to public chemical",
                0, messages.stream().filter(m -> m.getMessageType() == MESSAGE_TYPE.ERROR).count());
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void testPublicSubstanceWithPublicChemical() throws Exception {
        String molfile = "  ACCLDraw07022110302D\n"
                + "\n"
                + "  3  2  0  0  0  0  0  0  0  0999 V2000\n"
                + "    2.9688   -5.0625    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "    3.9916   -4.4719    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "    3.9916   -3.2905    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "  1  2  1  0  0  0  0\n"
                + "  2  3  1  0  0  0  0\n"
                + "M  END";
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        GinasChemicalStructure structure = new GinasChemicalStructure();
        Reference defRef = new Reference();
        defRef.publicDomain = false;
        structure.addReference(defRef);
        structure.molfile = molfile;
        //structure.setAccess(Collections.singleton(new Group("registrars")));
        ChemicalSubstance ethylamine = builder.setStructure(structure)
                .addName("Ethylamine")
                .addCode("CAS", "75-04-7")
                .setStructure(structure)
                .build();
        ethylamine.uuid = UUID.randomUUID();
        JsonNode ethylamineNode = ethylamine.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(ethylamineNode);
        Stream<ValidationMessage> messages = response.getValidationMessages().stream();
        assertTrue("public definition of public substance must have a public ref", messages.filter(m -> m.getMessageType() == MESSAGE_TYPE.ERROR).count() > 0);
    }

    @Test
    public void testPublicSubstanceWithPrivateProtein() throws Exception {
        Protein protein = new Protein();
        Subunit subunit1 = new Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        subunit1.sequence
                = "MKKNYNPKDIEEHLYNFWEKNGFFKPNNNLNKPAFCIMMPPPNITGNLHMGHAFQQTIMD"
                + "ILIRYNRMQGKNTLWQVGTDHAGIATQILIERQIFSEERKTKKDYSRNDFIKKIWKWKKK"
                + "SNFSVKKQMKRLGNSVDWDREKFTLDPDISNSVKEAFIILYKNNLIYQKKRLVHWDSKLE"
                + "TVISDLEVEHRLIKSKKWFIRYPIIKNIKNINIEYLLVATTRPETLLGDTALAINPKDDK"
                + "YNHLIGQSVICPIVNRIIPIIADHYADMNKDTGCVKITPGHDFNDYEVGQRHKLPMINIF"
                + "TFNGKIKSNFSIYDYQGSKSNFYDSSIPTEFQNLDILSARKKIIYEIEKLGLLEKIEECN"
                + "FFTPYSERSGVIIQPMLTNQWYLKTSHLSQSAIDVVREKKIKFIPNQYKSMYLSWMNNIE"
                + "DWCISRQLWWGHQIPVWYDDKKNIYVGHSEKKIREEYNISDDMILNQDNDVLDTWFSSGL"
                + "WTFSTLGWPEKTEFLKIFHSTDVLVSGFDIIFFWIARMIMLTMYLVKDSYGNPQIPFKDV"
                + "YITGLIRDEEGKKMSKSKGNVIDPIDMIDGISLNELIEKRTSNLLQPHLSQKIRYHTIKQ"
                + "FPNGISATGTDALRFTFSALASNTRDIQWDMNRLKGYRNFCNKLWNASRFVLKNTKDHDY"
                + "FNFSVNDNMLLINKWILIKFNNTVKSYRNSLDSYRFDIAANILYDFIWNVFCDWYLEFVK"
                + "SVIKSGSYQDIYFTKNVLIHVLELLLRLSHPIMPFITEAIWQRVKIIKHIKDRTIMLQSF"
                + "PEYNDQLFDKSTLSNINWIKKIIIFIRNTRSKMNISSTKLLSLFLKNINSEKKKVIQENK"
                + "FILKNIASLEKISILSKQDDEPCLSLKEIIDGVDILVPVLKAIDKEIELKRLNKEIEKIK"
                + "SKMLISEKKMSNQDFLSYAPKNIIDKEIKKLKSLNEIYLTLSQQLESLHDAFCKKNKIFN";

        protein.setAccess(Collections.singleton(new Group("registrars")));
        Reference defRef = new Reference();
        defRef.publicDomain = false;
        protein.addReference(defRef);
        ProteinSubstance proteinSubstance = new ProteinSubstance();
        proteinSubstance.uuid = UUID.randomUUID();
        proteinSubstance.setProtein(protein);
        JsonNode proteinNode = proteinSubstance.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(proteinNode);
        Stream<ValidationMessage> messages = response.getValidationMessages().stream();
        assertEquals("private definition of public substance may have a public or restricted ref",
                0, messages.filter(m -> m.getMessageType() == MESSAGE_TYPE.ERROR).count());
    }

    @Test
    public void testPublicSubstanceWithPublicProtein() throws Exception {
        Protein protein = new Protein();
        Subunit subunit1 = new Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        subunit1.sequence
                = "MKKNYNPKDIEEHLYNFWEKNGFFKPNNNLNKPAFCIMMPPPNITGNLHMGHAFQQTIMD"
                + "ILIRYNRMQGKNTLWQVGTDHAGIATQILIERQIFSEERKTKKDYSRNDFIKKIWKWKKK"
                + "SNFSVKKQMKRLGNSVDWDREKFTLDPDISNSVKEAFIILYKNNLIYQKKRLVHWDSKLE"
                + "TVISDLEVEHRLIKSKKWFIRYPIIKNIKNINIEYLLVATTRPETLLGDTALAINPKDDK"
                + "YNHLIGQSVICPIVNRIIPIIADHYADMNKDTGCVKITPGHDFNDYEVGQRHKLPMINIF"
                + "TFNGKIKSNFSIYDYQGSKSNFYDSSIPTEFQNLDILSARKKIIYEIEKLGLLEKIEECN"
                + "FFTPYSERSGVIIQPMLTNQWYLKTSHLSQSAIDVVREKKIKFIPNQYKSMYLSWMNNIE"
                + "DWCISRQLWWGHQIPVWYDDKKNIYVGHSEKKIREEYNISDDMILNQDNDVLDTWFSSGL"
                + "WTFSTLGWPEKTEFLKIFHSTDVLVSGFDIIFFWIARMIMLTMYLVKDSYGNPQIPFKDV"
                + "YITGLIRDEEGKKMSKSKGNVIDPIDMIDGISLNELIEKRTSNLLQPHLSQKIRYHTIKQ"
                + "FPNGISATGTDALRFTFSALASNTRDIQWDMNRLKGYRNFCNKLWNASRFVLKNTKDHDY"
                + "FNFSVNDNMLLINKWILIKFNNTVKSYRNSLDSYRFDIAANILYDFIWNVFCDWYLEFVK"
                + "SVIKSGSYQDIYFTKNVLIHVLELLLRLSHPIMPFITEAIWQRVKIIKHIKDRTIMLQSF"
                + "PEYNDQLFDKSTLSNINWIKKIIIFIRNTRSKMNISSTKLLSLFLKNINSEKKKVIQENK"
                + "FILKNIASLEKISILSKQDDEPCLSLKEIIDGVDILVPVLKAIDKEIELKRLNKEIEKIK"
                + "SKMLISEKKMSNQDFLSYAPKNIIDKEIKKLKSLNEIYLTLSQQLESLHDAFCKKNKIF";

        //protein.setAccess(Collections.singleton(new Group("registrars")));
        Reference defRef = new Reference();
        defRef.publicDomain = false;
        protein.addReference(defRef);
        ProteinSubstance proteinSubstance = new ProteinSubstance();
        proteinSubstance.setProtein(protein);
        proteinSubstance.uuid = UUID.randomUUID();
        JsonNode proteinNode = proteinSubstance.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(proteinNode);
        Stream<ValidationMessage> messages = response.getValidationMessages().stream();
        assertEquals("public definition of public substance must have a public ref",
                1, messages.filter(m -> m.getMessageType() == MESSAGE_TYPE.ERROR).count());
    }

    @Test
    public void testPublicSubstanceWithPrivateStrDiv() throws Exception {
        StructurallyDiverse strDiv = new StructurallyDiverse();
        strDiv.sourceMaterialClass = "ORGANISM";
        strDiv.sourceMaterialType = "PLANT";
        strDiv.organismFamily = "ACANTHACEAE";
        strDiv.organismGenus = "JUSTICIA";
        strDiv.organismSpecies = "ADHATODA";
        strDiv.organismAuthor = "L.";
        strDiv.setAccess(Collections.singleton(new Group("registrars")));
        Reference defRef = new Reference();
        defRef.publicDomain = false;
        strDiv.addReference(defRef);

        StructurallyDiverseSubstance substance = new StructurallyDiverseSubstance();
        substance.uuid = UUID.randomUUID();
        substance.structurallyDiverse = strDiv;

        JsonNode node = substance.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(node);
        Stream<ValidationMessage> messages = response.getValidationMessages().stream();
        assertEquals("private definition of public substance may have a public or restricted ref",
                0, messages.filter(m -> m.getMessageType() == MESSAGE_TYPE.ERROR).count());
    }
    
    @Test
    public void testPublicSubstanceWithPublicStrDiv() throws Exception {
        StructurallyDiverse strDiv = new StructurallyDiverse();
        strDiv.sourceMaterialClass = "ORGANISM";
        strDiv.sourceMaterialType = "PLANT";
        strDiv.organismFamily = "ACANTHACEAE";
        strDiv.organismGenus = "JUSTICIA";
        strDiv.organismSpecies = "ADHATODA";
        strDiv.organismAuthor = "L.";
        //strDiv.setAccess(Collections.singleton(new Group("registrars")));
        Reference defRef = new Reference();
        defRef.publicDomain = false;
        strDiv.addReference(defRef);

        StructurallyDiverseSubstance substance = new StructurallyDiverseSubstance();
        substance.uuid = UUID.randomUUID();
        substance.structurallyDiverse = strDiv;

        JsonNode node = substance.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(node);
        Stream<ValidationMessage> messages = response.getValidationMessages().stream();
        assertEquals("private definition of public substance may have a public or restricted ref",
                1, messages.filter(m -> m.getMessageType() == MESSAGE_TYPE.ERROR).count());
    }

}
