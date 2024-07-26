package example.substance.validation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.NucleicAcidSubstanceBuilder;
import ix.ginas.models.v1.*;
import ix.ginas.utils.NucleicAcidUtils;
import ix.ginas.utils.validation.validators.NucleicAcidValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Created by katzelda on 8/9/18.
 */
@WithMockUser(username = "admin", roles="Admin")
//@Ignore("don't have nucleic acid validator added yet since it uses seq search")
@Slf4j
public class NucleicAcidValidationTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    private TestGsrsValidatorFactory factory;
    @BeforeEach
    public void setup() {

        ValidatorConfig config = new DefaultValidatorConfig();
        config.setValidatorClass(NucleicAcidValidator.class);
        config.setNewObjClass(NucleicAcidSubstance.class);

        factory.addValidator("substances", config);
    }

    @Test
    public void mustHaveSubunit() throws Exception{
            JsonNode sub = new NucleicAcidSubstanceBuilder()
                    .addName("name")
                    .buildJson();

            ValidationResponse<Substance> response = substanceEntityService.validateEntity(sub);
//            System.out.println(response.getMessages());
            assertFalse(response.isValid());

            assertTrue(response.getValidationMessages().stream()
                    .filter(m->m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR &&  m.getMessage().contains("at least 1 subunit"))
                    .findAny().isPresent());



    }

     @Test
    public void flag0Subunit() throws Exception{
            Substance substance = new NucleicAcidSubstanceBuilder()
                    .addName("name")
                    .build();
            substance.definitionLevel = Substance.SubstanceDefinitionLevel.INCOMPLETE;
            JsonNode sub =substance.toFullJsonNode();

            ValidationResponse<Substance> response = substanceEntityService.validateEntity(sub);
            assertFalse(response.isValid());

            Assertions.assertEquals(1, response.getValidationMessages().stream()
                    .filter(m->m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING &&  m.getMessage().contains("Warning - Nucleic Acid substances usually have at least 1 subunit, but zero subunits were found here.  This is discouraged and is only allowed for records labelled as incomplete"))
                    .count());
    }

    private void assertArrayNotEquals(byte[] expecteds, byte[] actuals) {
        try {
            assertArrayEquals(expecteds, actuals);
        } catch (AssertionError e) {
            return;
        }
        fail("The arrays are equal");
    }

    //TODO move these def hash tests to their own test class
