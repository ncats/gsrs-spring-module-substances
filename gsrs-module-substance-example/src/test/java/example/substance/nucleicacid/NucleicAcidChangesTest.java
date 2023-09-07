package example.substance.nucleicacid;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.service.GsrsEntityService;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.EntityFetcher;
import ix.core.models.Keyword;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.NucleicAcidSubstanceBuilder;
import ix.ginas.models.v1.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
class NucleicAcidChangesTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    protected SubstanceEntityService substanceEntityService;

    @Test
    void addSubunitTest() throws Exception {
        UUID nucleicAcidId= UUID.randomUUID();
        NucleicAcidSubstanceBuilder builder = new NucleicAcidSubstanceBuilder();
        NucleicAcid nucleicAcid = new NucleicAcid();
        nucleicAcid.setNucleicAcidType("blah");
        Subunit subunit1 = new Subunit();
        String sequence1 = "ATATAAAAAG";
        subunit1.sequence=sequence1;
        nucleicAcid.subunits.add(subunit1);
        builder.setNucleicAcid(nucleicAcid);
        builder.setUUID(nucleicAcidId);
        Reference referenceForSubstance = new Reference();
        referenceForSubstance.docType="GenBank";
        referenceForSubstance.citation="GGGGG";
        builder.addReference(referenceForSubstance);
        Name name1 = new Name();
        name1.name="Some Sequence";
        name1.type="cn";
        name1.languages.add(new Keyword("en"));
        name1.addReference(referenceForSubstance);
        builder.addName(name1);

        GsrsEntityService.CreationResult<Substance> result= substanceEntityService.createEntity(builder.buildJson());

        NucleicAcidSubstance retrieved = (NucleicAcidSubstance) EntityFetcher.of(EntityUtils.Key.of(Substance.class, nucleicAcidId)).call();
        Subunit subunit2 = new Subunit();
        String sequence2 = "CGCCCCC";
        subunit2.sequence=sequence2;
        retrieved.nucleicAcid.subunits.add(subunit2);
        GsrsEntityService.UpdateResult<Substance> updateResult= substanceEntityService.updateEntity(retrieved.toFullJsonNode());
        NucleicAcidSubstance retrieved2 = (NucleicAcidSubstance) EntityFetcher.of(EntityUtils.Key.of(Substance.class, nucleicAcidId)).call();
        Assertions.assertEquals(2, retrieved2.nucleicAcid.subunits.size());
    }

    @Test
    void removeSubunitTest() throws Exception {
        UUID nucleicAcidId= UUID.randomUUID();
        NucleicAcidSubstanceBuilder builder = new NucleicAcidSubstanceBuilder();
        NucleicAcid nucleicAcid = new NucleicAcid();
        nucleicAcid.setNucleicAcidType("blah");
        Subunit subunit1 = new Subunit();
        String sequence1 = "ATATAAAAAG";
        subunit1.sequence=sequence1;
        nucleicAcid.subunits.add(subunit1);
        Subunit subunit2 = new Subunit();
        String sequence2 = "CGCCCCC";
        subunit2.sequence=sequence2;
        nucleicAcid.subunits.add(subunit2);
        builder.setNucleicAcid(nucleicAcid);
        builder.setUUID(nucleicAcidId);
        Reference referenceForSubstance = new Reference();
        referenceForSubstance.docType="GenBank";
        referenceForSubstance.citation="GGGGG";
        builder.addReference(referenceForSubstance);
        Name name1 = new Name();
        name1.name="Some Sequence";
        name1.type="cn";
        name1.languages.add(new Keyword("en"));
        name1.addReference(referenceForSubstance);
        builder.addName(name1);

        GsrsEntityService.CreationResult<Substance> result= substanceEntityService.createEntity(builder.buildJson());
        NucleicAcidSubstance retrieved = (NucleicAcidSubstance) EntityFetcher.of(EntityUtils.Key.of(Substance.class, nucleicAcidId)).call();
        Assertions.assertEquals(2, retrieved.nucleicAcid.subunits.size());
        retrieved.nucleicAcid.subunits.remove(1);
        GsrsEntityService.UpdateResult<Substance> updateResult= substanceEntityService.updateEntity(retrieved.toFullJsonNode());
        NucleicAcidSubstance retrieved2 = (NucleicAcidSubstance) EntityFetcher.of(EntityUtils.Key.of(Substance.class, nucleicAcidId)).call();
        Assertions.assertEquals(1, retrieved2.nucleicAcid.subunits.size());
    }

    @Test
    void changeSequenceTest() throws Exception {
        UUID nucleicAcidId= UUID.randomUUID();
        NucleicAcidSubstanceBuilder builder = new NucleicAcidSubstanceBuilder();
        NucleicAcid nucleicAcid = new NucleicAcid();
        nucleicAcid.setNucleicAcidType("blah");
        Subunit subunit1 = new Subunit();
        String sequence1 = "ATATAAAAAG";
        subunit1.sequence=sequence1;
        nucleicAcid.subunits.add(subunit1);
        String sequence2 = "CGCCCCC";
        builder.setNucleicAcid(nucleicAcid);
        builder.setUUID(nucleicAcidId);
        Reference referenceForSubstance = new Reference();
        referenceForSubstance.docType="GenBank";
        referenceForSubstance.citation="GGGGG";
        builder.addReference(referenceForSubstance);
        Name name1 = new Name();
        name1.name="Some Sequence";
        name1.type="cn";
        name1.languages.add(new Keyword("en"));
        name1.addReference(referenceForSubstance);
        builder.addName(name1);

        GsrsEntityService.CreationResult<Substance> result= substanceEntityService.createEntity(builder.buildJson());
        NucleicAcidSubstance retrieved = (NucleicAcidSubstance) EntityFetcher.of(EntityUtils.Key.of(Substance.class, nucleicAcidId)).call();
        retrieved.nucleicAcid.subunits.get(0).sequence=sequence2;
        GsrsEntityService.UpdateResult<Substance> updateResult= substanceEntityService.updateEntity(retrieved.toFullJsonNode());
        NucleicAcidSubstance retrieved2 = (NucleicAcidSubstance) EntityFetcher.of(EntityUtils.Key.of(Substance.class, nucleicAcidId)).call();
        Assertions.assertEquals(sequence2, retrieved2.nucleicAcid.subunits.get(0).sequence);
    }
}
