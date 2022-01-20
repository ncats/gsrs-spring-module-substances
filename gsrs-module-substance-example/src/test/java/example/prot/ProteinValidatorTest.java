package example.prot;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.*;
import ix.ginas.utils.ProteinUtils;
import ix.ginas.utils.validation.validators.ProteinValidator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author mitch miller
 */
@WithMockUser(username = "admin", roles = "Admin")
public class ProteinValidatorTest extends AbstractSubstanceJpaEntityTest
{

    @Autowired
    private TestGsrsValidatorFactory factory;

    public ProteinValidatorTest() {
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

    private boolean configured = false;

    @BeforeEach
    public void setup() {
        if (!configured) {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(ProteinValidator.class);
            config.setNewObjClass(ProteinSubstance.class);

            factory.addValidator("substances", config);
            configured = true;
        }
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testProteinSimpleValidation() throws Exception {
        ProteinSubstance proteinIF95YG4W0P = getProteinFromFile();
        JsonNode toSubmit = proteinIF95YG4W0P.toFullJsonNode();
        //System.out.println("read in protein file and converted to JsonNode");
        ValidationResponse response = substanceEntityService.validateEntity(toSubmit);
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();

        assertTrue(s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("Protein has no molecular formula property"))
                .findAny().isPresent());

        ProteinSubstance validatedProtein = (ProteinSubstance) SubstanceBuilder.from(toSubmit).build();
        List<Property> formulaProperties = ProteinUtils.getMolFormulaProperties(validatedProtein);
        //assertFalse(formulaProperties.isEmpty());
    }

    @Test
    public void testNoSubunitsComplete() throws Exception {
        ProteinSubstanceBuilder proteinSubstanceBuilder = new ProteinSubstanceBuilder();
        String name = "empty protein";
        proteinSubstanceBuilder.setDefinition(Substance.SubstanceDefinitionType.PRIMARY, Substance.SubstanceDefinitionLevel.COMPLETE);
        proteinSubstanceBuilder.addName(name);
        Protein protein = new Protein();//empty!
        proteinSubstanceBuilder.setProtein(protein);
        ProteinSubstance proteinSubstance = proteinSubstanceBuilder.build();
        JsonNode toSubmit = proteinSubstance.toFullJsonNode();
        //System.out.println("testNoSubunits built protein and converted to JsonNode");
        ValidationResponse response = substanceEntityService.validateEntity(toSubmit);
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        assertTrue(s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("Complete protein substance must have at least one Subunit element. Please add a subunit, or mark as incomplete"))
                .findAny().isPresent());

    }

