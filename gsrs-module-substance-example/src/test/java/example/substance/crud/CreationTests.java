package example.substance.crud;

import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.controllers.EntityFactory;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.EntityUtils.Key;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

/**
 *
 * @author mitch
 */
@Slf4j
@TestMethodOrder(OrderAnnotation.class)
@WithMockUser(username = "admin", roles = "Admin")
public class CreationTests extends AbstractSubstanceJpaFullStackEntityTest {

    public CreationTests() {
    }


    private String fileName = "rep18.gsrs";

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @Autowired
    private SubstanceLegacySearchService searchService;

    @BeforeEach
    public void clearIndexers() throws IOException {
        SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
        AutowireHelper.getInstance().autowire(hashIndexer);
        testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
        {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(ChemicalValidator.class);
            config.setNewObjClass(ChemicalSubstance.class);
            factory.addValidator("substances", config);
        }

        File dataFile = new ClassPathResource(fileName).getFile();
        loadGsrsFile(dataFile);
    }

    //@Override
    @Transactional()
    public Substance fullFetch(Substance s) {
        EntityWrapper ew = EntityWrapper.of(s);
        Key key = ew.getKey();
        Substance ret = substanceRepository.findByKey(key).get();
        //this forces a full fetch
        //but thats not always ideal as sometimes things are thrown away
        //TODO: discuss this
        String json=EntityFactory.EntityMapper.INTERNAL_ENTITY_MAPPER().toJson(ret);
        log.trace("json: ");
        log.trace(json);
        
        if( ret == null) {
            log.trace("ret null");
        }
        else {
            log.trace("ret not null ");
        }
        
        return ret;
    }
    @Test
    @Order(1)
    public void testChemicalCreation() {
        log.trace("started testChemicalCreation");
        ChemicalSubstance tryptophan = buildTryptophan();
        tryptophan.getStructure().setMwt(300.0);
        //tryptophan.getStructure().mwt=300.0;
        ChemicalSubstance savedTrypotophan = substanceRepository.saveAndFlush(tryptophan);
        Assertions.assertNotNull(savedTrypotophan.uuid);
        Assertions.assertEquals(tryptophan.uuid, savedTrypotophan.uuid);
        UUID objectId = savedTrypotophan.uuid;
        log.trace("savedTrypotophan.uuid: " + savedTrypotophan.uuid + "; tryptophan.uuid: " + tryptophan.uuid);
        Optional<Substance> readInTtryptophan = substanceRepository.findById(objectId);
        ChemicalSubstance tryptophan2 = (ChemicalSubstance) readInTtryptophan.get();
        log.trace("mwt: " + tryptophan2.getStructure().mwt);
        Assert.assertNotNull(tryptophan2);
    }

    @Test
    @Order(2)
    public void testChemicalReadAndModify() {
        log.trace("starting in testChemicalRead.  " );
        UUID id = UUID.fromString("ac776f92-b90f-48a0-a54e-7461a60c84b3");

        Optional<Substance> s = substanceRepository.findById(id);
        Assertions.assertEquals(id, s.get().uuid);

        String name = "RTI-113";
        String modName = "RTI-113-mod";
        //ChemicalSubstance substance = (ChemicalSubstance) s.get();
        ChemicalSubstance substance = (ChemicalSubstance) fullFetch(s.get());

        substance.names.forEach(n -> {
            if (n.name.equals(name)) {
                n.name = modName;
            }
        });
        substanceRepository.saveAndFlush(substance);

        Optional<Substance> sMod = substanceRepository.findById(id);
        ChemicalSubstance substanceMod = (ChemicalSubstance) sMod.get();
        Assert.assertTrue(substanceMod.names.stream().anyMatch(n -> n.name.equals(modName)) && substanceMod.names.stream().noneMatch(n -> n.name.equals(name)));
    }

    private ChemicalSubstance buildTryptophan() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();

        String tryMolfile = "\n  Marvin  01132108112D          \n\n 15 16  0  0  1  0            999 V2000\n    4.3175   -6.1954    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n    4.2700   -4.8406    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n    4.7574   -5.4821    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n    3.4937   -5.9933    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n    6.3495   -6.5754    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n    4.9951   -6.5833    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n    3.4937   -5.1136    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n    5.6723   -6.1914    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n    6.3455   -7.3599    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n    5.6683   -5.4068    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n    7.0271   -6.1835    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n    2.7849   -6.3970    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n    2.7095   -4.7098    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n    2.0241   -5.9933    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n    1.9884   -5.1690    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n  2  3  1  0  0  0  0\n  3  1  2  0  0  0  0\n  4  1  1  0  0  0  0\n  5  8  1  0  0  0  0\n  6  1  1  0  0  0  0\n  7  4  2  0  0  0  0\n  8  6  1  0  0  0  0\n  9  5  2  0  0  0  0\n  8 10  1  6  0  0  0\n 11  5  1  0  0  0  0\n 12  4  1  0  0  0  0\n 13  7  1  0  0  0  0\n 14 12  2  0  0  0  0\n 15 14  1  0  0  0  0\n  7  2  1  0  0  0  0\n 15 13  2  0  0  0  0\nM  END";

        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile = tryMolfile;
        structure.setMwt(204.2252);
        structure.formula = "C11H12N2O2";
        ChemicalSubstance tryptophan = builder.setStructure(structure)
                .addName("tryptophan")
                .addCode("FDA UNII", "8DUH1N11BX")
                .build();
        substanceRepository.saveAndFlush(tryptophan);

        System.out.println("mw: " + tryptophan.getMolecularWeight());

        return tryptophan;
    }

}
