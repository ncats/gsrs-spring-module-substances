package example.exports.expanders;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.expanders.basic.BasicRecordExpander;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
public class ExpanderTestForIssue extends AbstractSubstanceJpaFullStackEntityTest {

    private String fileName = "issue-4.gsrs";

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;

    @Autowired
    private TestGsrsValidatorFactory factory;

    private static boolean loadedData = false;

    @BeforeEach
    public void setupDataset() throws IOException {
        log.trace("starting in resetDataset()");
        if( substanceRepository.count()==0) {
            log.trace("need to load data");
            SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
            AutowireHelper.getInstance().autowire(hashIndexer);
            testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
            {
                ValidatorConfig config = new DefaultValidatorConfig();
                config.setValidatorClass(ChemicalValidator.class);
                config.setNewObjClass(ChemicalSubstance.class);
                factory.addValidator("substances", config);
            }

            ClassPathResource resource = new ClassPathResource(fileName);
            File dataFile = resource.getFile();
            loadGsrsFile(dataFile);
            log.trace("loaded data");
            loadedData=true;
        }
    }

    @Test
    public void testExpanderMixtureComponentAddition(){
        log.trace("starting testExpanderMixtureComponentAddition");
        log.trace("total substances: {} in repo", substanceRepository.count());
        String startingSubstanceId= "d8c4aedd-e2da-4256-924e-64175e58f324"; //onion powder, a str div

        List<String> expectedIds = Arrays.asList("f8b16d9d-7593-4baf-a92d-aca92d4dd4be", "d8c4aedd-e2da-4256-924e-64175e58f324",
                "86d15a22-d3db-4030-9a4c-9ccbe393f738", "8eeb12e4-0bd8-44a3-83cd-ac1ab9258239");
        BasicRecordExpander expander = new BasicRecordExpander();
        expander = AutowireHelper.getInstance().autowireAndProxy(expander);
        ObjectNode configurationNode = JsonNodeFactory.instance.objectNode();
        configurationNode.put("includeDefinitionalItems", true);
        configurationNode.put("definitionalGenerations", 3);
        configurationNode.put("includeRelated",true);
        configurationNode.put("generationsToExpandRelated",3);
        expander.applySettings(configurationNode);
        Substance startingSubstance = substanceEntityService.get(UUID.fromString(startingSubstanceId)).get();
        Stream<Substance> expanded = expander.expandRecord(startingSubstance);
        List<Substance> expandedList = expanded.collect(Collectors.toList());
        log.trace("expanded list of substance:");
        expandedList.forEach(s-> log.trace(s.uuid.toString()));

        Assertions.assertTrue(expectedIds.stream().allMatch(i->expandedList.stream().anyMatch(s->s.uuid.toString().equals(i))));
        Assertions.assertEquals(4, expandedList.size());
    }

}