    @Test
    public void testNoSubunitsIncomplete() throws Exception {
        ProteinSubstanceBuilder proteinSubstanceBuilder = new ProteinSubstanceBuilder();
        String name = "empty protein";
        proteinSubstanceBuilder.setDefinition(Substance.SubstanceDefinitionType.PRIMARY, Substance.SubstanceDefinitionLevel.INCOMPLETE);
        proteinSubstanceBuilder.addName(name);
        Protein protein = new Protein();//empty!
        proteinSubstanceBuilder.setProtein(protein);
        ProteinSubstance proteinSubstance = proteinSubstanceBuilder.build();
        JsonNode toSubmit = proteinSubstance.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(toSubmit);
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        assertTrue(s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("Having no subunits is allowed but discouraged for incomplete protein records"))
                .findAny().isPresent());
    }

    @Test
    public void testBlankSubunitComplete() throws Exception {
        ProteinSubstanceBuilder proteinSubstanceBuilder = new ProteinSubstanceBuilder();
        String name = "protein with no sequence";
        proteinSubstanceBuilder.setDefinition(Substance.SubstanceDefinitionType.PRIMARY, Substance.SubstanceDefinitionLevel.COMPLETE);
        proteinSubstanceBuilder.addName(name);
        Protein protein = new Protein();
        Subunit subunit1 = new Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        proteinSubstanceBuilder.setProtein(protein);
        ProteinSubstance proteinSubstance = proteinSubstanceBuilder.build();
        JsonNode toSubmit = proteinSubstance.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(toSubmit);
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        assertTrue(s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("subunit at position "))
                .findAny().isPresent());

    }

    @Test
    public void testBlankSubunitsIncomplete() throws Exception {
        ProteinSubstanceBuilder proteinSubstanceBuilder = new ProteinSubstanceBuilder();
        String name = "protein with empty subunit";
        proteinSubstanceBuilder.setDefinition(Substance.SubstanceDefinitionType.PRIMARY, Substance.SubstanceDefinitionLevel.INCOMPLETE);
        proteinSubstanceBuilder.addName(name);
        Protein protein = new Protein();
        Subunit subunit1 = new Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        proteinSubstanceBuilder.setProtein(protein);
        ProteinSubstance proteinSubstance = proteinSubstanceBuilder.build();
        JsonNode toSubmit = proteinSubstance.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(toSubmit);
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        assertTrue(s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("subunit at position 1 is blank"))
                .findAny().isPresent());
    }

    @Test
    public void testDisulfideLinkNoSites() throws Exception {
        ProteinSubstanceBuilder proteinSubstanceBuilder = new ProteinSubstanceBuilder();
        String name = "protein with subunit";
        proteinSubstanceBuilder.setDefinition(Substance.SubstanceDefinitionType.PRIMARY, Substance.SubstanceDefinitionLevel.INCOMPLETE);
        proteinSubstanceBuilder.addName(name);
        Protein protein = new Protein();
        Subunit subunit1 = new Subunit();
        subunit1.sequence="CAAC";
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        DisulfideLink link1= new DisulfideLink();
        link1.setSites(new ArrayList<>());
        List<DisulfideLink> links = Arrays.asList(link1);
        proteinSubstanceBuilder.setProtein(protein);
        proteinSubstanceBuilder.setDisulfideLinks(links);
        ProteinSubstance proteinSubstance = proteinSubstanceBuilder.build();
        JsonNode toSubmit = proteinSubstance.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(toSubmit);
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        assertTrue(s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("Disulfide Link")  && m.contains("should have 2"))
                .findAny().isPresent());
    }

    @Test
    public void testDisulfideLinkNonCSites() throws Exception {
        ProteinSubstanceBuilder proteinSubstanceBuilder = new ProteinSubstanceBuilder();
        String name = "protein with subunit";
        proteinSubstanceBuilder.setDefinition(Substance.SubstanceDefinitionType.PRIMARY, Substance.SubstanceDefinitionLevel.INCOMPLETE);
        proteinSubstanceBuilder.addName(name);
        Protein protein = new Protein();
        Subunit subunit1 = new Subunit();
        subunit1.sequence="CAACT";
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        DisulfideLink link1= new DisulfideLink();
        Site site1= new Site(1,2);
        Site site2= new Site(1,3);
        link1.setSites(Arrays.asList(site1, site2));
        List<DisulfideLink> links = Arrays.asList(link1);
        proteinSubstanceBuilder.setProtein(protein);
        proteinSubstanceBuilder.setDisulfideLinks(links);
        ProteinSubstance proteinSubstance = proteinSubstanceBuilder.build();
        JsonNode toSubmit = proteinSubstance.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(toSubmit);
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        assertTrue(s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("in disulfide link is not a Cysteine, found"))
                .findAny().isPresent());
    }

    @Test
    public void testDisulfideLinkInvalidSites() throws Exception {
        ProteinSubstanceBuilder proteinSubstanceBuilder = new ProteinSubstanceBuilder();
        String name = "protein with subunit";
        proteinSubstanceBuilder.setDefinition(Substance.SubstanceDefinitionType.PRIMARY, Substance.SubstanceDefinitionLevel.INCOMPLETE);
        proteinSubstanceBuilder.addName(name);
        Protein protein = new Protein();
        Subunit subunit1 = new Subunit();
        subunit1.sequence="CAACT";
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        DisulfideLink link1= new DisulfideLink();
        Site site1= new Site(1,9);
        Site site2= new Site(1,120);
        link1.setSites(Arrays.asList(site1, site2));
        List<DisulfideLink> links = Arrays.asList(link1);
        proteinSubstanceBuilder.setProtein(protein);
        proteinSubstanceBuilder.setDisulfideLinks(links);
        ProteinSubstance proteinSubstance = proteinSubstanceBuilder.build();
        JsonNode toSubmit = proteinSubstance.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(toSubmit);
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        assertTrue(s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("does not exist"))
                .findAny().isPresent());
    }

    @Test
    public void testDisulfideLinkValidSites() throws Exception {
        ProteinSubstanceBuilder proteinSubstanceBuilder = new ProteinSubstanceBuilder();
        String name = "protein with subunit";
        proteinSubstanceBuilder.setDefinition(Substance.SubstanceDefinitionType.PRIMARY, Substance.SubstanceDefinitionLevel.INCOMPLETE);
        proteinSubstanceBuilder.addName(name);
        Protein protein = new Protein();
        Subunit subunit1 = new Subunit();
        subunit1.sequence="CAACT";
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        DisulfideLink link1= new DisulfideLink();
        Site site1= new Site(1,1);
        Site site2= new Site(1,4);
        link1.setSites(Arrays.asList(site1, site2));
        List<DisulfideLink> links = Arrays.asList(link1);
        proteinSubstanceBuilder.setProtein(protein);
        proteinSubstanceBuilder.setDisulfideLinks(links);
        ProteinSubstance proteinSubstance = proteinSubstanceBuilder.build();
        JsonNode toSubmit = proteinSubstance.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(toSubmit);
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        assertFalse(s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("does not exist"))
                .findAny().isPresent());
    }

    @Test
    public void testUnknownResidues() throws Exception {
        ProteinSubstanceBuilder proteinSubstanceBuilder = new ProteinSubstanceBuilder();
        String name = "protein with subunit";
        proteinSubstanceBuilder.setDefinition(Substance.SubstanceDefinitionType.PRIMARY, Substance.SubstanceDefinitionLevel.INCOMPLETE);
        proteinSubstanceBuilder.addName(name);
        Protein protein = new Protein();
        Subunit subunit1 = new Subunit();
        subunit1.sequence="CAACTabC#";
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        proteinSubstanceBuilder.setProtein(protein);
        ProteinSubstance proteinSubstance = proteinSubstanceBuilder.build();
        JsonNode toSubmit = proteinSubstance.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(toSubmit);
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        assertTrue(s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("Protein has unknown amino acid residues"))
                .findAny().isPresent());
    }

    
    @Test
    public void testNoMwPropertyWarning() throws Exception {
        ProteinSubstanceBuilder proteinSubstanceBuilder = new ProteinSubstanceBuilder();
        String name = "protein with subunit";
        proteinSubstanceBuilder.setDefinition(Substance.SubstanceDefinitionType.PRIMARY, Substance.SubstanceDefinitionLevel.INCOMPLETE);
        proteinSubstanceBuilder.addName(name);
        Protein protein = new Protein();
        Subunit subunit1 = new Subunit();
        subunit1.sequence="CAACTFRL";
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        proteinSubstanceBuilder.setProtein(protein);
        ProteinSubstance proteinSubstance = proteinSubstanceBuilder.build();
        JsonNode toSubmit = proteinSubstance.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(toSubmit);
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        assertTrue(s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("Protein has no molecular weight, defaulting to calculated value of: 884"))
                .findAny().isPresent());
    }


       
    @Test
    public void testMwPropertyUnknownAAWarning() throws Exception {
        ProteinSubstanceBuilder proteinSubstanceBuilder = new ProteinSubstanceBuilder();
        String name = "protein with subunit";
        proteinSubstanceBuilder.setDefinition(Substance.SubstanceDefinitionType.PRIMARY, Substance.SubstanceDefinitionLevel.INCOMPLETE);
        proteinSubstanceBuilder.addName(name);
        Protein protein = new Protein();
        Subunit subunit1 = new Subunit();
        subunit1.sequence="CAACTFRLB";
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        proteinSubstanceBuilder.setProtein(protein);
        ProteinSubstance proteinSubstance = proteinSubstanceBuilder.build();
        JsonNode toSubmit = proteinSubstance.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(toSubmit);
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        //s1.peek(m-> System.out.println( m.getMessage());)
        assertTrue(s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("Calculated protein weight questionable, due to unknown amino acid residues: [B]"))
                .findAny().isPresent());
    }


    @Test
    public void testMwPropertyOff() throws Exception {
        ProteinSubstanceBuilder proteinSubstanceBuilder = new ProteinSubstanceBuilder();
        String name = "protein with subunit";
        proteinSubstanceBuilder.setDefinition(Substance.SubstanceDefinitionType.PRIMARY, Substance.SubstanceDefinitionLevel.INCOMPLETE);
        proteinSubstanceBuilder.addName(name);
        Protein protein = new Protein();
        Subunit subunit1 = new Subunit();
        subunit1.sequence="CAACTFRL";
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        proteinSubstanceBuilder.setProtein(protein);
        Property mwProperty = new Property();
        mwProperty.setName("MOL_WEIGHT:NUMBER AVERAGE");
        Amount value = new Amount();
        value.average =900.0;
        mwProperty.setValue(value);
        proteinSubstanceBuilder.addProperty(mwProperty);
        ProteinSubstance proteinSubstance = proteinSubstanceBuilder.build();
        JsonNode toSubmit = proteinSubstance.toFullJsonNode();
        ValidationResponse response = substanceEntityService.validateEntity(toSubmit);
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        //s1.peek(m-> System.out.println( m.getMessage());)
        assertTrue(s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("Calculated weight") && m.contains("off of given weight"))
                .findAny().isPresent());
    }


    private ProteinSubstance getProteinFromFile() {
        try {
            File proteinFile = new ClassPathResource("testJSON/IF95YG4W0P.json").getFile();
            ProteinSubstanceBuilder builder = SubstanceBuilder.from(proteinFile);
            return builder.build();
        } catch (IOException ex) {
            System.err.println("Error reading protein file: " + ex.getMessage());
            ex.printStackTrace();
            //Logger.getLogger(example.substance.ProtCalculationTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
