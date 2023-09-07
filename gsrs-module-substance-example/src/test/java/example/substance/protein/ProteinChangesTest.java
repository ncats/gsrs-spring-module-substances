package example.substance.protein;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.service.GsrsEntityService;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.EntityFetcher;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.models.v1.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
class ProteinChangesTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    protected SubstanceEntityService substanceEntityService;

    @Test
    void addSubunitTest() throws Exception {

        UUID proteinId = UUID.randomUUID();
        ProteinSubstanceBuilder builder = new ProteinSubstanceBuilder();

        Name name1 = new Name();
        name1.name = "POTE ankyrin domain family member B3";
        name1.type = "cn";
        Reference substanceOneReference = new Reference();
        substanceOneReference.docType = "UNIPROT";
        substanceOneReference.citation = "A0JP26";
        substanceOneReference.url = "https://www.uniprot.org/uniprotkb/A0JP26/entry";
        name1.addReference(substanceOneReference);

        String seq1 =
                "MVAEVCSMPAASAVKKPFDLRSKMGKWCHHRFPCCRGSGKSNMGTSGDHDDSFMKTLRSK\n" +
                        "MGKCCHHCFPCCRGSGTSNVGTSGDHDNSFMKTLRSKMGKWCCHCFPCCRGSGKSNVGTW\n" +
                        "GDYDDSAFMEPRYHVRREDLDKLHRAAWWGKVPRKDLIVMLRDTDMNKRDKQKRTALHLA\n" +
                        "SANGNSEVVQLLLDRRCQLNVLDNKKRTALIKAVQCQEDECVLMLLEHGADGNIQDEYGN\n" +
                        "TALHYAIYNEDKLMAKALLLYGADIESKNKCGLTPLLLGVHEQKQQVVKFLIKKKANLNA\n" +
                        "LDRYGRTALILAVCCGSASIVNLLLEQNVDVSSQDLSGQTAREYAVSSHHHVICELLSDY\n" +
                        "KEKQMLKISSENSNPEQDLKLTSEEESQRLKVSENSQPEKMSQEPEINKDCDREVEEEIK\n" +
                        "KHGSNPVGLPENLTNGASAGNGDDGLIPQRKSRKPENQQFPDTENEEYHSDEQNDTQKQL\n" +
                        "SEEQNTGISQDEILTNKQKQIEVAEKEMNSKLSLSHKKEEDLLRENSMLREEIAMLRLEL\n" +
                        "DETKHQNQLRENKILEEIESVKEKLLKAIQLNEEALTKTSI";
        Protein protein = new Protein();
        Subunit subunit1 = new Subunit();
        subunit1.sequence = seq1;
        protein.subunits.add(subunit1);

        builder.addName(name1);
        builder.setProtein(protein);
        builder.addReference(substanceOneReference);
        builder.setUUID(proteinId);

        GsrsEntityService.CreationResult<Substance> result = substanceEntityService.createEntity(builder.build().toFullJsonNode());
        Assertions.assertTrue(result.isCreated());

        ProteinSubstance retrieved = (ProteinSubstance) EntityFetcher.of(EntityUtils.Key.of(Substance.class, UUID.fromString(proteinId.toString()))).call();

        String seq2 =
                "MGKCCHHCFPCCRGSGTSNVGTSGDHDNSFMKTLRSKMGKWCCHCFPCCRGSGKSNVGTW\n" +
                        "GDYDDSAFMEPRYHVRREDLDKLHRAAWWGKVPRKDLIVMLRDTDMNKRDKQKRTALHLA\n" +
                        "SANGNSEVVQLLLDRRCQLNVLDNKKRTALIKAVQCQEDECVLMLLEHGADGNIQDEYGN\n" +
                        "TALHYAIYNEDKLMAKALLLYGADIESKNKCGLTPLLLGVHEQKQQVVKFLIKKKANLNA\n" +
                        "LDRYGRTALILAVCCGSASIVNLLLEQNVDVSSQDLSGQTAREYAVSSHHHVICELLSDY\n" +
                        "KEKQMLKISSENSNPEQDLKLTSEEESQRLKVSENSQPEKMSQEPEINKDCDREVEEEIK\n" +
                        "KHGSNPVGLPENLTNGASAGNGDDGLIPQRKSRKPENQQFPDTENEEYHSDEQNDTQKQL\n" +
                        "SEEQNTGISQDEILTNKQKQIEVAEKEMNSKLSLSHKKEEDLLRENSMLREEIAMLRLEL\n" +
                        "DETKHQNQLRENKILEEIESVKEKLLKAIQLNEEALTKTSI";
        Subunit subunit2 = new Subunit();
        subunit2.sequence = seq2;
        retrieved.protein.subunits.add(subunit2);
        GsrsEntityService.UpdateResult<Substance> result2 = substanceEntityService.updateEntity(retrieved.toFullJsonNode());
        Assertions.assertNull(result2.getThrowable());
        ProteinSubstance retrieved2 = (ProteinSubstance) EntityFetcher.of(EntityUtils.Key.of(Substance.class, UUID.fromString(proteinId.toString()))).call();
        Assertions.assertEquals(2, retrieved2.protein.subunits.size());
    }

    @Test
    void removeSubunitTest() throws Exception {

        UUID proteinId = UUID.randomUUID();
        ProteinSubstanceBuilder builder = new ProteinSubstanceBuilder();

        Name name1 = new Name();
        name1.name = "POTE ankyrin domain family member B3";
        name1.type = "cn";
        Reference substanceOneReference = new Reference();
        substanceOneReference.docType = "UNIPROT";
        substanceOneReference.citation = "A0JP26";
        substanceOneReference.url = "https://www.uniprot.org/uniprotkb/A0JP26/entry";
        name1.addReference(substanceOneReference);

        String seq1 =
                "MVAEVCSMPAASAVKKPFDLRSKMGKWCHHRFPCCRGSGKSNMGTSGDHDDSFMKTLRSK\n" +
                        "MGKCCHHCFPCCRGSGTSNVGTSGDHDNSFMKTLRSKMGKWCCHCFPCCRGSGKSNVGTW\n" +
                        "GDYDDSAFMEPRYHVRREDLDKLHRAAWWGKVPRKDLIVMLRDTDMNKRDKQKRTALHLA\n" +
                        "SANGNSEVVQLLLDRRCQLNVLDNKKRTALIKAVQCQEDECVLMLLEHGADGNIQDEYGN\n" +
                        "TALHYAIYNEDKLMAKALLLYGADIESKNKCGLTPLLLGVHEQKQQVVKFLIKKKANLNA\n" +
                        "LDRYGRTALILAVCCGSASIVNLLLEQNVDVSSQDLSGQTAREYAVSSHHHVICELLSDY\n" +
                        "KEKQMLKISSENSNPEQDLKLTSEEESQRLKVSENSQPEKMSQEPEINKDCDREVEEEIK\n" +
                        "KHGSNPVGLPENLTNGASAGNGDDGLIPQRKSRKPENQQFPDTENEEYHSDEQNDTQKQL\n" +
                        "SEEQNTGISQDEILTNKQKQIEVAEKEMNSKLSLSHKKEEDLLRENSMLREEIAMLRLEL\n" +
                        "DETKHQNQLRENKILEEIESVKEKLLKAIQLNEEALTKTSI";
        Protein protein = new Protein();
        Subunit subunit1 = new Subunit();
        subunit1.sequence = seq1;
        String seq2 =
                "MGKCCHHCFPCCRGSGTSNVGTSGDHDNSFMKTLRSKMGKWCCHCFPCCRGSGKSNVGTW\n" +
                        "GDYDDSAFMEPRYHVRREDLDKLHRAAWWGKVPRKDLIVMLRDTDMNKRDKQKRTALHLA\n" +
                        "SANGNSEVVQLLLDRRCQLNVLDNKKRTALIKAVQCQEDECVLMLLEHGADGNIQDEYGN\n" +
                        "TALHYAIYNEDKLMAKALLLYGADIESKNKCGLTPLLLGVHEQKQQVVKFLIKKKANLNA\n" +
                        "LDRYGRTALILAVCCGSASIVNLLLEQNVDVSSQDLSGQTAREYAVSSHHHVICELLSDY\n" +
                        "KEKQMLKISSENSNPEQDLKLTSEEESQRLKVSENSQPEKMSQEPEINKDCDREVEEEIK\n" +
                        "KHGSNPVGLPENLTNGASAGNGDDGLIPQRKSRKPENQQFPDTENEEYHSDEQNDTQKQL\n" +
                        "SEEQNTGISQDEILTNKQKQIEVAEKEMNSKLSLSHKKEEDLLRENSMLREEIAMLRLEL\n" +
                        "DETKHQNQLRENKILEEIESVKEKLLKAIQLNEEALTKTSI";
        Subunit subunit2 = new Subunit();
        subunit2.sequence = seq2;
        protein.subunits.add(subunit1);
        protein.subunits.add(subunit2);

        builder.addName(name1);
        builder.setProtein(protein);
        builder.addReference(substanceOneReference);
        builder.setUUID(proteinId);

        GsrsEntityService.CreationResult<Substance> result = substanceEntityService.createEntity(builder.build().toFullJsonNode());
        Assertions.assertTrue(result.isCreated());

        ProteinSubstance retrieved = (ProteinSubstance) EntityFetcher.of(EntityUtils.Key.of(Substance.class, UUID.fromString(proteinId.toString()))).call();

        retrieved.protein.subunits.remove(1);
        GsrsEntityService.UpdateResult<Substance> result2 = substanceEntityService.updateEntity(retrieved.toFullJsonNode());
        Assertions.assertNull(result2.getThrowable());
        ProteinSubstance retrieved2 = (ProteinSubstance) EntityFetcher.of(EntityUtils.Key.of(Substance.class, UUID.fromString(proteinId.toString()))).call();
        Assertions.assertEquals(1, retrieved2.protein.subunits.size());
    }

    @Test
    void pruneSubunitTest() throws Exception {

        UUID proteinId = UUID.randomUUID();
        ProteinSubstanceBuilder builder = new ProteinSubstanceBuilder();

        Name name1 = new Name();
        name1.name = "POTE ankyrin domain family member B3";
        name1.type = "cn";
        Reference substanceOneReference = new Reference();
        substanceOneReference.docType = "UNIPROT";
        substanceOneReference.citation = "A0JP26";
        substanceOneReference.url = "https://www.uniprot.org/uniprotkb/A0JP26/entry";
        name1.addReference(substanceOneReference);

        String seq1 =
                "MVAEVCSMPAASAVKKPFDLRSKMGKWCHHRFPCCRGSGKSNMGTSGDHDDSFMKTLRSK\n" +
                        "MGKCCHHCFPCCRGSGTSNVGTSGDHDNSFMKTLRSKMGKWCCHCFPCCRGSGKSNVGTW\n" +
                        "GDYDDSAFMEPRYHVRREDLDKLHRAAWWGKVPRKDLIVMLRDTDMNKRDKQKRTALHLA\n" +
                        "SANGNSEVVQLLLDRRCQLNVLDNKKRTALIKAVQCQEDECVLMLLEHGADGNIQDEYGN\n" +
                        "TALHYAIYNEDKLMAKALLLYGADIESKNKCGLTPLLLGVHEQKQQVVKFLIKKKANLNA\n" +
                        "LDRYGRTALILAVCCGSASIVNLLLEQNVDVSSQDLSGQTAREYAVSSHHHVICELLSDY\n" +
                        "KEKQMLKISSENSNPEQDLKLTSEEESQRLKVSENSQPEKMSQEPEINKDCDREVEEEIK\n" +
                        "KHGSNPVGLPENLTNGASAGNGDDGLIPQRKSRKPENQQFPDTENEEYHSDEQNDTQKQL\n" +
                        "SEEQNTGISQDEILTNKQKQIEVAEKEMNSKLSLSHKKEEDLLRENSMLREEIAMLRLEL\n" +
                        "DETKHQNQLRENKILEEIESVKEKLLKAIQLNEEALTKTSI";
        Protein protein = new Protein();
        Subunit subunit1 = new Subunit();
        subunit1.sequence = seq1;
        protein.subunits.add(subunit1);
        builder.addName(name1);
        builder.setProtein(protein);
        builder.addReference(substanceOneReference);
        builder.setUUID(proteinId);

        GsrsEntityService.CreationResult<Substance> result = substanceEntityService.createEntity(builder.build().toFullJsonNode());
        Assertions.assertTrue(result.isCreated());

        ProteinSubstance retrieved = (ProteinSubstance) EntityFetcher.of(EntityUtils.Key.of(Substance.class, UUID.fromString(proteinId.toString()))).call();
        String seq1Mod = seq1.substring(30);
        retrieved.protein.subunits.get(0).sequence = seq1Mod;
        GsrsEntityService.UpdateResult<Substance> updateResult = substanceEntityService.updateEntityWithoutValidation(retrieved.toFullJsonNode());
        ProteinSubstance retrieved2 = (ProteinSubstance) EntityFetcher.of(EntityUtils.Key.of(Substance.class, UUID.fromString(proteinId.toString()))).call();
        Assertions.assertEquals(seq1Mod, retrieved2.protein.subunits.get(0).sequence);
    }
}