/*
    @Test
    public void addingSubunitChangesDefinitionalHash(){
        String sequence = "ACGTACGTACGT";

        NucleicAcidSubstance sub = new NucleicAcidSubstanceBuilder()
                .addName("aName")
                .addDnaSubunit(sequence)
                .build();

        byte[] oldHash = sub.getDefinitionalElements().getDefinitionalHash();


        byte[] newHash = new NucleicAcidSubstanceBuilder(sub).addDnaSubunit("AAAAAAA").build().getDefinitionalElements().getDefinitionalHash();

        assertArrayNotEquals(oldHash, newHash);
    }

    @Test
    public void changingSubunitChangesDefinitionalHash(){
        String sequence = "ACGTACGTACGT";

        NucleicAcidSubstance sub = new NucleicAcidSubstanceBuilder()
                .addName("aName")
                .addDnaSubunit(sequence)
                .build();

        byte[] oldHash = sub.getDefinitionalElements().getDefinitionalHash();

        sub.nucleicAcid.getSubunits().get(0).sequence = sequence + "NN";

        byte[] newHash = sub.getDefinitionalElements().getDefinitionalHash();

        assertArrayNotEquals(oldHash, newHash);
    }
*/
    @Test
    public void dnaSeqMustReference() throws Exception{


            NucleicAcidSubstance sub = new NucleicAcidSubstanceBuilder()
                    .addName("name")
                    .addDnaSubunit("ACGTACGT")
                    .build();


            sub.nucleicAcid.setReferences(Collections.emptySet());
            sub.references.clear();

            ValidationResponse response = substanceEntityService.validateEntity(sub.toFullJsonNode());
//            assertFalse(response.isValid());

        Stream<ValidationMessage> stream = response.getValidationMessages().stream();
        assertTrue(response.getValidationMessages().toString(), stream
                    .filter(m-> /*m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR && */  m.getMessage().contains("needs at least 1 reference"))
                    .findAny().isPresent());



    }

    @Test
    public void validDna() throws Exception{


            NucleicAcidSubstance sub = new NucleicAcidSubstanceBuilder()
                    .addName("name")
                    .addDnaSubunit("ACGTACGT")
                    .build();



        ValidationResponse response = substanceEntityService.validateEntity(sub.toFullJsonNode());
        assertTrue(response.getValidationMessages().toString(), response.isValid());

        Stream<ValidationMessage> stream = response.getValidationMessages().stream();
        assertFalse(response.getValidationMessages().toString(), stream
                    .filter(m-> m.getMessage().contains("needs at least 1 reference"))
                    .findAny().isPresent());



    }
    @Test
    public void invalidDnaSequence() throws Exception{
            JsonNode sub = new NucleicAcidSubstanceBuilder()
                    .addName("name")
                    .addDnaSubunit("ACGT&&&NNT")
                    .buildJson();


        ValidationResponse response = substanceEntityService.validateEntity(sub);
        assertFalse(response.getValidationMessages().toString(), response.isValid());

        Stream<ValidationMessage> stream = response.getValidationMessages().stream();
        assertTrue(response.getValidationMessages().toString(), stream
                .filter(m-> m.getMessage().contains("invalid character"))
                .findAny().isPresent());




    }
    @Test
    public void invalidRnaSequence() throws Exception {
        JsonNode sub = new NucleicAcidSubstanceBuilder()
                .addName("name")
                .addRnaSubunit("ACGU&&&NNU")
                .buildJson();

        ValidationResponse response = substanceEntityService.validateEntity(sub);
        assertFalse(response.getValidationMessages().toString(), response.isValid());

        Stream<ValidationMessage> stream = response.getValidationMessages().stream();
        assertTrue(response.getValidationMessages().toString(), stream
                .filter(m -> m.getMessage().contains("invalid character"))
                .findAny().isPresent());

    }

    @Test
    public void dnaSugarMod() throws Exception{
        log.trace("starting dnaSugarMod");
        Reference simpleReference = new Reference();
        simpleReference.docType = "Book";
        simpleReference.citation = "ISBN 9780121550899";

        NucleicAcidSubstance sub = new NucleicAcidSubstanceBuilder()
                .addName("nucleic acid  1")
                .addDnaSubunit("ACGTACGT")
                .addReference(simpleReference)
                .build();

        log.trace("created substance");
        Sugar sugar1 = new Sugar();
        sugar1.setSugar("deoxyribose");

        List<Site> sites = new ArrayList<>();
        Site site = new Site();
        site.subunitIndex = 1;
        site.residueIndex = 1;
        sites.add(site);
        site = new Site();
        site.subunitIndex = 1;
        site.residueIndex = 2;
        sites.add(site);
        site = new Site();
        site.subunitIndex = 1;
        site.residueIndex = 3;
        sites.add(site);
        site = new Site();
        site.subunitIndex = 1;
        site.residueIndex = 4;
        sites.add(site);
        sugar1.setSites(sites);
        Sugar sugar2 = new Sugar();
        sugar2.setSugar("deoxyribose 2");
        sugar2.setSites(Arrays.asList(Site.of("1_5"), Site.of("1_6"), Site.of("1_7"), Site.of("1_8")));
        List<Site> sites2 = new ArrayList<>();
        Site site2 = new Site();
        site2.subunitIndex = 1;
        site2.residueIndex = 5;
        sites2.add(site2);
        site2 = new Site();
        site2.subunitIndex = 1;
        site2.residueIndex = 6;
        sites2.add(site2);
        site2 = new Site();
        site2.subunitIndex = 1;
        site2.residueIndex = 7;
        sites2.add(site2);
        site2 = new Site();
        site2.subunitIndex = 1;
        site2.residueIndex = 8;
        sites2.add(site2);
        sugar2.setSites(sites2);

        sub.nucleicAcid.setSugars(Arrays.asList(sugar1, sugar2));
        log.trace("assigned sugars");
        ValidationResponse response = substanceEntityService.validateEntity(sub.toFullJsonNode());
        assertTrue(response.getValidationMessages().toString(), response.isValid());
        log.trace("verified initial validation response");

        List<Site> updatedSites1 = new ArrayList<>();
        site = new Site();
        site.subunitIndex = 1;
        site.residueIndex = 2;
        updatedSites1.add(site);
        site = new Site();
        site.subunitIndex = 1;
        site.residueIndex = 3;
        updatedSites1.add(site);
        site = new Site();
        site.subunitIndex = 1;
        site.residueIndex = 4;
        updatedSites1.add(site);
        sugar1.setSites(updatedSites1);

        List<Site> updatedSites2 = new ArrayList<>();
        site2 = new Site();
        site2.subunitIndex = 1;
        site2.residueIndex = 6;
        updatedSites2.add(site2);
        site2 = new Site();
        site2.subunitIndex = 1;
        site2.residueIndex = 7;
        updatedSites2.add(site2);
        site2 = new Site();
        site2.subunitIndex = 1;
        site2.residueIndex = 8;
        updatedSites2.add(site2);
        sugar2.setSites(updatedSites2);

        Sugar sugar3 = new Sugar();
        sugar3.setSugar("radioactive deoxyribose");
        Site site3 = new Site();
        site3.residueIndex =1;
        site3.subunitIndex = 1;
        sugar3.setSites(Collections.singletonList(site3));
        Sugar sugar4 = new Sugar();
        sugar4.setSugar("radioactive deoxyribose2");
        Site site4 = new Site();
        site4.residueIndex =5;
        site4.subunitIndex = 1;
        sugar4.setSites(Collections.singletonList(site4));

        sub.nucleicAcid.setSugars(Arrays.asList(sugar1, sugar2, sugar3, sugar4));
        log.trace("modified sugars");
        log.trace("Json: {}", sub.toFullJsonNode().toPrettyString());

        ValidationResponse response2 = substanceEntityService.validateEntity(sub.toFullJsonNode());
        Stream<ValidationMessage> stream2 = response2.getValidationMessages().stream();

        assertFalse(response2.getValidationMessages().toString(), stream2
                .filter(m-> m.getMessage().contains("Nucleic Acid substance must have every base specify a sugar fragment"))
                .findAny().isPresent());
    }

    @Test
    public void dnaSugarTest() throws Exception {
        log.trace("starting dnaSugarTest");
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        File jsonFile =new ClassPathResource("testJSON/5ba799f2-2834-4b02-8ed7-d426ecc68ce3.json").getFile();
        JsonNode substanceAsJson=  mapper.readTree(jsonFile);
        ValidationResponse response = substanceEntityService.validateEntity(substanceAsJson);
        Stream<ValidationMessage> stream = response.getValidationMessages().stream();

        NucleicAcidSubstance substance= mapper.convertValue(substanceAsJson, NucleicAcidSubstance.class);
        int actualUnspecified =NucleicAcidUtils.getNumberOfUnspecifiedSugarSites(substance);
        int expectedUnspecified = 0;
        Assertions.assertEquals(expectedUnspecified, actualUnspecified);
    }

    @Test
    public void getActualSugarSiteCountTest() throws Exception {
        log.trace("starting getActualSugarSiteCountTest");
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        File jsonFile =new ClassPathResource("testJSON/5ba799f2-2834-4b02-8ed7-d426ecc68ce3.json").getFile();
        JsonNode substanceAsJson=  mapper.readTree(jsonFile);
        NucleicAcidSubstance substance= mapper.convertValue(substanceAsJson, NucleicAcidSubstance.class);
        int expectedSugarCount= 45;
        int actual = NucleicAcidUtils.getActualSugarSiteCount(substance);
        Assertions.assertEquals(expectedSugarCount, actual);
    }

    @Test
    public void getBaseCountTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        File jsonFile =new ClassPathResource("testJSON/5ba799f2-2834-4b02-8ed7-d426ecc68ce3.json").getFile();
        JsonNode substanceAsJson=  mapper.readTree(jsonFile);
        NucleicAcidSubstance substance= mapper.convertValue(substanceAsJson, NucleicAcidSubstance.class);
        int expectedBaseCount= 45;
        int actual = NucleicAcidUtils.getBaseCount(substance);
        Assertions.assertEquals(expectedBaseCount, actual);
    }
}
