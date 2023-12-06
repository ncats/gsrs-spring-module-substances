package example.prot;

import gov.nih.ncats.common.util.SingleThreadCounter;
import gov.nih.ncats.molwitch.Chemical;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.validator.ValidationMessage;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.PolymerSubstanceBuilder;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.*;
import ix.ginas.utils.MolecularWeightAndFormulaContribution;
import ix.ginas.utils.ProteinUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class ProtCalculationTest extends AbstractSubstanceJpaEntityTest {

    private static final String CV_AMINO_ACID_SUBSTITUTION = "AMINO_ACID_SUBSTITUTION";
    private static final Double CELLULOSE_SULFATE_MW =470000.0;
    private static final Double PROLINE_MW = 115.1305;
    private static final Double WATER_MW = 18.015;
    private static final Double LARGE_PROTEIN_MW_TOLERANCE = 12.0;
    private static final Double MW_HIGH_OFFSET =1000.0;
    private static final Double MW_LOW_OFFSET =-1000.0;
    private static final Double MW_HIGHLIMIT_OFFSET =1000.0;
    private static final Double MW_LOWLIMIT_OFFSET =-1000.0;
    

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
        double expected = 117.14788;
        Double actual=null;
        try {
            Chemical mol = Chemical.parseMol(indoleMolfile);
             actual = mol.getMass();

        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(expected, actual, 0.001);
    }

    @Test
    public void proteinMwTestUnmod() {
        /*
        Create an unmodified protein -- just an amino acid sequence -- and calculate its molecular weight
         */

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

        assertEquals(expectedMw, actual, 0.9);
    }

    @Test
    public void proteinMwTestDisulfides() {
        /*
        Create an unmodified protein -- just an amino acid sequence -- and calculate its molecular weight
         */
        ProteinSubstance proteinSubstance = new ProteinSubstance();
        Protein protein = new Protein();
        Subunit subunit1= new  Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        subunit1.sequence =
                "MKKNYNPKDIEEHLYNFWEKNGFFKPNNNLNKPAFCIMMPPPNITGNLHMGHAFQQTIMD" +
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
                "SKMLISEKKMSNQDFLSYAPKNIIDKEIKKLKSLNEIYLTLSQQLESLHDAFCKKNKIFN";
        DisulfideLink dsLink = new DisulfideLink();
        List<Site> dsSites = new ArrayList<>();
        dsSites.add(new Site(1, 36));
        dsSites.add(new Site(1, 251));
        dsLink.setSites(dsSites);
        protein.getDisulfideLinks().add(dsLink);
        proteinSubstance.setProtein(protein);
        Set<String> unknownResidues = new HashSet<>();
        MolecularWeightAndFormulaContribution contribution=ProteinUtils.generateProteinWeightAndFormula(substanceRepository,
                proteinSubstance, unknownResidues);
        double expectedMw = 113269.0 -2;
        double actual =contribution.getMw();

        assertEquals(expectedMw, actual, 0.9);
    }


    @Test
    public void proteinMwTestMod1() {
        /*
        Create an unmodified protein -- just an amino acid sequence -- and calculate its molecular weight
         */
        ProteinSubstance proteinSubstance = new ProteinSubstance();
        Protein protein = new Protein();
        Subunit subunit1= new  Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        subunit1.sequence =
                "MKKNYNPKDIEEHLYNFWEKNGFFKPNNNLNKPAFCIMMPPPNITGNLHMGHAFQQTIMD" +
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
                        "SKMLISEKKMSNQDFLSYAPKNIIDKEIKKLKSLNEIYLTLSQQLESLHDAFCKKNKIFN";
        StructuralModification modification = new StructuralModification();
        modification.structuralModificationType = CV_AMINO_ACID_SUBSTITUTION;
        List<Site> sites = new ArrayList<>();
        Site newSite= new Site();
        newSite.residueIndex=1;
        newSite.subunitIndex=1;
        sites.add(newSite);
        modification.setSites(sites);
        modification.extent="COMPLETE";
        modification.molecularFragment = new SubstanceReference();
        /*
        use nucleic acid validation test
         */
        ChemicalSubstance tryptophan= buildTryptophan();

        modification.molecularFragment.refuuid=tryptophan.getUuid().toString();
        //modification.molecularFragment.approvalID=tryptophan.approvalID;
        modification.residueModified="1_1";
        Modifications mods = new Modifications();
        mods.structuralModifications.add(modification);
        //protein.setModifications(mods);
        proteinSubstance.setModifications(mods);
        proteinSubstance.setProtein(protein);

        Set<String> unknownResidues = new HashSet<>();

        MolecularWeightAndFormulaContribution contribution=ProteinUtils.generateProteinWeightAndFormula(substanceRepository,
                proteinSubstance, unknownResidues);
        contribution.getMessages().forEach(m-> System.out.printf("message: %s; \n", m.message));
        double trypophanMw= 204.2;
        double methionineMw =149.2;
        double expectedMw = 113269.0 - methionineMw + trypophanMw;
        double actual =contribution.getMw();

        assertEquals(expectedMw, actual, 0.9);
    }

    @Test
    public void proteinMwTestMod2() {
        ProteinSubstance proteinSubstance = new ProteinSubstance();
        Protein protein = new Protein();
        Subunit subunit1= new  Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        subunit1.sequence =
                "MKKNYNPKDIEEHLYNFWEKNGFFKPNNNLNKPAFCIMMPPPNITGNLHMGHAFQQTIMD" +
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
                        "SKMLISEKKMSNQDFLSYAPKNIIDKEIKKLKSLNEIYLTLSQQLESLHDAFCKKNKIFN";
        StructuralModification modification = new StructuralModification();
        modification.structuralModificationType = CV_AMINO_ACID_SUBSTITUTION;
        List<Site> sites = new ArrayList<>();
        /*Site newSite= new Site();
        newSite.residueIndex=1;
        newSite.subunitIndex=1;*/
        sites.add(new Site(1,1));
        sites.add( new Site(1,2));
        modification.setSites(sites);
        modification.extent="COMPLETE";
        modification.molecularFragment = new SubstanceReference();
        /*
        use nucleic acid validation test
         */
        ChemicalSubstance tryptophan= buildTryptophan();

        modification.molecularFragment.refuuid=tryptophan.getUuid().toString();
        Modifications mods = new Modifications();
        mods.structuralModifications.add(modification);
        proteinSubstance.setModifications(mods);
        proteinSubstance.setProtein(protein);

        Set<String> unknownResidues = new HashSet<>();

        MolecularWeightAndFormulaContribution contribution=ProteinUtils.generateProteinWeightAndFormula(substanceRepository,
                proteinSubstance, unknownResidues);
        contribution.getMessages().forEach(m-> System.out.printf("message: %s; \n", m.message));
        double lycineMw = 146.189;
        double trypophanMw= 204.2;
        double methionineMw =149.2;
        double expectedMw = 113269.0 - methionineMw -lycineMw + 2*trypophanMw;
        double actual =contribution.getMw();

        assertEquals(expectedMw, actual, 0.9);
    }

    @Test
    public void proteinMwTestMod3() {
        ProteinSubstance proteinSubstance = new ProteinSubstance();
        Protein protein = new Protein();
        Subunit subunit1= new  Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        subunit1.sequence =
                "MKKNYNPKDIEEHLYNFWEKNGFFKPNNNLNKPAFCIMMPPPNITGNLHMGHAFQQTIMD" +
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
                        "SKMLISEKKMSNQDFLSYAPKNIIDKEIKKLKSLNEIYLTLSQQLESLHDAFCKKNKIFN";
        StructuralModification modification = new StructuralModification();
        modification.structuralModificationType = CV_AMINO_ACID_SUBSTITUTION;
        List<Site> sites = new ArrayList<>();
        sites.add(new Site(1, 4));
        modification.setSites(sites);
        modification.extent="COMPLETE";
        modification.molecularFragment = new SubstanceReference();
        /*
        use nucleic acid validation test
         */
        Substance polymer = buildPolymer();

        modification.molecularFragment.refuuid=polymer.getUuid().toString();
        Modifications mods = new Modifications();
        mods.structuralModifications.add(modification);
        proteinSubstance.setModifications(mods);
        proteinSubstance.setProtein(protein);

        Set<String> unknownResidues = new HashSet<>();

        MolecularWeightAndFormulaContribution contribution=ProteinUtils.generateProteinWeightAndFormula(substanceRepository,
                proteinSubstance, unknownResidues);

        double asparagineMw = 132.119; //wikipedia
        double expectedMw = 113269.0 - asparagineMw + CELLULOSE_SULFATE_MW;
        double actual =contribution.getMw();
        assertEquals(expectedMw, actual, 0.9);
    }


    @Test
    public void proteinMwTestModOnX() {
        ProteinSubstance proteinSubstance = new ProteinSubstance();
        Protein protein = new Protein();
        Subunit subunit1= new  Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        subunit1.sequence =
                "XMAKNY";
        StructuralModification modification = new StructuralModification();
        modification.structuralModificationType = CV_AMINO_ACID_SUBSTITUTION;
        List<Site> sites = new ArrayList<>();
        sites.add(new Site(1, 1));
        modification.setSites(sites);
        modification.extent="COMPLETE";
        modification.molecularFragment = new SubstanceReference();

        Substance proline = buildProline();

        modification.molecularFragment.refuuid=proline.getUuid().toString();
        Modifications mods = new Modifications();
        mods.structuralModifications.add(modification);
        proteinSubstance.setModifications(mods);
        proteinSubstance.setProtein(protein);

        Set<String> unknownResidues = new HashSet<>();

        MolecularWeightAndFormulaContribution contribution=ProteinUtils.generateProteinWeightAndFormula(substanceRepository,
                proteinSubstance, unknownResidues);

        double xMw = 0.0; //x is not recognized and contributes 0
        double expectedMw = 625.801 - xMw + PROLINE_MW-WATER_MW;
        double actual =contribution.getMw();
        assertEquals(expectedMw, actual, 0.9);
    }

    @Test
    public void proteinMwTestModWithHighAndLow() {
//        System.out.println("starting proteinMwTestModWithHighAndLow");
        ProteinSubstance proteinSubstance = new ProteinSubstance();
        Protein protein = new Protein();
        Subunit subunit1= new  Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        subunit1.sequence =readSequenceFromFile();
                //"MEEITQIKKRLSQTVRLEGKEDLLSKKDSITNLKTEEHVSVKKMVISEPKPEKKEDIQLKKKEVVAVAKKEEVLKKEVVVPSKKDEEILPLKKEVPRPPKKEEDVMPQKKEVPRPPKKEEDIVPQMRDVSLPPKEEEKIVPKKKEVPRPPKKVEEILPPKKEVHRPPKKEEDIVPQIREVSLPPKKDEEIVCEKKEVAPAKEEPSKKPKVPSLPATQREDVIEEIIHKKPTAALSKFEDVKEHEEKETFVVLKKEIIDAPTKKEMVTAKHVIVPQKEEIIPSPTQEEVVSFKRKQTVRTSKKDAVPQKKEITYTQQTLEDKEEKILKRLEVTSTPDEEEIAHIQKKLYHTVRLVEKDVFPEKEDITMLETEEFVSQEIKLVSEPKPEKEKEIQGKKKVPPVSKKEEPLHHPKMDEKIVLKQKDVTLSHRKDEETVPQKKDPILALRKDEEIVTQKKDVTPPLIKEEESVPQNKDVTRPLRKEEESVPQKKDVTRPLRKDEETIPQKKDVTLPHGKDEETVPQKKDVTRPLRKDEEIIPQKKDVTRPLRKDGETVPQKKDVTLPHRKDEEIIPQKKDVTRPLRKDGETVPQKKDVTLPHRKEEESVPQKKDVTLPPRKDEESVPQKKDTTGPLIKDEETVPQKKDVTLPHRKDEETVPQKKDVTLRKDEETVPQKKDVTLPHRKDEEIIPQKKDVILRKDEETVPQKKDVTLPHRKDEETVPQKKDVTLPLRKDKLSELTYKKKEDIIPIKEEVVAVDEKEEAILPRKKEIFLHSKKDEDIKPKKKQVAPTKVEKKPSVEPSVVPKETTVFPLEVKEHDKKAEDKDIPKPKEEKRIPTKVQSPKEAEKPRPGPKEEPVPLVQPVEAADKEPVSAPGQVKKGKVLRVKKEEEKVEMPVLKKTSRVSKDKEEDKEMIKLKKVLKTQSAEHEESQKVYVEAKTQAIITESYEAEMHLESYETIKRVEKMPSEVGKKKPIEPAQEPKQEKPESEADEKPKKEIATKVPKEDIPEEPSLALKKVKKLQLETKDEECVKLKPFEKPVKPSPEAEKAPPNDEKERKPISFEKRKEPSTSQDIEWPEKVDKTKGLDDKMVLPKKITPVKTDVTPKEDEKKPIVPQKGILPKETEEKEEITLKPIEHAKKDLKPKNIPSPRVEKTKPIETVSVEKKLSKDLAKKPKTVSPKVSLEAVTLKKVPKKVSPKEDKAKETRTISEAEKVPVMKELSPGAVELTKVPTQPEEEVFEEEAEAEAEFEAQDEDEAWGWEVASRDSYGSEGSEYLEEGALETPGMPGGRREGKPKEEVGKARQTPSPGDGGRGRGLRPGAGGDKPPGDAPIGFQLKPVPLKFVKELKDIMLQEAESVGSSVVFECQISPSTAITTWMKDGSNLRESPKHKFTSDGKDRKLAIIDVQLSDSGEYTCVGKLGNKEKTSTAKLIVEELPVKFTKDLEEEMSVIKGQPMYLSCELSKDREVVWKKDGKELKPAPGKVAINVIGLQRTVTIHDSNDDDAGVYTCECENLKTQVNVKIIEIIRDWLTKPLRDQHVKPKATATFKGDLFKDTPNWKWFKGNDEIPMEPSDKFEVKKDGKEVTLTIKNAQPGDVGEYGIEIEGRRYAAKLTLGEREAEILKPLASIEVVEKEEASFETEISEEDVVGEWKLRGQVLTRSPTCDIRMEGKKRYLTLKNVELDQAGEVSYQALNGVTSAMLTVKEIEMDFTVPLTDVTVHEKKQAKFECTITKDVPKVMWLRGSDIITSDQKYDIIDDGKKHILVINQCEFDDEGEYTIEVLGKTSPAKLTVEGMRLKVISAISDQTVKEEGDAYFTVKLQDYTAVEKDEVTLDCELSKDVPVKWFHNEAEIKASKMVSMKVDGKRRMLCIKKVEDKDKGQYACDCGTDKTAATVTIEARDIKVVRPMYGVELFDGETARFEVEISEDDVHGQWKLNGEVLSPSPDVEIIEDGAKHILTLYNCKVLQTGEISFQGANAKCSANLKVKELPITFVTPLTDVHVYEKDEARFECEVSRQPKTLRWLKGPVDITTDDKFELLQEGKRHTLVVKSAAYEDEAKYMFEAEDKKTSAKLVIQGIRLEFVRPIKDVTVKERETAEFRIELSHEKVQVSWYKNDVRLHPSKVVHLSEDGKIHTLSFKEVSLDDTSLIKVEALGKTCEAMLTVLEGEPYFTTKLQDYTAVEKDEVVLMCEVSKSAAEVKWFKDGKEIIPSKNILIKAEGKKRILTVRKAEKANIGEYLCDCGSDKTAAKLNTEERDIKIVRPLYSVEVTETETARFETEISEEDVHGNWKLKEETLHHSPDCEKKEEGTKHILILYNVRMDMAGSVDFSAANAKSRAQLRVKEPPVEFTKPLEDQTVEEEATAELECEVSRENAEVRWFKDGQEIHKTKKFDMVVDGRKRKLIIHESTIDDSKTYTCDAQKFKTSAFLNVEPPHVEFTKPLHDVEVKEKESARFECKVSRETAKVRWFKDGSEIRKGKKYEIISEGVKRILIISKSVFDDEAEYECDARTSKTSGMLTVVEEEARFTKNLANVEGTETDSVKLICEVSKPDAEVTWYKGDQELPEVGRYEHIADGKKRILIIKDLQMEDAGEYHCKLSSSQSTGSLRINELAAEFISRPQNQEVVEGEKAEFVCSVSKDTYEVKWVKGDNQLQSDDKYDIISDGKKRVLVIKSCELKDEGGFVAVIGTTRAPADLIVIEKLRIITPLKDLIANEGQETVLNCEVNTEGAKAKWLKNDETLFESSKFIMVQKDNVFSLRIKDTQKPNEGNYTIMLTNQRGEQAKSAASITVQEEDLRIIVPPEDVDTQEKRTISFSCKVNRPNVTVQWMKAGQEITFGKRILYRVDKDKHTLTIKDCTLADEGEYTVVGGADKASAELIISEAPTDFTAQLQDQTITEFEDAEFTCELSKEKAEIKWYRDGREIREGPRYQFERDGKTCRLRIKECRPDDECEYACGVDDKKTRARLFVEETPVEIIRPPADVFEPPGSDVVYEVELNKDRVEVKWLRNNMTVVQGDKYQMMSEGKIHRLQVCEIRPRDQGEYRVIAKDKDARAKLELAAVPTIKTLDQDLVTDAGKPFVMTIPYNAYPHAEAEWFFDSISLPKDNIHSSTDRTEYRLKDPKKSEEGRYKIIIQNKHGKGEAFINLKVVDVPGSVKNLQVVDTADGEVSIAWEEPDSDGGSKILAYVVERRNIKRKTWTLATDSADSTEYCVTGLQKDSKYLFRVCARNRVGSGPSIETDKAVQAKNKFDVPDPPQNVIVGNVNKFGATVSWEPPLSDGGSEITSYIIELRDRTSVNWAPVMVTKPHERTAIINDVIENKEYIFRVKAENRAGIGKPSAATNPVKIMDPIERPSPPLNLTHSEQTKDSCLLTWETPLKNGGTPITGYIIERCEEGSEKWLRCNARLSQDLVYRMSGLKFGTKYSYRVIAENAAGQSDSSNIVGPVLVDDPHFAPTLDLSAFKDGLEVIVPHPLAIRVPITGYPVPTAKWTFGETELAAGDRVSMVTKATFTELVITPSVRPDRGTYSLTLENDVTSVSGDIDVNVIASPSAPKDLKVAEVTRRHVHLMWEAPDHDGGSSITGYQVEKREVSRKTWVKVMAGLQDQEYTITDVVEGKEYLFRVIACNKCGPGEPAYIDEPVNVSSPATVPDPPENLKWRDKSASKIFLSWEPPKWDGGTVIKGYIIDKCQRGTDKWKPCGEPVPELKFEVTGLIEGQWYAYRVRALNRLGASRPCKATDEILAVDPKEPPEIQLDAKLLAGLTAKAGTKIELPADITGKPEPKVKWTKADLVLKPDDRITIDAKPGHSTLSIAKTKRDDTATYIIEAVNSSGRATATVDVNILDKPGPPAAFDISEITSESCLLSWNPPRDDGGSKVTNYIVERRALDSEIWYKLSSTVKQTTYKATKLVAFKEYVFRVYAENQFGVGAQAEHAPIIARYPFDTPGPPYKLETSDIAKDSVTLNWYEPDDDGGSPITGYWVERYEPDHDKWIRCNKLPIRDTNFRVKGLPTRKKYKFRVLAENLAGPGKPSKETDQILIKDPIDPPWAPGKPTVKDVAKTSAFLHWTKPEHDGGAKIESYIVELLKSGTDEWVRVADGIPTLEHFLRGLMEKQEYSFRVRAVNAAGESEPSEPSDPVLCKERLNPPSPPRWLLVVTSTRNSAELKWTAPERDGGSPVTNYIIEKRDVKRKGWQVVDTTVKELKYTASPLNEGSLYVFRVAAENAVGPSEYCELADSVLAKDTFGTPGPPYNLTVTEVSKSHVDLKWDAPQKDGGRPVLRYVIEKKEKLGTRWVKSGKTSGPDCHYRVTDVIEGTEVQFKVSAENEAGIGHPSEPTDIIVIEDPTGPPSPPQDLHITEAARDHISISWKAPDKNGGSPVIGYNIELCEAGTEKWMRVNSRPVKELKFRAGEEEGILPEKQYTFRVRAVNSVGASEPSEISESVYAKDSDCNPTLDFQTKDLVVVEGEKMHLPIPFRAVPAPKITWHKDGSELKADDRIFFRTEYTSCHLEIPSCLHADGGQYKVTLENGLGAASGTINVKVIGLPGPCKEIVDSEITKNSCKVSWDPPDYDGGSPVLHFVLQRREAGRRTYINVMSGENKLVWQVKDLIQNGEYYFRVRAVNKIGGGEFIELRNPVIAEDQKQRPDPPIEVETHNPTSESVTLTWKPPMYDGGSKIMGYILEKMMKGEDNFVRCNDFLVPVLSYTVKGLKEGNQYQFRVYAENAAGVSDPSRSTPLIKAIDAIDRPKVFLSGSLQSGLIVKRGEEMRLDAYISGFPYPQITWTRNNSSIWPEALKKRPEKPIRKKKEEKKEEKKEEDKEPKKEEDKKEEDKEKKEEIKEKKEEEKEQEVEQPEEPEEAYHPSLNERLTIDSKRKGESYIIVKDTIRADHGVFTIKVENDHGVASASCEVNILDTPGPPVNFSFEEVRKNSIICKWDPPLDDGGSEIFNYTLERKDNSKIELGWITVTSTLRGCRYPVTKLIEGKQYIFRVTAENKYGPGIPCISKPIIAKNPFDPPEAPEKPEIVYVTSNSMVVTWNEPNDNGSPIQGYWVEKREINSTHWARVNRIIVPDLEITVLGLLEGMTYIFRVCAENVAGPGKFSPPSEPKTAQAAIMPPGPPIPRVVETTDYSIDVEWDPPADNGGADIFGYHVDKVVAGTKDWSRATERPQKSRTFTVYGVREGAKYIVRVVAVNCAGEGAPGLTDAVIVRNPAEGPVIELDISVRNGVVVRAGEMLRIPAHVTGKPFPFLKWTKDDGDLEKDCMEVEEAGNDSTVVIKCTKRSDHGKYHIQAANPSGIKSASTRVEVMDVPGPVLDLKPVVVTRKLMMLNWSDPDDDGGSDVTGFIIERREPKMHTWRQPIETPSSKCEIVGIIEGQEYIFRVVAKNKFGCGPPVDLGPIRAVDPQGPPTSPEKFHYTERTKSSVTIEWRPPRNDGGSPIMGYIIEKKRQDQPAFQRINEELCTAQIMTIENLDELHLYEFRAKAVNAIGESEPSITMTVVIQDDEVAPSLHMLKHFKGDLIRARKNEPIEMPAEVTGLPMPKIEWLKDDVVIDMPTEKLLIETKEIDRVTSHTKLSIPGVVRLDKGTYTVNASNRLGSVSHHITVEVLDRPTPPRNIAFSNIKAESCQLTWDAPLDTGGSELTNYIVEMKDLNGEDPEKAEWVNVTNTIIERRYGVWNLETGGNYKFRVKAENKYGISEACETEEIQIRDPLALPGPPEKVTIAEYSKAHILLTWEPPMDSGGSMITGYWIEKREKGTSYWSRVNKVMVSKRGVKGWDYMVTRLIEETEYEFRVMACNAAGIGPPSATSESAIAVDPLTPPSMPAAPEIADKTRHSVTLSWTPPGKDGGRPIKGYIIEIQDEGTSEWARINDAENLHPSTLFTIPNLPELKKYRFRIIAVNEIGESEPSPRTTEVRIEDIQTAPKIFMDISAHDLLCIRAGTPFKIPATITGRSVPKVTWEFDGKAKTEKKDRLHVLPVDSQVESNDTNSTVIVPVSLRSHSGRYTITAKNKSGQKHVNVRVNVLDVPGAPKELKVTDVTRTTMRLIWKLPDNDGGERIKSYFIEKKAVNGKAWTTVSPACASMALVVPNLLEGQDYLFRIRAENRLGFGPFTETSEPVRARDPIYPPDPPTKVNINLVTKNTVTLTWVPPKNDGGAPVKHYIIERLSWDTSGPQKETWKQCNKRDVEETTFIVEDLKEGGEYEFRVKAVNDAGASRPSVTAGPVIIKDQTCPPNIDLREALEGAEGFDITIVSRIQGCPFPSLVWQKSPLDNPDDKTSVQYDKHVNKLVSDDKCTLLIQQSKRDDSAVYTLTATNSLGTASKSIKLNILGRPGVPVGPIKFEEVFAERIGLSWKPPTDDGGSKITNYVVEKREENRKTWVHVSSDPKECQYVVQRLTEGHEYEFRVMAQNKYGVGPPLYSEPEKARNLFTVPGQCDKPTVTDVALESMTVNWEEPEYDGGSPLTGYWLERKETTAKRWARVNRDPIRIRPMGVSHVVTGLMEGAIYQFRVIAMNAAGCGLPSLPSDPVVCRDPIAPPGPPTPKVTDCTKSTVDLEWIPPLVDGGSKITGYFVEYKEEGQEEWEKVKDKEIRGCKFVVPGLKELGHYRFRVRAVNAAGVGEPGEVAEVIEVKDRTIPPEVDLDASVKEKIIVHAGGVIRLLAYVSGKPAPEIIWNRDDADLPKEAVVETTSISSALVIKNCRRQHQGIYTLTAKNAGGERRKAIIVEVLDVPGPVGQPFSGENLTTDSCKLTWYSPEDDGGSAITNYIIEKREADRRGWTSVTYTVTRHNAVVQGLIDGKGYYFRIAAENIIGMGPFTETSAPVVIKDPLSVPERPEDLQVTAVTNDSISVSWRPPKYDGGSEITSYVLEVRLIGKDNFERIVADNKLMDRKFTHGGLKEGSSYEFRVSAVNQIGQGKPSFSTKPVTCKKEFEPPALDFGFRDKIVVRVGETCTLQSRYTGKPQPTIKWFKTDEELQANEEIALTSTNNILCLAIEKAKREHSGKYTVVLENSIGSRKGICNIIVVDRPQHPEGPVIFDEICRNYMVISWKPPLDDGGAAISNYIIEKRDTNRDLWMPVIESCTRTSCRVPKLIEGREYIVRICAQNIHGISDPLLSAETKARDVFRVPDAPQAPVAKEIYKDTALISWIQPADGGKPITNYIVEKKETMANTWTRAGKDRIFPNSEYWVPDILRGCEYEFRVMAENMIGVGDPSPPSKPVFAKDPIVIPSPPVLPVAIDTTKESVTLSWQPPKDSGRGKIFGYLIEYQKDDSDEWLQVNQTPDSCQETRFKVISLEDGALYRFRVKAANAAGESEPTYVPEPIRAEDRLEPPELLLDMGMPREVKAMAGTHINIIAGIKGIPFPNIIWKKNDADVPPKAEIETSGTASKLEIRYCTRADCGDYTIYVENPAGSKTATCTVLVFDKPGPVQNFRVSDVRCDSAQLSWKDPEDNGGTRITNFVVEKKDGAATQWVPVCSSSKKRSMMAKYLIEGMSYMFRVAAENQFGRSEYVETPKSIKAMNPLFPPGPPKDLHHVDVDKTEVWLQWNWPDRTGGSDITG";
                //readSequenceFromFile();
//        System.out.println("processing seq: " + subunit1.sequence);
        
        StructuralModification modification = new StructuralModification();
        modification.structuralModificationType = CV_AMINO_ACID_SUBSTITUTION;
        List<Site> sites = new ArrayList<>();
        sites.add(new Site(1, 4));
        modification.setSites(sites);
        modification.extent="COMPLETE";
        modification.molecularFragment = new SubstanceReference();
        Substance polymer = buildPolymerWithHighAndLow();

        modification.molecularFragment.refuuid=polymer.getUuid().toString();
        Modifications mods = new Modifications();
        mods.structuralModifications.add(modification);
        proteinSubstance.setModifications(mods);
        proteinSubstance.setProtein(protein);

        Set<String> unknownResidues = new HashSet<>();

        MolecularWeightAndFormulaContribution contribution=ProteinUtils.generateProteinWeightAndFormula(substanceRepository,
                proteinSubstance, unknownResidues);

        double isoleucineMw = 131.175; //wikipedia
        double expectedAverageMw = 5047292.71 - isoleucineMw + CELLULOSE_SULFATE_MW;
        double expectedLowMw = MW_LOW_OFFSET;
        double expectedHighMw = MW_HIGH_OFFSET;
        double actualAverage =contribution.getMw();
        double actualLow = contribution.getMwLow();
        double actualHigh = contribution.getMwHigh();
        double allowedProteinMWTolerance =12;
        assertEquals(expectedAverageMw, actualAverage, allowedProteinMWTolerance);
        assertEquals(expectedLowMw, actualLow, 0.9);
        assertEquals(expectedHighMw, actualHigh, 0.9);
    }


    @Test
    public void proteinMwTestModWithHighAndLowAndLims() {
        ProteinSubstance proteinSubstance = new ProteinSubstance();
        Protein protein = new Protein();
        Subunit subunit1= new  Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        subunit1.sequence =readSequenceFromFile();
        
        StructuralModification modification = new StructuralModification();
        modification.structuralModificationType = CV_AMINO_ACID_SUBSTITUTION;
        List<Site> sites = new ArrayList<>();
        sites.add(new Site(1, 4));
        modification.setSites(sites);
        modification.extent="COMPLETE";
        modification.molecularFragment = new SubstanceReference();
        Substance polymer = buildPolymerWithHighAndLowAndLimits();

        modification.molecularFragment.refuuid=polymer.getUuid().toString();
        Modifications mods = new Modifications();
        mods.structuralModifications.add(modification);
        proteinSubstance.setModifications(mods);
        proteinSubstance.setProtein(protein);

        Set<String> unknownResidues = new HashSet<>();

        MolecularWeightAndFormulaContribution contribution=ProteinUtils.generateProteinWeightAndFormula(substanceRepository,
                proteinSubstance, unknownResidues);
        contribution.getMessages().forEach(m-> System.out.printf("message: %s; \n", m.message));
        double isoleucineMw = 131.175; //wikipedia
        double expectedAverageMw = 5047292.71 - isoleucineMw + CELLULOSE_SULFATE_MW;
        double actualAverage =contribution.getMw();
        double actualLow = contribution.getMwLow();
        double actualHigh = contribution.getMwHigh();
        double actualHighLimit =contribution.getMwHighLimit();
        double actualLowLimit =contribution.getMwLowLimit();

        assertEquals(expectedAverageMw, actualAverage, LARGE_PROTEIN_MW_TOLERANCE);
        assertEquals(Math.abs(MW_LOW_OFFSET), actualLow, 0.9);
        assertEquals(Math.abs(MW_HIGH_OFFSET), actualHigh, 0.9);
        assertEquals(Math.abs(MW_LOW_OFFSET), actualHigh, 0.9);
        assertEquals(Math.abs(MW_HIGHLIMIT_OFFSET), actualHighLimit);
        assertEquals(Math.abs(MW_LOWLIMIT_OFFSET), actualLowLimit);
    }

    @Test
    public void proteinMwTestModExtent() {
        ProteinSubstance proteinSubstance = new ProteinSubstance();
        Protein protein = new Protein();
        Subunit subunit1= new  Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        //from https://www.uniprot.org/uniprot/Q9FGE9
        
        subunit1.sequence =
"MELGSRRIYTTMPSKLRSSSSLLPRILLLSLLLLLFYSLILRRPITSNIASPPPCDLFSGRWVFNPETPKPLYDETCPFHRNAWNCLRNKRDNMDVINSWRWEPNGCGLSRIDPTRFLGMMRNKNVGFVGDSLNENFLVSFLCILRVADPSAIKWKKKKAWRGAYFPKFNVTVAYHRAVLLAKYQWQARSSAEANQDGVKGTYRVDVDVPANEWINVTSFYDVLIFNSGHWWGYDKFPKETPLVFYRKGKPINPPLDILPGFELVLQNMVSYIQREVPAKTLKFWRLQSPRHFYGGDWNQNGSCLLDKPLEENQLDLWFDPRNNGVNKEARKINQIIKNELQTTKIKLLDLTHLSEFRADAHPAIWLGKQDAVAIWGQDCMHWCLPGVPDTWVDILAELILTNLKTE" ;
        StructuralModification modification = new StructuralModification();
        modification.structuralModificationType = CV_AMINO_ACID_SUBSTITUTION;
        List<Site> sites = new ArrayList<>();
        sites.add(new Site(1, 4));
        modification.setSites(sites);
        modification.extent="PARTIAL";
        modification.molecularFragment = new SubstanceReference();
        /*
        use nucleic acid validation test
         */
        Substance polymer = buildPolymer();

        modification.molecularFragment.refuuid=polymer.getUuid().toString();
        Modifications mods = new Modifications();
        mods.structuralModifications.add(modification);
        proteinSubstance.setModifications(mods);
        proteinSubstance.setProtein(protein);

        Set<String> unknownResidues = new HashSet<>();

        MolecularWeightAndFormulaContribution contribution=ProteinUtils.generateProteinWeightAndFormula(substanceRepository,
                proteinSubstance, unknownResidues);

        double expectedMw = 47142.0;
        double actual =contribution.getMw();
        assertEquals(expectedMw, actual, 0.9);
        
        Assertions.assertTrue(contribution.getMessages().size() >0);
        Assertions.assertTrue(contribution.getMessages().stream().anyMatch(m->m.messageType==ValidationMessage.MESSAGE_TYPE.WARNING));
    }

    @Test
    public void getSingleAAFormulaTest() {
        String serineAbbreviation = "S";
        Map<String, SingleThreadCounter> expected = new HashMap<>();
        expected.put("C", new SingleThreadCounter(3));
        expected.put("H", new SingleThreadCounter(7-2)); //remove 2 as in 'H2O'
        expected.put("N", new SingleThreadCounter(1));
        expected.put("O", new SingleThreadCounter(3-1)); //remove 1 for O in 'H2O'
        Map<String, SingleThreadCounter> actual = ProteinUtils.getSingleAAFormula(serineAbbreviation);
        assertFalse(actual.isEmpty());
        assertTrue( actual.keySet().stream().allMatch(k-> expected.get(k).getAsInt() == actual.get(k).getAsInt()));
    }
    
    @Test
    public void getSingleAAWeightTest() {
        String histideAbbreviation = "H";
        double histidineMw = 155.156 - WATER_MW;
        double actual = ProteinUtils.getSingleAAWeight(histideAbbreviation);
        assertEquals(histidineMw, actual, 0.01);
    }
    
    @Test
    public void getSubunitWeightTest() {
        Subunit unit1 = new Subunit();
        unit1.sequence="MKKLVIALCLMMVLAVMVEEAEAKWCFRVCYRGICYRRCRGKRNEVRQYRDRGYDVRAIPEETFFTRQDEDEDDDEE";
        Set<String> unknowns = new HashSet<>();
        double expected = 9349.0;
        double actual = ProteinUtils.getSubunitWeight(unit1, unknowns);
        assertEquals(expected, actual, 0.5);
        assertEquals(0, unknowns.size());
    }
    
    @Test
    public void getSubunitWeightTest2() {
        Subunit unit1 = new Subunit();
        unit1.sequence="MKKLVIALCLMMVLAVMVEEAEAKWCFRVCYRGICYRRCRGKRNEVRQYRDRGYDVRAIPEETFFTRQDEDEDDDEE@";
        Set<String> unknowns = new HashSet<>();
        double expected = 9349.0;
        double actual = ProteinUtils.getSubunitWeight(unit1, unknowns);
        assertEquals(expected, actual, 0.4);
        assertEquals("@", unknowns.toArray()[0]);
    }

    @Test
    public void getSubunitFormulaInfoTest(){
        Subunit unit1 = new Subunit();
        unit1.sequence ="GKLSVDTLRF";
        Map<String, SingleThreadCounter> expectedCounts = new HashMap<>();
        expectedCounts.put("C", new SingleThreadCounter(51));
        expectedCounts.put("H", new SingleThreadCounter(86));
        expectedCounts.put("O", new SingleThreadCounter(15));
        expectedCounts.put("N", new SingleThreadCounter(14));
        Set<String> unknowns = new HashSet<>();
        Map<String, SingleThreadCounter> actual = ProteinUtils.getSubunitFormulaInfo(unit1.sequence, unknowns);
        Assertions.assertTrue( expectedCounts.keySet().stream().allMatch(k-> expectedCounts.get(k).getAsInt() == actual.get(k).getAsInt()) );
        assertEquals(0, unknowns.size());
    }

    @Test
    public void makeFormulaFromMapTest() {
        Map<String, SingleThreadCounter> counts = new HashMap<>();
        counts.put("H", new SingleThreadCounter(86));
        counts.put("O", new SingleThreadCounter(15));
        counts.put("N", new SingleThreadCounter(14));
        counts.put("C", new SingleThreadCounter(51));
        
        String expected = "C51H86N14O15";
        String actual = ProteinUtils.makeFormulaFromMap(counts);

        assertEquals(expected, actual);
    }
    
    @Test
    public void getSubunitFormulaInfoTinyTest(){

        Subunit unit1 = new Subunit();
        unit1.sequence ="GK";
        Map<String, SingleThreadCounter> expectedCounts = new HashMap<>();
        expectedCounts.put("C", new SingleThreadCounter(8));
        expectedCounts.put("H", new SingleThreadCounter(17));
        expectedCounts.put("O", new SingleThreadCounter(3));
        expectedCounts.put("N", new SingleThreadCounter(3));
        Set<String> unknowns = new HashSet<>();
        Map<String, SingleThreadCounter> actual = ProteinUtils.getSubunitFormulaInfo(unit1.sequence, unknowns);
        Assertions.assertTrue( expectedCounts.keySet().stream().allMatch(k-> expectedCounts.get(k).getAsInt() == actual.get(k).getAsInt()) );
        assertEquals(0, unknowns.size());
    }

    @Test
    public void getMolWeightPropertiesTest() {
        ProteinSubstance protein88ECG9H7RA = getProteinFromFile();
        double expectedMwValue = 23900.0;
        List<Property> mwProps = ProteinUtils.getMolWeightProperties(protein88ECG9H7RA);
        assertEquals(expectedMwValue, mwProps.get(0).getValue().average, 0.1);
    }
    
    @Test
    public void getMolFormulaPropertiesTest(){
        ProteinSubstance protein88ECG9H7RA = getProteinFromFile();
        String expectedFormula = "C1030H1734O304N336S5";
        List<Property> formulaProps = ProteinUtils.getMolFormulaProperties(protein88ECG9H7RA);
        assertEquals(expectedFormula, formulaProps.get(0).getValue().nonNumericValue);
    }

    @Test
    public void proteinMolFormulaTest() {
        ProteinSubstance proteinSubstance = new ProteinSubstance();
        Protein protein = new Protein();
        Subunit subunit1= new  Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        subunit1.sequence =
                "MKKN";
        proteinSubstance.setProtein(protein);

        Set<String> unknownResidues = new HashSet<>();

        MolecularWeightAndFormulaContribution contribution=ProteinUtils.generateProteinWeightAndFormula(substanceRepository,
                proteinSubstance, unknownResidues);

        ProteinSubstance proteinSubstanceMixedCase = new ProteinSubstance();
        Protein proteinMixedCase = new Protein();
        Subunit subunit1MixedCase= new  Subunit();
        proteinMixedCase.subunits = new ArrayList<>();
        proteinMixedCase.subunits.add(subunit1MixedCase);
        subunit1MixedCase.sequence =
                "MkKn";
        proteinSubstanceMixedCase.setProtein(proteinMixedCase);

        Set<String> unknownResiduesMixed = new HashSet<>();

        MolecularWeightAndFormulaContribution contributionMixedCase=
                ProteinUtils.generateProteinWeightAndFormula(substanceRepository,
                        proteinSubstanceMixedCase, unknownResiduesMixed);

        assertEquals(contribution.getFormula(), contributionMixedCase.getFormula(),
                "Formula must be the same when protein sequence includes lowercase codes");
    }

    @Test
    public void aparaginePeptideMolFormulaTest() {
        ProteinSubstance proteinSubstance = new ProteinSubstance();
        Protein protein = new Protein();
        Subunit subunit1= new  Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        subunit1.sequence =
                "N";
        proteinSubstance.setProtein(protein);

        Set<String> unknownResidues = new HashSet<>();

        MolecularWeightAndFormulaContribution contribution=ProteinUtils.generateProteinWeightAndFormula(substanceRepository,
                proteinSubstance, unknownResidues);
        String expectedFormula ="C4H8N2O3";
        Assertions.assertEquals(8, contribution.getFormulaMap().get("H").getAsInt());
        Assertions.assertEquals(expectedFormula, contribution.getFormula());
    }

    @Test
    public void proteinMwTestMod4() {
        ProteinSubstance proteinSubstance = new ProteinSubstance();
        Protein protein = new Protein();
        Subunit subunit1= new  Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        subunit1.sequence =
                "MDPQEMVVKNPYAHISIPRAHLRPDLGQQLEVASTCSSSSEMQPLPVGPCAPEPTHLLQPTEVPGPKGAKGNQGAAPIQNQQAWQQPGNPYSSSQQAGLTYAGPPPAGRGDDIAHHCCCCPCCHCCHCPPFCRCHSCCCCVIS";
        StructuralModification modification = new StructuralModification();
        modification.structuralModificationType = CV_AMINO_ACID_SUBSTITUTION;

        List<Site> sites = new ArrayList<>();
        Site newSite= new Site();
        newSite.residueIndex=1;
        newSite.subunitIndex=1;
        sites.add(newSite);
        modification.setSites(sites);
        modification.extent="COMPLETE";
        modification.molecularFragment = new SubstanceReference();
        ChemicalSubstance substitute = buildSubstituteChemical();

        modification.molecularFragment.refuuid=substitute.getUuid().toString();
        modification.residueModified="1_1";
        Modifications mods = new Modifications();
        mods.structuralModifications.add(modification);
        proteinSubstance.setModifications(mods);
        proteinSubstance.setProtein(protein);

        Set<String> unknownResidues = new HashSet<>();
        String formulaSource ="C639 H992 N192 O197 S20";
        Map<String,SingleThreadCounter> baseProteinFormula = parseMapFromFormula(formulaSource);
        ChemicalSubstance methionine= buildMethionine();
        Map<String,SingleThreadCounter> methionineFormula = parseMapFromFormula(methionine.getStructure().formula);
        ProteinUtils.removeWater(methionineFormula);

        Map<String,SingleThreadCounter> substituteFormula = parseMapFromFormula(substitute.getStructure().formula);
        ProteinUtils.removeWater(substituteFormula);

        Map<String,SingleThreadCounter> expectedFormula = ProteinUtils.addFormulas( ProteinUtils.subtractFormulas(baseProteinFormula, methionineFormula), substituteFormula);

        MolecularWeightAndFormulaContribution contribution=ProteinUtils.generateProteinWeightAndFormula(substanceRepository,
                proteinSubstance, unknownResidues);
        contribution.getMessages().forEach(m-> log.trace("message: {} ", m.message));

        Assertions.assertTrue(formulasEqual(expectedFormula, contribution.getFormulaMap()));
    }

    @Test
    public void proteinMwTestMod5() {
        ProteinSubstance proteinSubstance = new ProteinSubstance();
        Protein protein = new Protein();
        Subunit subunit1= new  Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        subunit1.sequence =
                "MDPQEMVVKNPYAHISIPRAHLRPDLGQQLEVASTCSSSSEMQPLPVGPCAPEPTHLLQPTEVPGPKGAKGNQGAAPIQNQQAWQQPGNPYSSSQQAGLTYAGPPPAGRGDDIAHHCCCCPCCHCCHCPPFCRCHSCCCCVIS";
        StructuralModification modification = new StructuralModification();
        modification.structuralModificationType = CV_AMINO_ACID_SUBSTITUTION;

        List<Site> sites = new ArrayList<>();
        Site newSite= new Site();
        newSite.residueIndex=1;
        newSite.subunitIndex=1;
        sites.add(newSite);
        Site newSite2= new Site();
        newSite2.residueIndex=6;
        newSite2.subunitIndex=1;
        sites.add(newSite2);
        modification.setSites(sites);
        modification.extent="COMPLETE";
        modification.molecularFragment = new SubstanceReference();
        ChemicalSubstance substitute = buildSubstituteChemical();

        modification.molecularFragment.refuuid=substitute.getUuid().toString();
        modification.residueModified="1_1";
        Modifications mods = new Modifications();
        mods.structuralModifications.add(modification);
        proteinSubstance.setModifications(mods);
        proteinSubstance.setProtein(protein);

        Set<String> unknownResidues = new HashSet<>();
        String formulaSource ="C639 H992 N192 O197 S20";
        Map<String,SingleThreadCounter> baseProteinFormula = parseMapFromFormula(formulaSource);
        ChemicalSubstance methionine= buildMethionine();
        Map<String,SingleThreadCounter> methionineFormula = parseMapFromFormula(methionine.getStructure().formula);
        ProteinUtils.removeWater(methionineFormula);

        Map<String,SingleThreadCounter> substituteFormula = parseMapFromFormula(substitute.getStructure().formula);
        ProteinUtils.removeWater(substituteFormula);

        Map<String,SingleThreadCounter> removedLeavingGroup = ProteinUtils.subtractFormulas(baseProteinFormula, methionineFormula);
        removedLeavingGroup = ProteinUtils.subtractFormulas(removedLeavingGroup, methionineFormula);
        Map<String,SingleThreadCounter> expectedFormula = ProteinUtils.addFormulas( removedLeavingGroup, substituteFormula);
        expectedFormula = ProteinUtils.addFormulas( expectedFormula, substituteFormula);

        MolecularWeightAndFormulaContribution contribution=ProteinUtils.generateProteinWeightAndFormula(substanceRepository,
                proteinSubstance, unknownResidues);
        contribution.getMessages().forEach(m->{
            log.trace("message: {} ", m.message);
        });

        Assertions.assertTrue(formulasEqual(expectedFormula, contribution.getFormulaMap()));
    }


    @Test
    public void proteinMwTestMod6() {
        ProteinSubstance proteinSubstance = new ProteinSubstance();
        Protein protein = new Protein();
        Subunit subunit1= new  Subunit();
        protein.subunits = new ArrayList<>();
        protein.subunits.add(subunit1);
        subunit1.sequence = "MLSRNDDICIYGGLGLGGLLLLAVVLLSACLCWLHRRVKRLERSWAQGSSEQELHYASLQRLPVPSSEGPDLRGRDKRGTKEDPRADYACIAENKPT";
        StructuralModification modification = new StructuralModification();
        modification.structuralModificationType = CV_AMINO_ACID_SUBSTITUTION;

        List<Site> sites = new ArrayList<>();
        Site newSite= new Site();
        newSite.residueIndex=2;
        newSite.subunitIndex=1;
        sites.add(newSite);
        Site newSite2= new Site();
        newSite2.residueIndex=3;
        newSite2.subunitIndex=1;
        sites.add(newSite2);
        modification.setSites(sites);
        modification.extent="COMPLETE";
        modification.molecularFragment = new SubstanceReference();
        ChemicalSubstance substitute = buildSubstituteChemical2();

        modification.molecularFragment.refuuid=substitute.getUuid().toString();
        modification.residueModified="1_2;1_3";
        Modifications mods = new Modifications();
        mods.structuralModifications.add(modification);
        proteinSubstance.setModifications(mods);
        proteinSubstance.setProtein(protein);

        Set<String> unknownResidues = new HashSet<>();
        String formulaSource ="C469 H764 N142 O140 S5";
        Map<String,SingleThreadCounter> baseProteinFormula = parseMapFromFormula(formulaSource);
        ChemicalSubstance leucine= buildLeucine();
        Map<String,SingleThreadCounter> leucineFormula = parseMapFromFormula(leucine.getStructure().formula);
        ProteinUtils.removeWater(leucineFormula);

        ChemicalSubstance serine = buildSerine();
        Map<String,SingleThreadCounter> serineFormula = parseMapFromFormula(serine.getStructure().formula);
        ProteinUtils.removeWater(serineFormula);

        Map<String,SingleThreadCounter> substituteFormula = parseMapFromFormula(substitute.getStructure().formula);
        ProteinUtils.removeWater(substituteFormula);

        Map<String,SingleThreadCounter> expectedFormula = ProteinUtils.subtractFormulas(baseProteinFormula, leucineFormula);
        expectedFormula = ProteinUtils.addFormulas( expectedFormula, substituteFormula);
        expectedFormula = ProteinUtils.subtractFormulas(expectedFormula, serineFormula);
        expectedFormula = ProteinUtils.addFormulas( expectedFormula, substituteFormula);

        MolecularWeightAndFormulaContribution contribution=ProteinUtils.generateProteinWeightAndFormula(substanceRepository,
                proteinSubstance, unknownResidues);
        contribution.getMessages().forEach(m->{
            log.trace("message: {} ", m.message);
        });

        Assertions.assertTrue(formulasEqual(expectedFormula, contribution.getFormulaMap()));
    }

    private ChemicalSubstance buildTryptophan() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();

        String tryMolfile="\\n  Marvin  01132108112D          \\n\\n 15 16  0  0  1  0            999 V2000\\n    4.3175   -6.1954    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    4.2700   -4.8406    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\\n    4.7574   -5.4821    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    3.4937   -5.9933    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    6.3495   -6.5754    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    4.9951   -6.5833    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    3.4937   -5.1136    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    5.6723   -6.1914    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\\n    6.3455   -7.3599    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n    5.6683   -5.4068    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.0271   -6.1835    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n    2.7849   -6.3970    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    2.7095   -4.7098    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    2.0241   -5.9933    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    1.9884   -5.1690    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n  2  3  1  0  0  0  0\\n  3  1  2  0  0  0  0\\n  4  1  1  0  0  0  0\\n  5  8  1  0  0  0  0\\n  6  1  1  0  0  0  0\\n  7  4  2  0  0  0  0\\n  8  6  1  0  0  0  0\\n  9  5  2  0  0  0  0\\n  8 10  1  6  0  0  0\\n 11  5  1  0  0  0  0\\n 12  4  1  0  0  0  0\\n 13  7  1  0  0  0  0\\n 14 12  2  0  0  0  0\\n 15 14  1  0  0  0  0\\n  7  2  1  0  0  0  0\\n 15 13  2  0  0  0  0\\nM  END";

        GinasChemicalStructure structure= new GinasChemicalStructure();
        structure.molfile =tryMolfile;
        structure.setMwt(204.2252);
        structure.formula ="C11H12N2O2";
        ChemicalSubstance tryptophan = builder.setStructure(structure)
                .addName("tryptophan")
                .addCode("FDA UNII", "8DUH1N11BX")
                .build();
        substanceRepository.saveAndFlush(tryptophan);

        return tryptophan;
    }

    private ChemicalSubstance buildMethionine() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();

        String methioMolfile="\\n  Marvin  01132112312D          \\n\\n  9  8  0  0  1  0            999 V2000\\n    8.9709  -10.7849    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    8.9709   -9.9648    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n    8.2622  -11.2022    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\\n    8.2622  -12.0175    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.5499  -10.7849    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    6.8383  -11.2022    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    6.1290  -10.7849    0.0000 S   0  0  0  0  0  0  0  0  0  0  0  0\\n    5.4123  -11.2022    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    9.6832  -11.2022    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n  1  2  2  0  0  0  0\\n  1  3  1  0  0  0  0\\n  3  4  1  1  0  0  0\\n  3  5  1  0  0  0  0\\n  5  6  1  0  0  0  0\\n  7  6  1  0  0  0  0\\n  7  8  1  0  0  0  0\\n  1  9  1  0  0  0  0\\nM  END";

        GinasChemicalStructure structure= new GinasChemicalStructure();
        structure.molfile =methioMolfile;
        structure.setMwt(149.21);
        structure.formula ="C5H11NO2S";
        ChemicalSubstance methionine = builder.setStructure(structure)
                .addName("methionine")
                .addCode("FDA UNII", "8DUH1N11BX")
                .build();
        substanceRepository.saveAndFlush(methionine);

        return methionine;
    }


    private ChemicalSubstance buildThreonine() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();

        String threoMolfile="\\n  Marvin  01132107332D          \\n\\n  8  7  0  0  1  0            999 V2000\\n   -0.0958   -0.0667    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\\n    0.6250   -0.4792    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    0.6250   -1.3042    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n   -0.0958    0.7583    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\\n   -0.8000   -0.4792    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\\n    1.3417   -0.0667    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n    0.6250    1.1708    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n   -0.8000    1.1708    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n  2  1  1  0  0  0  0\\n  3  2  2  0  0  0  0\\n  4  1  1  0  0  0  0\\n  1  5  1  1  0  0  0\\n  6  2  1  0  0  0  0\\n  4  7  1  1  0  0  0\\n  8  4  1  0  0  0  0\\nM  END";

        GinasChemicalStructure structure= new GinasChemicalStructure();
        structure.molfile =threoMolfile;
        structure.setMwt(119.12);
        structure.formula ="C4H9NO3";
        ChemicalSubstance threonine = builder.setStructure(structure)
                .addName("threonine")
                .addCode("FDA UNII", "2ZD004190S")
                .build();
        substanceRepository.saveAndFlush(threonine);

        return threonine;
    }


    private ChemicalSubstance buildSerine() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();

        String serMolfile="\\n  Marvin  01132107052D          \\n\\n  7  6  0  0  1  0            999 V2000\\n    2.7387    0.3545    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    2.0245   -0.0720    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\\n    2.7207    1.1663    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n    2.0168   -0.8863    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\\n    3.4323   -0.0642    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n    1.3077    0.3365    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    0.5883   -0.0617    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n  2  1  1  0  0  0  0\\n  3  1  2  0  0  0  0\\n  2  4  1  1  0  0  0\\n  5  1  1  0  0  0  0\\n  6  2  1  0  0  0  0\\n  7  6  1  0  0  0  0\\nM  END";

        GinasChemicalStructure structure= new GinasChemicalStructure();
        structure.molfile =serMolfile;
        structure.setMwt(105.09);
        structure.formula ="C3H7NO3";
        ChemicalSubstance serine = builder.setStructure(structure)
                .addName("serine")
                .addCode("FDA UNII", "452VLY9402")
                .build();
        substanceRepository.saveAndFlush(serine);

        return serine;
    }

    private ChemicalSubstance buildLeucine() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();

        String leuMolfile="\\n  Marvin  01132100552D          \\n\\n  9  8  0  0  1  0            999 V2000\\n   12.6108   -0.4395    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   11.8937   -0.0291    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\\n   12.6108   -1.2603    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n   11.8937    0.7918    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   11.1930   -0.4395    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\\n   13.3197   -0.0249    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n   12.6108    1.2022    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   13.3197    0.7918    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   12.6108    2.0272    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n  2  1  1  0  0  0  0\\n  3  1  2  0  0  0  0\\n  4  2  1  0  0  0  0\\n  2  5  1  1  0  0  0\\n  6  1  1  0  0  0  0\\n  7  4  1  0  0  0  0\\n  8  7  1  0  0  0  0\\n  9  7  1  0  0  0  0\\nM  END";

        GinasChemicalStructure structure= new GinasChemicalStructure();
        structure.molfile =leuMolfile;
        structure.setMwt(131.17);
        structure.formula ="C6H13NO2";
        ChemicalSubstance leucine = builder.setStructure(structure)
                .addName("leucine")
                .addCode("FDA UNII", "GMW67QNF9C")
                .build();
        substanceRepository.saveAndFlush(leucine);

        return leucine;
    }

    private ChemicalSubstance buildSubstituteChemical() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        String molfile ="\\n  Marvin  04212313312D          \\n\\n 11 11  0  0  0  0            999 V2000\\n    4.9509    1.1880    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n    4.2365    0.7755    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    4.2365   -0.0495    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    3.5220   -0.4620    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    2.8075   -0.0495    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    2.0930   -0.4620    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n    3.5220   -1.2870    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    4.2365   -1.6995    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\\n    4.9509   -1.2870    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    4.9509   -0.4620    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    5.6655   -0.0495    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n  1  2  1  0  0  0  0\\n  2  3  1  0  0  0  0\\n  3  4  2  0  0  0  0\\n  4  5  1  0  0  0  0\\n  5  6  1  0  0  0  0\\n  4  7  1  0  0  0  0\\n  7  8  2  0  0  0  0\\n  8  9  1  0  0  0  0\\n  9 10  2  0  0  0  0\\n  3 10  1  0  0  0  0\\n 10 11  1  0  0  0  0\\nM  END";
        String mofileFormula ="C7H9NO3";
        GinasChemicalStructure structure= new GinasChemicalStructure();
        structure.molfile =molfile;
        structure.formula =mofileFormula;
        structure.mwt= 7*12.011+9*1.008+14.007+3*15.999;
        ChemicalSubstance chemicalSubstance = builder
                .setStructure(structure)
                .addName("4,5-bis(hydroxymethyl)pyridin-3-ol")
                .addCode("PubChem CID", "21678630")
                .build();
        substanceRepository.saveAndFlush(chemicalSubstance);
        return chemicalSubstance;
    }

    private ChemicalSubstance buildSubstituteChemical2() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        String molfile ="\\n  Marvin  01132112462D          \\n\\n 30 33  0  0  0  0            999 V2000\\n    8.7068   -4.1345    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.3079   -4.2821    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    8.0151   -4.7070    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.4789   -3.4817    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    8.3389   -3.4817    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    6.5851   -4.6940    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    5.8753   -4.2744    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n    0.1554   -3.4428    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    0.1554   -4.2666    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    5.1552   -4.6863    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    4.4402   -4.2744    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    8.0151   -5.5256    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    3.0180   -4.2744    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\\n    0.8782   -3.0309    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n    0.8782   -4.6863    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n    4.4402   -3.4428    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n    6.9970   -2.8340    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    8.7378   -2.7511    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.3079   -5.9375    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    6.5851   -5.5178    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    3.7407   -4.6863    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   -0.5440   -3.0309    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   -0.5569   -4.6707    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    2.3108   -4.6863    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    1.5880   -4.2666    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    0.8886   -2.1994    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.3985   -2.1035    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    8.2482   -2.1035    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   -1.2745   -4.2433    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   -1.2745   -3.4428    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n  2  3  2  0  0  0  0\\n  3  1  1  0  0  0  0\\n  4  5  2  0  0  0  0\\n  5  1  1  0  0  0  0\\n  6  2  1  0  0  0  0\\n  7  6  1  0  0  0  0\\n  8  9  2  0  0  0  0\\n  9 15  1  0  0  0  0\\n 10  7  1  0  0  0  0\\n 11 10  1  0  0  0  0\\n 12  3  1  0  0  0  0\\n 13 21  1  0  0  0  0\\n 14  8  1  0  0  0  0\\n 15 25  1  0  0  0  0\\n 11 16  1  0  0  0  0\\n 17  4  1  0  0  0  0\\n 18  5  1  0  0  0  0\\n 19 12  2  0  0  0  0\\n 20 19  1  0  0  0  0\\n 21 11  1  0  0  0  0\\n 22  8  1  0  0  0  0\\n 23  9  1  0  0  0  0\\n 24 13  1  0  0  0  0\\n 25 24  1  0  0  0  0\\n 26 14  1  0  0  0  0\\n 27 28  1  0  0  0  0\\n 28 18  2  0  0  0  0\\n 29 23  2  0  0  0  0\\n 30 29  1  0  0  0  0\\n  4  2  1  0  0  0  0\\n 27 17  2  0  0  0  0\\n 20  6  2  0  0  0  0\\n 30 22  2  0  0  0  0\\nM  END";
        String mofileFormula ="C24H26N2O4";
        GinasChemicalStructure structure= new GinasChemicalStructure();
        structure.molfile =molfile;
        structure.formula =mofileFormula;
        structure.mwt= 24*12.011+26*1.008+2*14.007+4*15.999;
        ChemicalSubstance chemicalSubstance = builder
                .setStructure(structure)
                .addName("CARVEDILOL")
                .addCode("CAS", "72956-09-3")
                .build();
        substanceRepository.saveAndFlush(chemicalSubstance);
        return chemicalSubstance;
    }
    private PolymerSubstance buildPolymer() {
        PolymerSubstanceBuilder builder = new PolymerSubstanceBuilder(new Substance());
        builder.addName("CELLULOSE SULFATE");
        Property mwProp = new Property();
        mwProp.setName("MOL_WEIGHT:NUMBER");
        Amount mwValue = new Amount();
        mwValue.average =CELLULOSE_SULFATE_MW;
        mwProp.setValue(mwValue);
        mwProp.setPropertyType("PhysicoChemical");
        builder.addProperty(mwProp);
        PolymerSubstance polymer = builder.build();
        substanceRepository.saveAndFlush(polymer);
        return polymer;
    }

    private ChemicalSubstance buildProline() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("proline");
        builder.addCode("FDA UNII", "9DLQ4CIU6V");
        GinasChemicalStructure structure= new GinasChemicalStructure();
        structure.molfile =readProlineMolfileFromFile();
        structure.setMwt(115.1305);
        structure.formula ="C5H9NO2";
        builder.setStructure(structure);
        ChemicalSubstance proline = builder.build();
        substanceRepository.saveAndFlush(proline);
        return proline;
    }

    private PolymerSubstance buildPolymerWithHighAndLow() {
        PolymerSubstanceBuilder builder = new PolymerSubstanceBuilder(new Substance());
        builder.addName("CELLULOSE SULFATE");
        Property mwProp = new Property();
        mwProp.setName("MOL_WEIGHT:NUMBER");
        Amount mwValue = new Amount();
        mwValue.average =CELLULOSE_SULFATE_MW;
        mwValue.high =CELLULOSE_SULFATE_MW + MW_HIGH_OFFSET;
        mwValue.low =CELLULOSE_SULFATE_MW - MW_LOW_OFFSET;
        mwProp.setValue(mwValue);
        mwProp.setPropertyType("PhysicoChemical");
        builder.addProperty(mwProp);
        PolymerSubstance polymer = builder.build();
        substanceRepository.saveAndFlush(polymer);
        return polymer;
    }

    private PolymerSubstance buildPolymerWithHighAndLowAndLimits() {
        PolymerSubstanceBuilder builder = new PolymerSubstanceBuilder(new Substance());
        builder.addName("CELLULOSE SULFATE");
        Property mwProp = new Property();
        mwProp.setName("MOL_WEIGHT:NUMBER");
        Amount mwValue = new Amount();
        mwValue.average =CELLULOSE_SULFATE_MW;
        mwValue.high =CELLULOSE_SULFATE_MW + MW_HIGH_OFFSET;
        mwValue.low =CELLULOSE_SULFATE_MW + MW_LOW_OFFSET;
        mwValue.highLimit=CELLULOSE_SULFATE_MW + MW_HIGHLIMIT_OFFSET;
        mwValue.lowLimit=CELLULOSE_SULFATE_MW + MW_LOWLIMIT_OFFSET;
        
        mwProp.setValue(mwValue);
        mwProp.setPropertyType("PhysicoChemical");
        builder.addProperty(mwProp);
        PolymerSubstance polymer = builder.build();
        substanceRepository.saveAndFlush(polymer);
        return polymer;
    }
    

    private ProteinSubstance getProteinFromFile() {
        try {
            File proteinFile =new ClassPathResource("testJSON/88ECG9H7RA.json").getFile();
            ProteinSubstanceBuilder builder =SubstanceBuilder.from(proteinFile);
            return builder.build();
        } catch (IOException ex) {
            Logger.getLogger(ProtCalculationTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private String readSequenceFromFile() {
        try {
            File fastaFile = new ClassPathResource("testFASTA/A0A5A9P0L4.fasta").getFile();
            
            List<String> lines =Files.readAllLines(fastaFile.toPath());
            lines.remove(0);
            return String.join("", lines);
        }
        catch(Exception ex) {
            Logger.getLogger(ProtCalculationTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }

    private String readProlineMolfileFromFile() {
        try {
            File fastaFile = new ClassPathResource("molfiles/9DLQ4CIU6V.mol").getFile();

            List<String> lines =Files.readAllLines(fastaFile.toPath());
            return String.join("\n", lines);
        }
        catch(Exception ex) {
            Logger.getLogger(ProtCalculationTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }

    @Test
    public void testParseH2O() {
        String inputFormula = "H2O";
        Map<String, SingleThreadCounter> expected = new HashMap<>();
        expected.put("H", new SingleThreadCounter(2));
        expected.put("O", new SingleThreadCounter(1));

        Map<String, SingleThreadCounter> actual =parseMapFromFormula(inputFormula);
        for(String symbol : expected.keySet()){
            Assertions.assertEquals(expected.get(symbol).getAsInt(), actual.get(symbol).getAsInt());
        }
    }

    @Test
    public void testParseSugar() {
        String inputFormula = "C12H22O11";
        Map<String, SingleThreadCounter> expected = new HashMap<>();
        expected.put("H", new SingleThreadCounter(22));
        expected.put("O", new SingleThreadCounter(11));
        expected.put("C", new SingleThreadCounter(12));

        Map<String, SingleThreadCounter> actual =parseMapFromFormula(inputFormula);
        for(String symbol : expected.keySet()){
            Assertions.assertEquals(expected.get(symbol).getAsInt(), actual.get(symbol).getAsInt());
        }
    }

    @Test
    public void testParseCalciumCarbonate() {
        String inputFormula = "CaCO3";
        Map<String, SingleThreadCounter> expected = new HashMap<>();
        expected.put("Ca", new SingleThreadCounter(1));
        expected.put("O", new SingleThreadCounter(3));
        expected.put("C", new SingleThreadCounter(1));

        Map<String, SingleThreadCounter> actual =parseMapFromFormula(inputFormula);
        for(String symbol : expected.keySet()){
            Assertions.assertEquals(expected.get(symbol).getAsInt(), actual.get(symbol).getAsInt());
        }
    }

    @Test
    public void testParseMolFmla() {
        String inputFormula = "C8H5Br2N";
        Map<String, SingleThreadCounter> expected = new HashMap<>();
        expected.put("H", new SingleThreadCounter(5));
        expected.put("Br", new SingleThreadCounter(2));
        expected.put("N", new SingleThreadCounter(1));
        expected.put("C", new SingleThreadCounter(8));

        Map<String, SingleThreadCounter> actual =parseMapFromFormula(inputFormula);
        for(String symbol : expected.keySet()){
            Assertions.assertEquals(expected.get(symbol).getAsInt(), actual.get(symbol).getAsInt());
        }
    }

    @Test
    public void testParseMolFmlaWithSpaces() {
        String inputFormula = "C8 H5 Br2 N";
        Map<String, SingleThreadCounter> expected = new HashMap<>();
        expected.put("H", new SingleThreadCounter(5));
        expected.put("Br", new SingleThreadCounter(2));
        expected.put("N", new SingleThreadCounter(1));
        expected.put("C", new SingleThreadCounter(8));

        Map<String, SingleThreadCounter> actual =parseMapFromFormula(inputFormula);
        for(String symbol : expected.keySet()){
            Assertions.assertEquals(expected.get(symbol).getAsInt(), actual.get(symbol).getAsInt());
        }
    }

    private Map<String, SingleThreadCounter> parseMapFromFormula(String formula){

        Map<String, SingleThreadCounter> elementData = new HashMap<>();
        int pos=0;
        StringBuilder symbolBuilder = new StringBuilder();
        StringBuilder multiplierBuilder = new StringBuilder();
        while(pos< formula.length()) {
            if(Character.isDigit(formula.charAt(pos))) {
                multiplierBuilder.append(formula.charAt(pos));
            } else if(symbolBuilder.length()==0 || Character.isLowerCase(formula.charAt(pos))){
                symbolBuilder.append(formula.charAt(pos));
            }else if(Character.isUpperCase(formula.charAt(pos))){
                if(symbolBuilder.length()>0 ) {
                    int multiplier =1;
                    if(multiplierBuilder.length()>0) {
                        multiplier=Integer.parseInt(multiplierBuilder.toString());
                    }
                    elementData.put(symbolBuilder.toString(), new SingleThreadCounter(multiplier));
                    symbolBuilder= new StringBuilder();
                    multiplierBuilder = new StringBuilder();
                }
                symbolBuilder.append(formula.charAt(pos));
            }
            pos++;
        }
        //handle last char
        if(symbolBuilder.length()>0 ) {
            int multiplier =1;
            if(multiplierBuilder.length()>0) {
                multiplier=Integer.parseInt(multiplierBuilder.toString());
            }
            elementData.put(symbolBuilder.toString(), new SingleThreadCounter(multiplier));
        }
        return elementData;
    }


    private boolean formulasEqual(Map<String,SingleThreadCounter> formula1, Map<String,SingleThreadCounter> formula2){
        if( formula1.size()!=formula2.size()){
            log.warn("formulas of different lengths");
            return false;
        }

        Boolean[] result = new Boolean[1];
        result[0]=true;
        formula1.keySet().forEach(s->{
            if( !formula2.containsKey(s) || formula1.get(s).getAsInt()!= formula2.get(s).getAsInt()){
                log.warn("formulas disagree for species {}", s);
                result[0] = false;
            }
        });
        return result[0];
    }

    @Test
    public void testSubtractFormulas() {
        Map<String,SingleThreadCounter> formula1 = new HashMap<>();
        formula1.put("C", new SingleThreadCounter(10));
        formula1.put("H", new SingleThreadCounter(2));
        formula1.put("N", new SingleThreadCounter(20));
        formula1.put("O", new SingleThreadCounter(4));

        Map<String,SingleThreadCounter> formula2 = new HashMap<>();
        formula2.put("C", new SingleThreadCounter(2));
        formula2.put("H", new SingleThreadCounter(1));
        formula2.put("N", new SingleThreadCounter(2));
        formula2.put("O", new SingleThreadCounter(2));

        Map<String,SingleThreadCounter> expected = new HashMap<>();
        expected.put("C", new SingleThreadCounter(8));
        expected.put("H", new SingleThreadCounter(1));
        expected.put("N", new SingleThreadCounter(18));
        expected.put("O", new SingleThreadCounter(2));

        Map<String, SingleThreadCounter> actual = ProteinUtils.subtractFormulas(formula1, formula2);
        expected.keySet().forEach(k-> Assertions.assertEquals(expected.get(k).getAsInt(), actual.get(k).getAsInt()));
    }

    @Test
    public void testSubtractFormulas2() {
        Map<String,SingleThreadCounter> formula1 = new HashMap<>();
        formula1.put("C", new SingleThreadCounter(2));
        formula1.put("H", new SingleThreadCounter(2));
        formula1.put("N", new SingleThreadCounter(20));
        formula1.put("S", new SingleThreadCounter(4));

        Map<String,SingleThreadCounter> formula2 = new HashMap<>();
        formula2.put("C", new SingleThreadCounter(5));
        formula2.put("H", new SingleThreadCounter(1));
        formula2.put("N", new SingleThreadCounter(2));
        formula2.put("O", new SingleThreadCounter(2));

        Map<String,SingleThreadCounter> expected = new HashMap<>();
        expected.put("C", new SingleThreadCounter(-3));
        expected.put("H", new SingleThreadCounter(1));
        expected.put("N", new SingleThreadCounter(18));
        expected.put("S", new SingleThreadCounter(4));
        expected.put("O", new SingleThreadCounter(-2));

        Map<String, SingleThreadCounter> actual = ProteinUtils.subtractFormulas(formula1, formula2);
        expected.keySet().forEach(k-> Assertions.assertEquals(expected.get(k).getAsInt(), actual.get(k).getAsInt()));
    }

    @Test
    public void testAddFormulas() {
        Map<String,SingleThreadCounter> formula1 = new HashMap<>();
        formula1.put("C", new SingleThreadCounter(2));
        formula1.put("H", new SingleThreadCounter(2));
        formula1.put("N", new SingleThreadCounter(20));
        formula1.put("S", new SingleThreadCounter(4));

        Map<String,SingleThreadCounter> formula2 = new HashMap<>();
        formula2.put("C", new SingleThreadCounter(5));
        formula2.put("H", new SingleThreadCounter(1));
        formula2.put("N", new SingleThreadCounter(2));
        formula2.put("O", new SingleThreadCounter(2));

        Map<String,SingleThreadCounter> expected = new HashMap<>();
        expected.put("C", new SingleThreadCounter(7));
        expected.put("H", new SingleThreadCounter(3));
        expected.put("N", new SingleThreadCounter(22));
        expected.put("S", new SingleThreadCounter(4));
        expected.put("O", new SingleThreadCounter(2));

        Map<String, SingleThreadCounter> actual = ProteinUtils.addFormulas(formula1, formula2);
        expected.keySet().forEach(k-> Assertions.assertEquals(expected.get(k).getAsInt(), actual.get(k).getAsInt()));
    }

    @Test
    public void testFormulasEqual() {
        Map<String,SingleThreadCounter> formula1 = new HashMap<>();
        formula1.put("C", new SingleThreadCounter(2));
        formula1.put("H", new SingleThreadCounter(2));
        formula1.put("N", new SingleThreadCounter(20));
        formula1.put("S", new SingleThreadCounter(4));

        Map<String,SingleThreadCounter> formula2 = new HashMap<>();
        formula2.put("C", new SingleThreadCounter(5));
        formula2.put("H", new SingleThreadCounter(1));
        formula2.put("N", new SingleThreadCounter(2));
        formula2.put("O", new SingleThreadCounter(2));

        Assertions.assertFalse(formulasEqual(formula1, formula2));
    }

    @Test
    public void testFormulasEqual2() {
        Map<String,SingleThreadCounter> formula1 = new HashMap<>();
        formula1.put("C", new SingleThreadCounter(23));
        formula1.put("H", new SingleThreadCounter(42));
        formula1.put("N", new SingleThreadCounter(20));
        formula1.put("S", new SingleThreadCounter(4));

        Map<String,SingleThreadCounter> formula2 = new HashMap<>();
        formula2.put("C", new SingleThreadCounter(23));
        formula2.put("H", new SingleThreadCounter(42));
        formula2.put("N", new SingleThreadCounter(20));
        formula2.put("S", new SingleThreadCounter(4));

        Assertions.assertTrue(formulasEqual(formula1, formula2));
    }

}
