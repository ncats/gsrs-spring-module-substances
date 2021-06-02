package example.substance;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.molwitch.Chemical;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.models.v1.*;
import ix.ginas.utils.MolecularWeightAndFormulaContribution;
import ix.ginas.utils.ProteinUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MolCalculationTest extends AbstractSubstanceJpaEntityTest{

    @Autowired
    protected SubstanceRepository substanceRepository;

    @Test
    public void mwIndoleTest() {
        String indoleMolfile = "\n" +
                "  ACCLDraw04162117372D\n" +
                "\n" +
                "  9 10  0  0  0  0  0  0  0  0999 V2000\n" +
                "    5.7278   -5.1901    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.7701   -5.1896    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    6.7509   -4.5998    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.7701   -6.3710    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    5.7278   -6.3763    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    6.7534   -6.9606    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    8.8936   -4.8245    0.0000 N   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "    8.8937   -6.7360    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    9.5880   -5.7802    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  6  4  1  0  0  0  0\n" +
                "  5  6  2  0  0  0  0\n" +
                "  2  3  1  0  0  0  0\n" +
                "  1  5  1  0  0  0  0\n" +
                "  4  2  2  0  0  0  0\n" +
                "  3  1  2  0  0  0  0\n" +
                "  9  7  1  0  0  0  0\n" +
                "  8  9  2  0  0  0  0\n" +
                "  2  7  1  0  0  0  0\n" +
                "  4  8  1  0  0  0  0\n" +
                "M  END\n";
        Double expected = 117.14788;
        Double actual=null;
        try {
            Chemical mol = Chemical.parseMol(indoleMolfile);
            actual = mol.getMass();

        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(expected, actual, 0.001);
        System.out.println("finished mwIndoleTest");
    }
    /**
     * Create an unmodified protein -- just an amino acid sequence -- and calculate its molecular weight
     */
    @Test
    public void proteinMwTestUnmod() {

        System.out.println("starting proteinMwTestUnmod");
        ProteinSubstance proteinSubstance = new ProteinSubstance();
        Protein protein = new Protein();
        Subunit subunit1= new  Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        subunit1.sequence =
                "MKKNYNPKDIEEHLYNFWEKNGFFKPNNNLNKPAFCIMMPPPNITGNLHMGHAFQQTIMD\n" +
                        "ILIRYNRMQGKNTLWQVGTDHAGIATQILIERQIFSEERKTKKDYSRNDFIKKIWKWKKK\n" +
                        "SNFSVKKQMKRLGNSVDWDREKFTLDPDISNSVKEAFIILYKNNLIYQKKRLVHWDSKLE\n" +
                        "TVISDLEVEHRLIKSKKWFIRYPIIKNIKNINIEYLLVATTRPETLLGDTALAINPKDDK\n" +
                        "YNHLIGQSVICPIVNRIIPIIADHYADMNKDTGCVKITPGHDFNDYEVGQRHKLPMINIF\n" +
                        "TFNGKIKSNFSIYDYQGSKSNFYDSSIPTEFQNLDILSARKKIIYEIEKLGLLEKIEECN\n" +
                        "FFTPYSERSGVIIQPMLTNQWYLKTSHLSQSAIDVVREKKIKFIPNQYKSMYLSWMNNIE\n" +
                        "DWCISRQLWWGHQIPVWYDDKKNIYVGHSEKKIREEYNISDDMILNQDNDVLDTWFSSGL\n" +
                        "WTFSTLGWPEKTEFLKIFHSTDVLVSGFDIIFFWIARMIMLTMYLVKDSYGNPQIPFKDV\n" +
                        "YITGLIRDEEGKKMSKSKGNVIDPIDMIDGISLNELIEKRTSNLLQPHLSQKIRYHTIKQ\n" +
                        "FPNGISATGTDALRFTFSALASNTRDIQWDMNRLKGYRNFCNKLWNASRFVLKNTKDHDY\n" +
                        "FNFSVNDNMLLINKWILIKFNNTVKSYRNSLDSYRFDIAANILYDFIWNVFCDWYLEFVK\n" +
                        "SVIKSGSYQDIYFTKNVLIHVLELLLRLSHPIMPFITEAIWQRVKIIKHIKDRTIMLQSF\n" +
                        "PEYNDQLFDKSTLSNINWIKKIIIFIRNTRSKMNISSTKLLSLFLKNINSEKKKVIQENK\n" +
                        "FILKNIASLEKISILSKQDDEPCLSLKEIIDGVDILVPVLKAIDKEIELKRLNKEIEKIK\n" +
                        "SKMLISEKKMSNQDFLSYAPKNIIDKEIKKLKSLNEIYLTLSQQLESLHDAFCKKNKIFN\n";
        proteinSubstance.setProtein(protein);
        Set<String> unknownResidues = new HashSet<>();
        MolecularWeightAndFormulaContribution contribution=ProteinUtils.generateProteinWeightAndFormula(substanceRepository,
                proteinSubstance, unknownResidues);
        double expectedMw = 113269.0;
        double actual =contribution.getMw();
        System.out.println("calculated MW: " + actual);

        assertEquals(expectedMw, actual, 0.9);
    }

    @Test
    public void proteinMwTestMod1() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        UUID uuid = UUID.randomUUID();
       new ChemicalSubstanceBuilder()
                .setStructure("N[C@@H](CC1=CNC2=C1C=CC=C2)C(O)=O")
                .setApproval(admin, new Date(), "8DUH1N11BX")
                .addName("TRYPTOPHAN")
                .setUUID(uuid)
                .buildJsonAnd(this::assertCreated);
        ProteinSubstanceBuilder builder = new ProteinSubstanceBuilder()
                            .addSubUnit("MKKNYNPKDIEEHLYNFWEKNGFFKPNNNLNKPAFCIMMPPPNITGNLHMGHAFQQTIMD" +
                                            "ILIRYNRMQGKNTLWQVGTDHAGIATQILIERQIFSEERKTKKDYSRNDFIKKIWKWKKK" +
                                            "SNFSVKKQMKRLGNSVDWDREKFTLDPDISNSVKEAFIILYKNNLIYQKKRLVHWDSKLE" +
                                            "TVISDLEVEHRLIKSKKWFIRYPIIKNIKNINIEYLLVATTRPETLLGDTALAINPKDDK" +
                                            "YNHLIGQSVICPIVNRIIPIIADHYADMNKDTGCVKITPGHDFNDYEVGQRHKLPMINIF" +
                                            "TFNGKIKSNFSIYDYQGSKSNFYDSSIPTEFQNLDILSARKKIIYEIEKLGLLEKIEECN" +
                                            "FFTPYSERSGVIIQPMLTNQWYLKTSHLSQSAIDVVREKKIKFIPNQYKSMYLSWMNNIE" +
                                            "DWCISRQLWWGHQIPVWYDDKKNIYVGHSEKKIREEYNISDDMILNQDNDVLDTWFSSGL" +
                                            "WTFSTLGWPEKTEFLKIFHSTDVLVSGFDIIFFWIARMIMLTMYLVKDSYGNPQIPFKDV" +
                                            "YITGLIRDEEGKKMSKSKGNVIDPIDMIDGISLNELIEKRTSNLLQPHLSQKIRYHTIKQ" +
                                            "FPNGISATGTDALRFTFSALASNTRDIQWDMNRLKGYRNFCNKLWNASRFVLKNTKDHDY" +
                                            "FNFSVNDNMLLINKWILIKFNNTVKSYRNSLDSYRFDIAANILYDFIWNVFCDWYLEFVK" +
                                            "SVIKSGSYQDIYFTKNVLIHVLELLLRLSHPIMPFITEAIWQRVKIIKHIKDRTIMLQSF" +
                                            "PEYNDQLFDKSTLSNINWIKKIIIFIRNTRSKMNISSTKLLSLFLKNINSEKKKVIQENK" +
                                            "FILKNIASLEKISILSKQDDEPCLSLKEIIDGVDILVPVLKAIDKEIELKRLNKEIEKIK" +
                                            "SKMLISEKKMSNQDFLSYAPKNIIDKEIKKLKSLNEIYLTLSQQLESLHDAFCKKNKIFN");

        SubstanceReference substanceReference = newTransactionTemplate().execute( s->
             substanceRepository.findSummaryByUuid(uuid).get().toSubstanceReference()
        );

        StructuralModification modification = new StructuralModification();
        modification.structuralModificationType = "AMINO_ACID_SUBSTITUTION";
        List<Site> sites = new ArrayList<>();
        Site newSite= new Site();
        newSite.residueIndex=1;
        newSite.subunitIndex=1;
        sites.add(newSite);
        modification.setSites(sites);
        modification.extent="COMPLETE";
        modification.molecularFragment = substanceReference;
        modification.residueModified="1_1";

        builder.addStructuralModification(modification);



        Set<String> unknownResidues = new HashSet<>();

        MolecularWeightAndFormulaContribution contribution=ProteinUtils.generateProteinWeightAndFormula(substanceRepository,
                builder.build(), unknownResidues);
        contribution.getMessages().forEach(m->{
            System.out.printf("message: %s; ", m.message);
        });
        double valineMw= 117.1463;
        double isoleucineMw = 113.15764;
        double trypophanMw= 204.2;
        double methionineMw =149.2;
        double expectedMw = 113269.0 - methionineMw + trypophanMw;
        double actual =contribution.getMw();
        System.out.println("calculated MW: " + actual);

        assertEquals(expectedMw, actual, 0.9);
    }

}

