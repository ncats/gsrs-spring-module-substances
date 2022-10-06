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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
public class BasicSubstanceExpanderTest extends AbstractSubstanceJpaFullStackEntityTest {
    private String fileName = "rep18+12.tsv";

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
        log.trace("total substances: {}", substanceRepository.count());
        String basicSubstanceId="c3685a23-87a4-47de-8bb3-ab9a9cfe9606"; //a mixture

        String oneComponentId="0d1371fc-904f-45e9-b073-ba55dacc4f30";
        BasicRecordExpander expander = new BasicRecordExpander();
        expander = AutowireHelper.getInstance().autowireAndProxy(expander);
        ObjectNode configurationNode = JsonNodeFactory.instance.objectNode();
        configurationNode.put("includeDefinitions", true);
        configurationNode.put("generationsToExpand", 1);
        expander.applySettings(configurationNode);
        Substance startingMixture = substanceEntityService.get(UUID.fromString(basicSubstanceId)).get();

        Stream<Substance> expanded = expander.expandRecord(startingMixture);
        Assertions.assertTrue(expanded.anyMatch(r->r.uuid.toString().equals(oneComponentId)));
    }

    @Test
    public void testExpanderMixtureComponentAdditionTop(){
        log.trace("starting testExpanderMixtureComponentAddition");
        log.trace("total substances: {}", substanceRepository.count());
        String basicSubstanceId="ac5bd24d-3453-4373-8578-2ba36f3de999"; //a mixture

        String firstLevelComponent="c3685a23-87a4-47de-8bb3-ab9a9cfe9606";
        String secondLevelComponent="0d1371fc-904f-45e9-b073-ba55dacc4f30";
        BasicRecordExpander expander = new BasicRecordExpander();
        expander = AutowireHelper.getInstance().autowireAndProxy(expander);
        ObjectNode configurationNode = JsonNodeFactory.instance.objectNode();
        configurationNode.put("includeDefinitions", true);
        configurationNode.put("generationsToExpand", 1);
        expander.applySettings(configurationNode);
        Substance startingMixture = substanceEntityService.get(UUID.fromString(basicSubstanceId)).get();

        Stream<Substance> expanded = expander.expandRecord(startingMixture);
        List<Substance> expandedSubstances = expanded.collect(Collectors.toList());
        Assertions.assertTrue(expandedSubstances.stream().anyMatch(r->r.uuid.toString().equals(firstLevelComponent)));
        Assertions.assertTrue(expandedSubstances.stream().noneMatch(r->r.uuid.toString().equals(secondLevelComponent)));
    }

    @Test
    public void testExpanderMixtureComponentAddition2Generations(){
        log.trace("starting testExpanderMixtureComponentAddition");
        log.trace("total substances: {}", substanceRepository.count());
        String basicSubstanceId="ac5bd24d-3453-4373-8578-2ba36f3de999"; //a mixture

        String firstLevelComponent="c3685a23-87a4-47de-8bb3-ab9a9cfe9606";
        String secondLevelComponent="0d1371fc-904f-45e9-b073-ba55dacc4f30";
        String tolueneComponentId="045233d3-1e86-4af5-9e05-cad75504c84c";
        BasicRecordExpander expander = new BasicRecordExpander();
        expander = AutowireHelper.getInstance().autowireAndProxy(expander);
        ObjectNode configurationNode = JsonNodeFactory.instance.objectNode();
        configurationNode.put("includeDefinitions", true);
        configurationNode.put("generationsToExpand", 2);
        configurationNode.put("includeRelated", false);
        expander.applySettings(configurationNode);
        Substance startingMixture = substanceEntityService.get(UUID.fromString(basicSubstanceId)).get();

        Stream<Substance> expanded = expander.expandRecord(startingMixture);
        List<Substance> expandedSubstances = expanded.collect(Collectors.toList());
        expandedSubstances.forEach(s-> System.out.printf("substance: %s\n", s.uuid.toString()));
        Assertions.assertTrue(expandedSubstances.stream().anyMatch(r->r.uuid.toString().equals(firstLevelComponent)));
        Assertions.assertTrue(expandedSubstances.stream().anyMatch(r->r.uuid.toString().equals(secondLevelComponent)));
        Assertions.assertTrue(expandedSubstances.stream().anyMatch(r->r.uuid.toString().equals(tolueneComponentId)));
    }

    @Test
    public void testExpanderStrDivAddition(){
        log.trace("starting testExpanderStrDivAddition");
        log.trace("total substances: {}", substanceRepository.count());
        String basicSubstanceId="f8b16d9d-7593-4baf-a92d-aca92d4dd4be"; //garlic powder

        String relatedId="8eeb12e4-0bd8-44a3-83cd-ac1ab9258239";
        BasicRecordExpander expander = new BasicRecordExpander();
        expander = AutowireHelper.getInstance().autowireAndProxy(expander);
        ObjectNode configurationNode = JsonNodeFactory.instance.objectNode();
        configurationNode.put("includeDefinitions", true);
        configurationNode.put("generationsToExpand", 1);
        expander.applySettings(configurationNode);
        Substance startingMixture = substanceEntityService.get(UUID.fromString(basicSubstanceId)).get();

        Stream<Substance> expanded = expander.expandRecord(startingMixture);
        Assertions.assertTrue(expanded.anyMatch(r->r.uuid.toString().equals(relatedId)));
    }

    @Test
    public void testExpanderRelated(){
        log.trace("starting testExpanderRelated");
        log.trace("total substances: {}", substanceRepository.count());
        String basicSubstanceId="4cf90035-16a7-4b50-b45c-fc1451b122d2";

        String relatedId="c8963d5a-4418-486b-b8a2-e2be95342312";
        BasicRecordExpander expander = new BasicRecordExpander();
        expander = AutowireHelper.getInstance().autowireAndProxy(expander);
        ObjectNode configurationNode = JsonNodeFactory.instance.objectNode();
        configurationNode.put("includeDefinitions",false);
        configurationNode.put("includeRelated",true);
        configurationNode.put("generationsToExpand",1);
        expander.applySettings(configurationNode);
        Substance startingMixture = substanceEntityService.get(UUID.fromString(basicSubstanceId)).get();

        Stream<Substance> expanded = expander.expandRecord(startingMixture);
        List<Substance> fullExpanded = expanded.collect(Collectors.toList());
        Assertions.assertTrue(fullExpanded.stream().anyMatch(r->r.uuid.toString().equals(relatedId)));
    }

    @Test
    public void testExpanderRelated2Generations(){
        log.trace("starting testExpanderRelated");
        log.trace("total substances: {}", substanceRepository.count());
        String startingSubstanceId="bd953285-a7a4-2965-cc7d-73bdceb7abd0";

        String relatedId="0eaa2c2d-6683-412e-8826-75981dcc8e57";
        String relatedSecondGenerationId= "0fd12593-d5ac-47f3-acff-dec15abf0385";

        BasicRecordExpander expander = new BasicRecordExpander();
        expander = AutowireHelper.getInstance().autowireAndProxy(expander);
        ObjectNode configurationNode = JsonNodeFactory.instance.objectNode();
        configurationNode.put("includeDefinitions",false);
        configurationNode.put("includeRelated",true);
        configurationNode.put("generationsToExpand",1);
        expander.applySettings(configurationNode);
        Substance startingMixture = substanceEntityService.get(UUID.fromString(startingSubstanceId)).get();

        Stream<Substance> expanded = expander.expandRecord(startingMixture);
        List<Substance> fullExpanded = expanded.collect(Collectors.toList());
        Assertions.assertTrue(fullExpanded.stream().anyMatch(r->r.uuid.toString().equals(relatedId)));
    }

    @Test
    public void testExpanderRelated2GenerationsPlus(){
        log.trace("starting testExpanderRelated2GenerationsPlus");
        log.trace("total substances: {}", substanceRepository.count());
        String startingSubstanceId="414f48c2-fe3f-4a80-baa0-eee805faf9b1";

        String relatedId="4cf90035-16a7-4b50-b45c-fc1451b122d2";
        BasicRecordExpander expander = new BasicRecordExpander();
        expander = AutowireHelper.getInstance().autowireAndProxy(expander);
        ObjectNode configurationNode = JsonNodeFactory.instance.objectNode();
        configurationNode.put("includeDefinitions",false);
        configurationNode.put("includeRelated",true);
        configurationNode.put("generationsToExpand",1);
        expander.applySettings(configurationNode);
        Substance startingMixture = substanceEntityService.get(UUID.fromString(startingSubstanceId)).get();

        Stream<Substance> expanded = expander.expandRecord(startingMixture);
        List<Substance> fullExpanded = expanded.collect(Collectors.toList());
        Assertions.assertEquals(3, fullExpanded.size());
        Assertions.assertTrue(fullExpanded.stream().anyMatch(s->s.getUuid().toString().equals(relatedId)));
    }

    @Test
    public void testExpanderRelated3GenerationsPlus(){
        log.trace("starting testExpanderRelated2GenerationsPlus");
        log.trace("total substances: {}", substanceRepository.count());
        String startingSubstanceId="414f48c2-fe3f-4a80-baa0-eee805faf9b1";

        String relatedId="c8963d5a-4418-486b-b8a2-e2be95342312";
        String mediatorSubstanceId="045233d3-1e86-4af5-9e05-cad75504c84c";
        BasicRecordExpander expander = new BasicRecordExpander();
        expander = AutowireHelper.getInstance().autowireAndProxy(expander);
        ObjectNode configurationNode = JsonNodeFactory.instance.objectNode();
        configurationNode.put("includeDefinitions",false);
        configurationNode.put("includeRelated",true);
        configurationNode.put("generationsToExpand",3);
        expander.applySettings(configurationNode);
        Substance startingMixture = substanceEntityService.get(UUID.fromString(startingSubstanceId)).get();

        Stream<Substance> expanded = expander.expandRecord(startingMixture);
        List<Substance> fullExpanded = expanded.collect(Collectors.toList());
        fullExpanded.forEach(s-> System.out.printf("substance %s (%s)\n", s.uuid, s.names.get(0).name));
        Assertions.assertTrue(fullExpanded.stream().anyMatch(s->s.getUuid().toString().equals(relatedId)));
        Assertions.assertTrue(fullExpanded.stream().anyMatch(s->s.getUuid().toString().equals(mediatorSubstanceId)));
        Assertions.assertTrue(fullExpanded.size()>1);
    }

    @Test
    public void testExpanderRelated3GenerationsPlus2(){
        log.trace("starting testExpanderRelated2GenerationsPlus");
        log.trace("total substances: {}", substanceRepository.count());
        String startingSubstanceId="414f48c2-fe3f-4a80-baa0-eee805faf9b1";

        String relatedId="c8963d5a-4418-486b-b8a2-e2be95342312";
        String mediatorSubstanceId="045233d3-1e86-4af5-9e05-cad75504c84c";
        BasicRecordExpander expander = new BasicRecordExpander();
        expander = AutowireHelper.getInstance().autowireAndProxy(expander);
        ObjectNode configurationNode = JsonNodeFactory.instance.objectNode();
        configurationNode.put("includeDefinitions",false);
        configurationNode.put("includeRelated",true);
        configurationNode.put("generationsToExpand",4);
        expander.applySettings(configurationNode);
        Substance startingMixture = substanceEntityService.get(UUID.fromString(startingSubstanceId)).get();

        Stream<Substance> expanded = expander.expandRecord(startingMixture);
        List<Substance> fullExpanded = expanded.collect(Collectors.toList());
        fullExpanded.forEach(s-> System.out.printf("substance %s (%s)\n", s.uuid, s.names.get(0).name));
        Assertions.assertTrue(fullExpanded.stream().anyMatch(s->s.getUuid().toString().equals(relatedId)));
        Assertions.assertTrue(fullExpanded.stream().anyMatch(s->s.getUuid().toString().equals(mediatorSubstanceId)));
        Assertions.assertTrue(fullExpanded.size()>1);
    }

}
