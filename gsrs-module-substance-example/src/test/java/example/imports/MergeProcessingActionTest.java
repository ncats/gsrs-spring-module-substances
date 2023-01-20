package example.imports;

import example.GsrsModuleSubstanceApplication;
import gsrs.dataexchange.processing_actions.MergeProcessingAction;
import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.services.PrincipalServiceImpl;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.models.Keyword;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.NucleicAcidSubstanceBuilder;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class MergeProcessingActionTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private StructureIndexerService indexer;

    @Autowired
    private SubstanceEntityService substanceEntityService;

    @Autowired
    private PrincipalServiceImpl principalService;

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @BeforeEach
    public void clearIndexers() throws IOException {
        indexer.removeAll();
        principalService.clearCache();
    }

    @Test
    public void testMergeNames() throws Exception {

        ChemicalSubstanceBuilder builder= new ChemicalSubstanceBuilder();
        builder.setStructureWithDefaultReference("NCCCCN");
        Name name1 = new Name();
        name1.name="putrecine";
        name1.languages.add(new Keyword("en"));
        name1.displayName=true;
        builder.addName(name1);

        Name name2 = new Name();
        name2.name="1,4 diaminobutane";
        name2.languages.add(new Keyword("de"));
        name2.displayName=false;
        builder.addName(name2);
        builder.addName("Stuff");
        builder.addCode("CHEMBL", "CHEMBL46257");
        ChemicalSubstance chemical1 = builder.build();

        ChemicalSubstanceBuilder builder2= new ChemicalSubstanceBuilder();
        builder2.setStructureWithDefaultReference("OCCCCO");
        builder2.addName("1,4-BUTANEDIOL");
        builder2.addCode("CHEMBL", "CHEMBL171623");
        ChemicalSubstance chemical2 = builder2.build();

        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeNames", "true");

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( chemical1, chemical2, parms, logger);
        Assertions.assertEquals(4,output.names.size());
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);
        Assertions.assertTrue(chemical1.names.stream().allMatch(n->output.names.stream().anyMatch(n2->n.name.equals(n2.name) && n.displayName== n2.displayName &&
                n.languages.stream().allMatch(l1-> n2.languages.stream().anyMatch(l2->l1.term.equals(l2.term))))));
    }


    @Test
    public void testMergeNamesIncludeReferences() throws Exception {

        ChemicalSubstanceBuilder builder= new ChemicalSubstanceBuilder();
        builder.setStructureWithDefaultReference("NCCCCN");
        Name name1 = new Name();
        name1.name="putrecine";
        name1.languages.add(new Keyword("en"));
        name1.displayName=true;
        Reference substanceOneReference = new Reference();
        substanceOneReference.docType="CATALOG";
        substanceOneReference.citation="Value specific to substance 1";
        name1.addReference(substanceOneReference);
        builder.addName(name1);
        builder.addReference(substanceOneReference);

        Name name2 = new Name();
        name2.name="1,4 diaminobutane";
        name2.languages.add(new Keyword("de"));
        name2.displayName=false;
        builder.addName(name2);
        builder.addName("Stuff");
        builder.addCode("CHEMBL", "CHEMBL46257");
        ChemicalSubstance chemical1 = builder.build();

        ChemicalSubstanceBuilder builder2= new ChemicalSubstanceBuilder();
        builder2.setStructureWithDefaultReference("OCCCCO");
        builder2.addName("1,4-BUTANEDIOL");
        builder2.addCode("CHEMBL", "CHEMBL171623");
        ChemicalSubstance chemical2 = builder2.build();

        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeNames", "true");

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( chemical1, chemical2, parms, logger);
        Assertions.assertEquals(4,output.names.size());
        Assertions.assertTrue(chemical1.names.stream().allMatch(n->output.names.stream().anyMatch(n2->n.name.equals(n2.name) && n.displayName== n2.displayName &&
                n.languages.stream().allMatch(l1-> n2.languages.stream().anyMatch(l2->l1.term.equals(l2.term))))));
        Assertions.assertTrue(output.references.stream().anyMatch(r->r.citation.equals(substanceOneReference.citation) && r.docType.equals(substanceOneReference.docType)));
    }

    @Test
    public void testMergeNamesIncludeReferencesWhenBlocked() throws Exception {

        ChemicalSubstanceBuilder builder= new ChemicalSubstanceBuilder();
        builder.setStructureWithDefaultReference("NCCCCN");
        Name name1 = new Name();
        name1.name="putrecine";
        name1.languages.add(new Keyword("en"));
        name1.displayName=true;
        Reference substanceOneReference = new Reference();
        substanceOneReference.docType="CATALOG";
        substanceOneReference.citation="Value specific to substance 1";
        name1.addReference(substanceOneReference);
        builder.addName(name1);
        builder.addReference(substanceOneReference);

        Name name2 = new Name();
        name2.name="1,4 diaminobutane";
        name2.languages.add(new Keyword("de"));
        name2.displayName=false;
        builder.addName(name2);
        builder.addName("Stuff");
        builder.addCode("CHEMBL", "CHEMBL46257");
        ChemicalSubstance chemical1 = builder.build();

        ChemicalSubstanceBuilder builder2= new ChemicalSubstanceBuilder();
        builder2.setStructureWithDefaultReference("OCCCCO");
        builder2.addName("1,4-BUTANEDIOL");
        builder2.addCode("CHEMBL", "CHEMBL171623");
        ChemicalSubstance chemical2 = builder2.build();

        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeNames", "true");
        parms.put("SkipLevelingReferences", "true");

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( chemical1, chemical2, parms, logger);
        Assertions.assertEquals(4,output.names.size());
        Assertions.assertTrue(chemical1.names.stream().allMatch(n->output.names.stream().anyMatch(n2->n.name.equals(n2.name) && n.displayName== n2.displayName &&
                n.languages.stream().allMatch(l1-> n2.languages.stream().anyMatch(l2->l1.term.equals(l2.term))))));
        Assertions.assertFalse(output.references.stream().anyMatch(r->r.citation.equals(substanceOneReference.citation) && r.docType.equals(substanceOneReference.docType)));
    }

    @Test
    public void testMergeNamesHTML() {

        ChemicalSubstanceBuilder builder= new ChemicalSubstanceBuilder();
        builder.setStructureWithDefaultReference("NCCCCN");
        Name name1 = new Name();
        name1.name="putrecine";
        name1.languages.add(new Keyword("en"));
        name1.displayName=true;
        builder.addName(name1);

        Name name2 = new Name();
        name2.name="1,4 <i>interesting</i> diaminobutane";
        name2.languages.add(new Keyword("de"));
        name2.displayName=false;
        builder.addName(name2);
        builder.addName("Stuff");
        builder.addCode("CHEMBL", "CHEMBL46257");
        ChemicalSubstance chemical1 = builder.build();

        ChemicalSubstanceBuilder builder2= new ChemicalSubstanceBuilder();
        builder2.setStructureWithDefaultReference("OCCCCO");
        Name nameHtml2 = new Name();
        nameHtml2.name="1,4-<i>idea</i> BUTANEDIOL";
        nameHtml2.displayName=true;
        nameHtml2.languages.add(new Keyword("en"));
        builder2.addName(nameHtml2);
        builder2.addCode("CHEMBL", "CHEMBL171623");
        ChemicalSubstance chemical2 = builder2.build();

        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeNames", "true");

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( chemical1, chemical2, parms, logger);
        Assertions.assertEquals(4,output.names.size());
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);
        Assertions.assertTrue(chemical1.names.stream().allMatch(n->output.names.stream().anyMatch(n2->n.name.equals(n2.name) && n.displayName== n2.displayName &&
                n.languages.stream().allMatch(l1-> n2.languages.stream().anyMatch(l2->l1.term.equals(l2.term))))));
    }

    /*
    The same name has been added to 2 substances; automatic filtering prevents it from being added twice to the output
     */
    @Test
    public void testMergeNamesDuplicates() throws Exception {

        ChemicalSubstanceBuilder builder= new ChemicalSubstanceBuilder();
        builder.setStructureWithDefaultReference("NCCCCN");
        Name name1 = new Name();
        name1.name="putrecine";
        name1.languages.add(new Keyword("en"));
        name1.displayName=true;
        builder.addName(name1);

        Name name2 = new Name();
        name2.name="1,4 diaminobutane";
        name2.languages.add(new Keyword("de"));
        name2.displayName=false;
        builder.addName(name2);
        builder.addName("Stuff");
        builder.addCode("CHEMBL", "CHEMBL46257");
        ChemicalSubstance chemical1 = builder.build();

        ChemicalSubstanceBuilder builder2= new ChemicalSubstanceBuilder();
        builder2.setStructureWithDefaultReference("OCCCCO");
        builder2.addName("1,4-BUTANEDIOL");
        EntityUtils.EntityInfo<Name> eics= EntityUtils.getEntityInfoFor(Name.class);
        Name name2Clone =eics.fromJson(name2.toJson());
        builder2.addName(name2Clone);
        builder2.addCode("CHEMBL", "CHEMBL171623");
        ChemicalSubstance chemical2 = builder2.build();

        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeNames", "true");

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( chemical1, chemical2, parms, logger);
        Assertions.assertEquals(4,output.names.size());
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);
        Assertions.assertTrue(chemical1.names.stream().allMatch(n->output.names.stream().anyMatch(n2->n.name.equals(n2.name) && n.displayName== n2.displayName &&
                n.languages.stream().allMatch(l1-> n2.languages.stream().anyMatch(l2->l1.term.equals(l2.term))))));
    }

    @Test
    public void testMergeCodes() {

        ChemicalSubstanceBuilder builder= new ChemicalSubstanceBuilder();
        builder.setStructureWithDefaultReference("NCCCCN");
        builder.addName("putrecine");
        builder.addCode("CHEMBL", "CHEMBL46257");
        builder.addCode("CHEBI", "CHEBI46257");
        builder.addCode("CAS", "BOGUS");
        ChemicalSubstance chemical1 = builder.build();

        ChemicalSubstanceBuilder builder2= new ChemicalSubstanceBuilder();
        builder2.setStructureWithDefaultReference("OCCCCO");
        builder2.addName("1,4-BUTANEDIOL");
        builder2.addCode("CHEMBL", "CHEMBL171623");
        ChemicalSubstance chemical2 = builder2.build();

        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeCodes", "true");

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( chemical1, chemical2, parms, logger);
        Assertions.assertEquals(4,output.codes.size());
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);
        Assertions.assertTrue( chemical1.codes.stream().allMatch(c-> output.codes.stream().anyMatch(c2->c2.code.equals(c.code)&& c2.codeSystem.equals(c.codeSystem))));
    }


    @Test
    public void testMergeNamesAndCodes() {

        SubstanceBuilder builder= new SubstanceBuilder();
        builder.addName("putrecine");
        builder.addName("1,4 diaminobutane");
        builder.addCode("CHEMBL", "CHEMBL46257");
        builder.addCode("CHEBI", "CHEBI46257");
        builder.addCode("CAS", "BOGUS");
        Substance concept1 = builder.build();

        SubstanceBuilder builder2= new SubstanceBuilder();
        builder2.addName("1,4-BUTANEDIOL");
        builder2.addCode("CHEMBL", "CHEMBL171623");
        Substance concept2 = builder2.build();

        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeCodes", "true");
        parms.put("MergeNames", true);

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( concept1, concept2, parms, logger);
        Assertions.assertEquals(4,output.codes.size());
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);
        Assertions.assertTrue( concept1.codes.stream().allMatch(c-> output.codes.stream().anyMatch(c2->c2.code.equals(c.code)&& c2.codeSystem.equals(c.codeSystem))));
        Assertions.assertTrue(concept1.names.stream().allMatch(n->output.names.stream().anyMatch(n2->n.name.equals(n2.name) && n.displayName== n2.displayName &&
                n.languages.stream().allMatch(l1-> n2.languages.stream().anyMatch(l2->l1.term.equals(l2.term))))));
    }

    @Test
    public void testMergeReferences() {

        ProteinSubstanceBuilder builderSource= new ProteinSubstanceBuilder();
        Reference ref1 = new Reference();
        ref1.citation="A0JP26";
        ref1.docType="Uniprot";
        Subunit newUnit=new Subunit();
        newUnit.sequence="MVAEVCSMPAASAVKKPFDLRSKMGKWCHHRFPCCRGSGKSNMGTSGDHDDSFMKTLRSKMGKCCHHCFPCCRGSGTSNVGTSGDHDNSFMKTLRSKMGKWCCHCFPCCRGSGKSNVGTWGDYDDSAFMEPRYHVRREDLDKLHRAAWWGKVPRKDLIVMLRDTDMNKRDKQKRTALHLASANGNSEVVQLLLDRRCQLNVLDNKKRTALIKAVQCQEDECVLMLLEHGADGNIQDEYGNTALHYAIYNEDKLMAKALLLYGADIESKNKCGLTPLLLGVHEQKQQVVKFLIKKKANLNALDRYGRTALILAVCCGSASIVNLLLEQNVDVSSQDLSGQTAREYAVSSHHHVICELLSDYKEKQMLKISSENSNPEQDLKLTSEEESQRLKVSENSQPEKMSQEPEINKDCDREVEEEIKKHGSNPVGLPENLTNGASAGNGDDGLIPQRKSRKPENQQFPDTENEEYHSDEQNDTQKQLSEEQNTGISQDEILTNKQKQIEVAEKEMNSKLSLSHKKEEDLLRENSMLREEIAMLRLELDETKHQNQLRENKILEEIESVKEKLLKAIQLNEEALTKTSI";
        builderSource.addSubUnit(newUnit);

        Name newName = new Name("POTE ankyrin domain family member B3");
        newName.addReference(ref1);
        builderSource.addName(newName);
        Code code1= new Code("Uniprot", "A0JP26");
        builderSource.addCode(code1);
        builderSource.addReference(ref1);
        ProteinSubstance proteinSource= builderSource.build();

        ProteinSubstanceBuilder builderExisting= new ProteinSubstanceBuilder();
        Subunit newUnit2=new Subunit();
        newUnit2.sequence="MVAEVCSMPAASAVKKPFDLRSKMGKWCHHRFPCCRGSGKSNMGTSGDHDDSFMKTLRSKMGKCCHHCF";
        builderExisting.addSubUnit(newUnit2);
        builderExisting.addName("POTE ankyrin domain family member B Three");
        ProteinSubstance proteinExisting = builderExisting.build();
        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeReferences", true);

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( proteinSource, proteinExisting, parms, logger);
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);
        Assertions.assertTrue( proteinSource.references.stream().allMatch(r-> output.references.stream().anyMatch(r2-> r2.docType.equals(r.docType) && r2.citation.equals(r.citation))));
    }

    @Test
    public void testMergeReferencesWithUrl() {

        ProteinSubstanceBuilder builderSource= new ProteinSubstanceBuilder();
        Reference ref1 = new Reference();
        ref1.citation="A0JP26";
        ref1.docType="Uniprot";
        ref1.uploadedFile ="https://www.uniprot.org/uniprotkb/A0JP26/entry";
        Subunit newUnit=new Subunit();
        newUnit.sequence="MVAEVCSMPAASAVKKPFDLRSKMGKWCHHRFPCCRGSGKSNMGTSGDHDDSFMKTLRSKMGKCCHHCFPCCRGSGTSNVGTSGDHDNSFMKTLRSKMGKWCCHCFPCCRGSGKSNVGTWGDYDDSAFMEPRYHVRREDLDKLHRAAWWGKVPRKDLIVMLRDTDMNKRDKQKRTALHLASANGNSEVVQLLLDRRCQLNVLDNKKRTALIKAVQCQEDECVLMLLEHGADGNIQDEYGNTALHYAIYNEDKLMAKALLLYGADIESKNKCGLTPLLLGVHEQKQQVVKFLIKKKANLNALDRYGRTALILAVCCGSASIVNLLLEQNVDVSSQDLSGQTAREYAVSSHHHVICELLSDYKEKQMLKISSENSNPEQDLKLTSEEESQRLKVSENSQPEKMSQEPEINKDCDREVEEEIKKHGSNPVGLPENLTNGASAGNGDDGLIPQRKSRKPENQQFPDTENEEYHSDEQNDTQKQLSEEQNTGISQDEILTNKQKQIEVAEKEMNSKLSLSHKKEEDLLRENSMLREEIAMLRLELDETKHQNQLRENKILEEIESVKEKLLKAIQLNEEALTKTSI";
        builderSource.addSubUnit(newUnit);

        Name newName = new Name("POTE ankyrin domain family member B3");
        newName.addReference(ref1);
        builderSource.addName(newName);
        Code code1= new Code("Uniprot", "A0JP26");
        builderSource.addCode(code1);
        builderSource.addReference(ref1);
        ProteinSubstance proteinSource= builderSource.build();

        ProteinSubstanceBuilder builderExisting= new ProteinSubstanceBuilder();
        Subunit newUnit2=new Subunit();
        newUnit2.sequence="MVAEVCSMPAASAVKKPFDLRSKMGKWCHHRFPCCRGSGKSNMGTSGDHDDSFMKTLRSKMGKCCHHCF";
        builderExisting.addSubUnit(newUnit2);
        builderExisting.addName("POTE ankyrin domain family member B Three");
        Reference ref2 = new Reference();
        ref2.citation="A0JP26";
        ref2.docType="Uniprot";
        ref2.uploadedFile ="https://www.uniprot.org/uniprotkb/A0JP26/entry";
        builderExisting.addReference(ref2);
        ProteinSubstance proteinExisting = builderExisting.build();
        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeReferences", true);

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( proteinSource, proteinExisting, parms, logger);
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);
        Assertions.assertTrue( proteinSource.references.stream().allMatch(r-> output.references.stream().anyMatch(r2-> r2.docType.equals(r.docType) && r2.citation.equals(r.citation))));
    }
    @Test
    public void testMergeReferencesNegative() {
        ProteinSubstanceBuilder builderSource= new ProteinSubstanceBuilder();
        Reference ref1 = new Reference();
        ref1.citation="A0JP26";
        ref1.docType="Uniprot";
        Subunit newUnit=new Subunit();
        newUnit.sequence="MVAEVCSMPAASAVKKPFDLRSKMGKWCHHRFPCCRGSGKSNMGTSGDHDDSFMKTLRSKMGKCCHHCFPCCRGSGTSNVGTSGDHDNSFMKTLRSKMGKWCCHCFPCCRGSGKSNVGTWGDYDDSAFMEPRYHVRREDLDKLHRAAWWGKVPRKDLIVMLRDTDMNKRDKQKRTALHLASANGNSEVVQLLLDRRCQLNVLDNKKRTALIKAVQCQEDECVLMLLEHGADGNIQDEYGNTALHYAIYNEDKLMAKALLLYGADIESKNKCGLTPLLLGVHEQKQQVVKFLIKKKANLNALDRYGRTALILAVCCGSASIVNLLLEQNVDVSSQDLSGQTAREYAVSSHHHVICELLSDYKEKQMLKISSENSNPEQDLKLTSEEESQRLKVSENSQPEKMSQEPEINKDCDREVEEEIKKHGSNPVGLPENLTNGASAGNGDDGLIPQRKSRKPENQQFPDTENEEYHSDEQNDTQKQLSEEQNTGISQDEILTNKQKQIEVAEKEMNSKLSLSHKKEEDLLRENSMLREEIAMLRLELDETKHQNQLRENKILEEIESVKEKLLKAIQLNEEALTKTSI";
        builderSource.addSubUnit(newUnit);

        Name newName = new Name("POTE ankyrin domain family member B3");
        newName.addReference(ref1);
        builderSource.addName(newName);
        Code code1= new Code("Uniprot", "A0JP26");
        builderSource.addCode(code1);
        builderSource.addReference(ref1);
        ProteinSubstance proteinSource= builderSource.build();

        ProteinSubstanceBuilder builderExisting= new ProteinSubstanceBuilder();
        Subunit newUnit2=new Subunit();
        newUnit2.sequence="MVAEVCSMPAASAVKKPFDLRSKMGKWCHHRFPCCRGSGKSNMGTSGDHDDSFMKTLRSKMGKCCHHCF";
        builderExisting.addSubUnit(newUnit2);
        builderExisting.addName("POTE ankyrin domain family member B Three");
        ProteinSubstance proteinExisting = builderExisting.build();
        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeReferences", false);

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( proteinSource, proteinExisting, parms, logger);
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);
        Assertions.assertEquals( proteinExisting.references.size(), output.references.size());
    }

    @Test
    public void testMergeProperty1() {

        NucleicAcidSubstanceBuilder builder= new NucleicAcidSubstanceBuilder();
        Reference ref1 = new Reference();
        ref1.citation="215";
        ref1.docType="GenBank";
        ref1.url="https://www.ncbi.nlm.nih.gov/gene/215";
        String sequence="ACTGTCGCTTCAGCCAGGCTGCGGAGCGGACGGACGCGCCTGGTGCCCCGGGGAGGGGCGCCACCGGGGGAGGAGGAGGAGGAGAAGGTGGAGAGGAAGAGACGCCCCCTCTGCCCGAGACCTCTCAAGGCCCTGACCTCAGGGGCCAGGGCACTGACAGGACAGGAGAGCCAAGTTCCTCCACTTGGGCTGCCCGAAGAGGCCGCGACC";
        builder.addDnaSubunit(sequence);

        String nameValue="ATP binding cassette subfamily D member 1";
        Name newName = new Name(nameValue);
        newName.addReference(ref1);
        builder.addName(newName);
        Code code1= new Code("GenBank", "215");
        builder.addCode(code1);
        builder.addReference(ref1);
        Property p1 = new Property();
        String property1Name ="Color";
        p1.setName(property1Name);
        Amount value1 = new Amount();
        value1.nonNumericValue="purple";
        p1.setValue(value1);
        builder.addProperty(p1);

        Property p2 = new Property();
        String property2Name ="Melting Point";
        p2.setName(property2Name);
        Amount value2 = new Amount();
        value2.average=123.4;
        p2.setValue(value2);
        builder.addProperty(p2);
        NucleicAcidSubstance nucleicAcidSubstance = builder.build();

        NucleicAcidSubstanceBuilder builder2= new NucleicAcidSubstanceBuilder();
        builder2.addDnaSubunit(sequence);
        builder2.addName(nameValue);
        NucleicAcidSubstance nucleicAcidSubstance2 = builder2.build();
        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeReferences", true);

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( nucleicAcidSubstance, nucleicAcidSubstance2, parms, logger);
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);

        Assertions.assertEquals(0, output.properties.size());
    }

    /*
    2 properties in source; will be copied based on setting
     */
    @Test
    public void testMergeProperty2() {

        NucleicAcidSubstanceBuilder builder= new NucleicAcidSubstanceBuilder();
        Reference ref1 = new Reference();
        ref1.citation="215";
        ref1.docType="GenBank";
        ref1.url="https://www.ncbi.nlm.nih.gov/gene/215";
        String sequence="ACTGTCGCTTCAGCCAGGCTGCGGAGCGGACGGACGCGCCTGGTGCCCCGGGGAGGGGCGCCACCGGGGGAGGAGGAGGAGGAGAAGGTGGAGAGGAAGAGACGCCCCCTCTGCCCGAGACCTCTCAAGGCCCTGACCTCAGGGGCCAGGGCACTGACAGGACAGGAGAGCCAAGTTCCTCCACTTGGGCTGCCCGAAGAGGCCGCGACC";
        builder.addDnaSubunit(sequence);

        String nameValue="ATP binding cassette subfamily D member 1";
        Name newName = new Name(nameValue);
        newName.addReference(ref1);
        builder.addName(newName);
        Code code1= new Code("GenBank", "215");
        builder.addCode(code1);
        builder.addReference(ref1);
        Property p1 = new Property();
        String property1Name ="Color";
        p1.setName(property1Name);
        Amount value1 = new Amount();
        value1.nonNumericValue="purple";
        p1.setValue(value1);
        builder.addProperty(p1);

        Property p2 = new Property();
        String property2Name ="Melting Point";
        p2.setName(property2Name);
        Amount value2 = new Amount();
        value2.average=123.4;
        p2.setValue(value2);
        builder.addProperty(p2);
        NucleicAcidSubstance nucleicAcidSubstance = builder.build();

        NucleicAcidSubstanceBuilder builder2= new NucleicAcidSubstanceBuilder();
        builder2.addDnaSubunit(sequence);
        builder2.addName(nameValue);
        NucleicAcidSubstance nucleicAcidSubstance2 = builder2.build();
        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeProperties", true);

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( nucleicAcidSubstance, nucleicAcidSubstance2, parms, logger);
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);

        Assertions.assertEquals(2, output.properties.size());
    }

    /*
    2 properties have the same name (different value) - setting does not block duplicates
     */
    @Test
    public void testMergeProperty3() {
        NucleicAcidSubstanceBuilder builder= new NucleicAcidSubstanceBuilder();
        Reference ref1 = new Reference();
        ref1.citation="215";
        ref1.docType="GenBank";
        ref1.url="https://www.ncbi.nlm.nih.gov/gene/215";
        String sequence="ACTGTCGCTTCAGCCAGGCTGCGGAGCGGACGGACGCGCCTGGTGCCCCGGGGAGGGGCGCCACCGGGGGAGGAGGAGGAGGAGAAGGTGGAGAGGAAGAGACGCCCCCTCTGCCCGAGACCTCTCAAGGCCCTGACCTCAGGGGCCAGGGCACTGACAGGACAGGAGAGCCAAGTTCCTCCACTTGGGCTGCCCGAAGAGGCCGCGACC";
        builder.addDnaSubunit(sequence);

        String nameValue="ATP binding cassette subfamily D member 1";
        Name newName = new Name(nameValue);
        newName.addReference(ref1);
        builder.addName(newName);
        Code code1= new Code("GenBank", "215");
        builder.addCode(code1);
        builder.addReference(ref1);
        Property p1 = new Property();
        String property1Name ="Color";
        p1.setName(property1Name);
        Amount value1 = new Amount();
        value1.nonNumericValue="purple";
        p1.setValue(value1);
        builder.addProperty(p1);

        Property p2 = new Property();
        String property2Name ="Melting Point";
        p2.setName(property2Name);
        Amount value2 = new Amount();
        value2.average=123.4;
        p2.setValue(value2);
        builder.addProperty(p2);
        Property p3 = new Property();
        p3.setName(property2Name);
        Amount value3 = new Amount();
        value3.average=121.4;
        p3.setValue(value3);
        builder.addProperty(p3);
        NucleicAcidSubstance nucleicAcidSubstance = builder.build();

        NucleicAcidSubstanceBuilder builder2= new NucleicAcidSubstanceBuilder();
        builder2.addDnaSubunit(sequence);
        builder2.addName(nameValue);
        NucleicAcidSubstance nucleicAcidSubstance2 = builder2.build();
        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeProperties", true);

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( nucleicAcidSubstance, nucleicAcidSubstance2, parms, logger);
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);
        Assertions.assertEquals(3, output.properties.size());
    }

    /*
    2 properties have the same name (different value) - setting blocks duplicates
     */
    @Test
    public void testMergeProperty4() {
        NucleicAcidSubstanceBuilder builder= new NucleicAcidSubstanceBuilder();
        Reference ref1 = new Reference();
        ref1.citation="215";
        ref1.docType="GenBank";
        ref1.url="https://www.ncbi.nlm.nih.gov/gene/215";
        String sequence="ACTGTCGCTTCAGCCAGGCTGCGGAGCGGACGGACGCGCCTGGTGCCCCGGGGAGGGGCGCCACCGGGGGAGGAGGAGGAGGAGAAGGTGGAGAGGAAGAGACGCCCCCTCTGCCCGAGACCTCTCAAGGCCCTGACCTCAGGGGCCAGGGCACTGACAGGACAGGAGAGCCAAGTTCCTCCACTTGGGCTGCCCGAAGAGGCCGCGACC";
        builder.addDnaSubunit(sequence);

        String nameValue="ATP binding cassette subfamily D member 1";
        Name newName = new Name(nameValue);
        newName.addReference(ref1);
        builder.addName(newName);
        Code code1= new Code("GenBank", "215");
        builder.addCode(code1);
        builder.addReference(ref1);
        Property p1 = new Property();
        String property1Name ="Color";
        p1.setName(property1Name);
        Amount value1 = new Amount();
        value1.nonNumericValue="purple";
        p1.setValue(value1);
        builder.addProperty(p1);

        Property p2 = new Property();
        String property2Name ="Melting Point";
        String propertyType = "Physical";
        p2.setName(property2Name);
        Amount value2 = new Amount();
        value2.average=123.4;
        p2.setValue(value2);
        p2.setPropertyType(propertyType );
        builder.addProperty(p2);
        Property p3 = new Property();
        p3.setName(property2Name);
        Amount value3 = new Amount();
        value3.average=121.4;
        p3.setValue(value3);
        p3.setPropertyType(propertyType );
        builder.addProperty(p3);
        NucleicAcidSubstance nucleicAcidSubstance = builder.build();

        NucleicAcidSubstanceBuilder builder2= new NucleicAcidSubstanceBuilder();
        builder2.addDnaSubunit(sequence);
        builder2.addName(nameValue);
        NucleicAcidSubstance nucleicAcidSubstance2 = builder2.build();
        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeProperties", true);
        parms.put("PropertyNameUniqueness", true);

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( nucleicAcidSubstance, nucleicAcidSubstance2, parms, logger);
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);

        Assertions.assertEquals(2, output.properties.size());
    }

    @Test
    public void testMergeRelationships() {
        NucleicAcidSubstanceBuilder builderSource= new NucleicAcidSubstanceBuilder();
        Reference ref1 = new Reference();
        ref1.citation="215";
        ref1.docType="GenBank";
        ref1.url="https://www.ncbi.nlm.nih.gov/gene/215";
        String sequence="ACTGTCGCTTCAGCCAGGCTGCGGAGCGGACGGACGCGCCTGGTGCCCCGGGGAGGGGCGCCACCGGGGGAGGAGGAGGAGGAGAAGGTGGAGAGGAAGAGACGCCCCCTCTGCCCGAGACCTCTCAAGGCCCTGACCTCAGGGGCCAGGGCACTGACAGGACAGGAGAGCCAAGTTCCTCCACTTGGGCTGCCCGAAGAGGCCGCGACC";
        builderSource.addDnaSubunit(sequence);

        String nameValue="ATP binding cassette subfamily D member 1";
        Name newName = new Name(nameValue);
        newName.addReference(ref1);
        builderSource.addName(newName);
        Code code1= new Code("GenBank", "215");
        builderSource.addCode(code1);
        builderSource.addReference(ref1);
        Property p1 = new Property();
        String property1Name ="Color";
        p1.setName(property1Name);
        Amount value1 = new Amount();
        value1.nonNumericValue="purple";
        p1.setValue(value1);
        builderSource.addProperty(p1);

        Property p2 = new Property();
        String property2Name ="Melting Point";
        String propertyType = "Physical";
        p2.setName(property2Name);
        Amount value2 = new Amount();
        value2.average=123.4;
        p2.setValue(value2);
        p2.setPropertyType(propertyType );
        builderSource.addProperty(p2);
        Property p3 = new Property();
        p3.setName(property2Name);
        Amount value3 = new Amount();
        value3.average=121.4;
        p3.setValue(value3);
        p3.setPropertyType(propertyType );
        builderSource.addProperty(p3);

        Relationship relationship1 = new Relationship();
        relationship1.type="TARGET->INHIBITOR";
        relationship1.relatedSubstance = new SubstanceReference();
        relationship1.relatedSubstance.refuuid="Fill something in";
        builderSource.addRelationship(relationship1);
        NucleicAcidSubstance nucleicAcidSubstanceSource = builderSource.build();

        NucleicAcidSubstanceBuilder builderExisting= new NucleicAcidSubstanceBuilder();
        builderExisting.addDnaSubunit(sequence);
        builderExisting.addName(nameValue);
        NucleicAcidSubstance nucleicAcidSubstanceExisting = builderExisting.build();
        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeProperties", true);
        parms.put("PropertyNameUniqueness", true);
        parms.put("MergeRelationships", true);

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( nucleicAcidSubstanceSource, nucleicAcidSubstanceExisting, parms, logger);
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);

        Assertions.assertEquals(1, output.relationships.size());
        Assertions.assertEquals(relationship1.type, output.relationships.get(0).type);
    }

    @Test
    public void testMergeRelationshipNegative() {
        NucleicAcidSubstanceBuilder builderSource= new NucleicAcidSubstanceBuilder();
        Reference ref1 = new Reference();
        ref1.citation="215";
        ref1.docType="GenBank";
        ref1.url="https://www.ncbi.nlm.nih.gov/gene/215";
        String sequence="ACTGTCGCTTCAGCCAGGCTGCGGAGCGGACGGACGCGCCTGGTGCCCCGGGGAGGGGCGCCACCGGGGGAGGAGGAGGAGGAGAAGGTGGAGAGGAAGAGACGCCCCCTCTGCCCGAGACCTCTCAAGGCCCTGACCTCAGGGGCCAGGGCACTGACAGGACAGGAGAGCCAAGTTCCTCCACTTGGGCTGCCCGAAGAGGCCGCGACC";
        builderSource.addDnaSubunit(sequence);

        String nameValue="ATP binding cassette subfamily D member 1";
        Name newName = new Name(nameValue);
        newName.addReference(ref1);
        builderSource.addName(newName);
        Code code1= new Code("GenBank", "215");
        builderSource.addCode(code1);
        builderSource.addReference(ref1);
        Property p1 = new Property();
        String property1Name ="Color";
        p1.setName(property1Name);
        Amount value1 = new Amount();
        value1.nonNumericValue="purple";
        p1.setValue(value1);
        builderSource.addProperty(p1);

        Property p2 = new Property();
        String property2Name ="Melting Point";
        String propertyType = "Physical";
        p2.setName(property2Name);
        Amount value2 = new Amount();
        value2.average=123.4;
        p2.setValue(value2);
        p2.setPropertyType(propertyType );
        builderSource.addProperty(p2);
        Property p3 = new Property();
        p3.setName(property2Name);
        Amount value3 = new Amount();
        value3.average=121.4;
        p3.setValue(value3);
        p3.setPropertyType(propertyType );
        builderSource.addProperty(p3);

        Relationship relationship1 = new Relationship();
        relationship1.type="TARGET->INHIBITOR";
        relationship1.relatedSubstance = new SubstanceReference();
        relationship1.relatedSubstance.refuuid="Fill something in";
        builderSource.addRelationship(relationship1);
        NucleicAcidSubstance nucleicAcidSubstanceSource = builderSource.build();

        NucleicAcidSubstanceBuilder builderExisting= new NucleicAcidSubstanceBuilder();
        builderExisting.addDnaSubunit(sequence);
        builderExisting.addName(nameValue);
        NucleicAcidSubstance nucleicAcidSubstanceExisting = builderExisting.build();
        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeRelationships", false);

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( nucleicAcidSubstanceSource, nucleicAcidSubstanceExisting, parms, logger);
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);
        Assertions.assertEquals(0, output.relationships.size());
    }

    @Test
    public void testMergeNotes() {
        NucleicAcidSubstanceBuilder builderSource= new NucleicAcidSubstanceBuilder();
        Reference ref1 = new Reference();
        ref1.citation="215";
        ref1.docType="GenBank";
        ref1.url="https://www.ncbi.nlm.nih.gov/gene/215";
        String sequence="ACTGTCGCTTCAGCCAGGCTGCGGAGCGGACGGACGCGCCTGGTGCCCCGGGGAGGGGCGCCACCGGGGGAGGAGGAGGAGGAGAAGGTGGAGAGGAAGAGACGCCCCCTCTGCCCGAGACCTCTCAAGGCCCTGACCTCAGGGGCCAGGGCACTGACAGGACAGGAGAGCCAAGTTCCTCCACTTGGGCTGCCCGAAGAGGCCGCGACC";
        builderSource.addDnaSubunit(sequence);

        String nameValue="ATP binding cassette subfamily D member 1";
        Name newName = new Name(nameValue);
        newName.addReference(ref1);
        builderSource.addName(newName);
        Code code1= new Code("GenBank", "215");
        builderSource.addCode(code1);
        builderSource.addReference(ref1);
        Note basicNote = new Note();
        basicNote.note="Something to keep track of";
        builderSource.addNote(basicNote);

        Property p2 = new Property();
        String property2Name ="Melting Point";
        String propertyType = "Physical";
        p2.setName(property2Name);
        Amount value2 = new Amount();
        value2.average=123.4;
        p2.setValue(value2);
        p2.setPropertyType(propertyType );
        builderSource.addProperty(p2);
        Property p3 = new Property();
        p3.setName(property2Name);
        Amount value3 = new Amount();
        value3.average=121.4;
        p3.setValue(value3);
        p3.setPropertyType(propertyType );
        builderSource.addProperty(p3);

        Relationship relationship1 = new Relationship();
        relationship1.type="TARGET->INHIBITOR";
        relationship1.relatedSubstance = new SubstanceReference();
        relationship1.relatedSubstance.refuuid="Fill something in";
        builderSource.addRelationship(relationship1);
        NucleicAcidSubstance nucleicAcidSubstanceSource = builderSource.build();

        NucleicAcidSubstanceBuilder builderExisting= new NucleicAcidSubstanceBuilder();
        builderExisting.addDnaSubunit(sequence);
        builderExisting.addName(nameValue);
        NucleicAcidSubstance nucleicAcidSubstanceExisting = builderExisting.build();
        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeNotes", true);
        parms.put("MergeRelationships", true);

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( nucleicAcidSubstanceSource, nucleicAcidSubstanceExisting, parms, logger);
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);

        Assertions.assertEquals(1, output.notes.size());
        Assertions.assertEquals(basicNote.note, output.notes.get(0).note);
    }

    @Test
    public void testMergeNotesNegative() {
        NucleicAcidSubstanceBuilder builderSource= new NucleicAcidSubstanceBuilder();
        Reference ref1 = new Reference();
        ref1.citation="215";
        ref1.docType="GenBank";
        ref1.url="https://www.ncbi.nlm.nih.gov/gene/215";
        String sequence="ACTGTCGCTTCAGCCAGGCTGCGGAGCGGACGGACGCGCCTGGTGCCCCGGGGAGGGGCGCCACCGGGGGAGGAGGAGGAGGAGAAGGTGGAGAGGAAGAGACGCCCCCTCTGCCCGAGACCTCTCAAGGCCCTGACCTCAGGGGCCAGGGCACTGACAGGACAGGAGAGCCAAGTTCCTCCACTTGGGCTGCCCGAAGAGGCCGCGACC";
        builderSource.addDnaSubunit(sequence);

        String nameValue="ATP binding cassette subfamily D member 1";
        Name newName = new Name(nameValue);
        newName.addReference(ref1);
        builderSource.addName(newName);
        Code code1= new Code("GenBank", "215");
        builderSource.addCode(code1);
        builderSource.addReference(ref1);
        Note basicNote = new Note();
        basicNote.note="Something to keep track of";
        builderSource.addNote(basicNote);

        Property p2 = new Property();
        String property2Name ="Melting Point";
        String propertyType = "Physical";
        p2.setName(property2Name);
        Amount value2 = new Amount();
        value2.average=123.4;
        p2.setValue(value2);
        p2.setPropertyType(propertyType );
        builderSource.addProperty(p2);
        Property p3 = new Property();
        p3.setName(property2Name);
        Amount value3 = new Amount();
        value3.average=121.4;
        p3.setValue(value3);
        p3.setPropertyType(propertyType );
        builderSource.addProperty(p3);

        Relationship relationship1 = new Relationship();
        relationship1.type="TARGET->INHIBITOR";
        relationship1.relatedSubstance = new SubstanceReference();
        relationship1.relatedSubstance.refuuid="Fill something in";
        builderSource.addRelationship(relationship1);
        NucleicAcidSubstance nucleicAcidSubstanceSource = builderSource.build();

        NucleicAcidSubstanceBuilder builderExisting= new NucleicAcidSubstanceBuilder();
        builderExisting.addDnaSubunit(sequence);
        builderExisting.addName(nameValue);
        NucleicAcidSubstance nucleicAcidSubstanceExisting = builderExisting.build();
        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeNotes", false);

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Substance output = action.process( nucleicAcidSubstanceSource, nucleicAcidSubstanceExisting, parms, logger);
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);
        Assertions.assertEquals(0, output.notes.size());
    }

    @Test
    public void mergeMods1Test() throws IOException {
        File proteinFile =new ClassPathResource("testJSON/YYD6UT8T47.json").getFile();
        ProteinSubstanceBuilder builder =SubstanceBuilder.from(proteinFile);
        ProteinSubstance proteinSubstanceSource= builder.build();
        ProteinSubstance proteinSubstanceTarget= builder.build();
        proteinSubstanceTarget.modifications.structuralModifications.clear();

        StringBuilder buildMessage = new StringBuilder();
        Consumer<String> logger = buildMessage::append;
        MergeProcessingAction action = new MergeProcessingAction();
        Map<String, Object> parms = new HashMap<>();
        parms.put("MergeModifications", true);
        parms.put("MergeStructuralModifications", true);

        Substance output = action.process( proteinSubstanceSource, proteinSubstanceTarget, parms, logger);
        System.out.printf("message: %s; type: %s", buildMessage, output.substanceClass);
        Assertions.assertEquals(proteinSubstanceSource.getModifications().structuralModifications.size(), output.getModifications().structuralModifications.size());
    }

    @Test
    public void hasTrueValueTest(){
        MergeProcessingAction action = new MergeProcessingAction();
        Map<String, Object> parmset = new HashMap<>();

        Assertions.assertFalse( action.hasTrueValue(parmset, "anything"));

    }

    @Test
    public void hasTrueValueTest2(){
        MergeProcessingAction action = new MergeProcessingAction();
        Map<String, Object> parmset = new HashMap<>();

        parmset.put("Something", false);
        Assertions.assertFalse( action.hasTrueValue(parmset, "Something"));
    }

    @Test
    public void hasTrueValueTest3(){
        MergeProcessingAction action = new MergeProcessingAction();
        Map<String, Object> parmset = new HashMap<>();
        parmset.put("Something", true);
        Assertions.assertTrue( action.hasTrueValue(parmset, "Something"));
    }


    @Test
    public void hasTrueValueTest4(){
        MergeProcessingAction action = new MergeProcessingAction();
        Map<String, Object> parmset = new HashMap<>();
        parmset.put("Something", "TRUE");
        Assertions.assertTrue( action.hasTrueValue(parmset, "Something"));
    }
}
