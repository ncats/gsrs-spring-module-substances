package example.prot;

import example.substance.AbstractSubstanceJpaEntityTest;
import gov.nih.ncats.common.util.SingleThreadCounter;
import gov.nih.ncats.molwitch.Chemical;
import ix.core.validator.ValidationMessage;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.PolymerSubstanceBuilder;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.*;
import ix.ginas.utils.MolecularWeightAndFormulaContribution;
import ix.ginas.utils.ProteinUtils;
import java.io.File;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

public class ProtCalculationTest extends AbstractSubstanceJpaEntityTest {

    private static final String CV_AMINO_ACID_SUBSTITUTION = "AMINO_ACID_SUBSTITUTION";
    private static final Double CELLULOSE_SULFATE_MW =470000.0;
    private static final Double WATER_MW = 18.015;;

    @TempDir
    static File file;
    
    @Test
    public void mwIndoleTest() {
        System.out.println("starting mwIndoleTest");
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

    @Test
    public void proteinMwTestUnmod() {
        /*
        Create an unmodified protein -- just an amino acid sequence -- and calculate its molecular weight
         */
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
    public void proteinMwTestDisulfides() {
        /*
        Create an unmodified protein -- just an amino acid sequence -- and calculate its molecular weight
         */
        System.out.println("starting proteinMwTestUnmod");
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
        System.out.println("calculated MW: " + actual);

        assertEquals(expectedMw, actual, 0.9);
    }


    @Test
    public void proteinMwTestMod1() {
        /*
        Create an unmodified protein -- just an amino acid sequence -- and calculate its molecular weight
         */
        System.out.println("starting proteinMwTestMod1");
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

    @Test
    public void proteinMwTestMod2() {
        System.out.println("starting proteinMwTestMod2");
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
        contribution.getMessages().forEach(m->{
            System.out.printf("message: %s; ", m.message);
        });
        double valineMw= 117.1463;
        double lycineMw = 146.189;
        double trypophanMw= 204.2;
        double methionineMw =149.2;
        double expectedMw = 113269.0 - methionineMw -lycineMw + 2*trypophanMw;
        double actual =contribution.getMw();
        System.out.println("calculated MW: " + actual);

        assertEquals(expectedMw, actual, 0.9);
    }

    @Test
    public void proteinMwTestMod3() {
        System.out.println("starting proteinMwTestMod3");
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
        contribution.getMessages().forEach(m->{
            System.out.printf("message: %s; ", m.message);
        });
        double asparagineMw = 132.119; //wikipedia
        double expectedMw = 113269.0 - asparagineMw + CELLULOSE_SULFATE_MW;
        double actual =contribution.getMw();
        System.out.println("calculated MW: " + actual);
        assertEquals(expectedMw, actual, 0.9);
    }

    @Test
    public void proteinMwTestModExtent() {
        System.out.println("starting proteinMwTestModExtent");
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
        contribution.getMessages().forEach(m->{
            System.out.printf("message: %s; ", m.message);
        });
        double expectedMw = 47142.0;
        double actual =contribution.getMw();
        System.out.println("calculated MW: " + actual);
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
        Assertions.assertTrue( actual.keySet().stream().allMatch(k-> expected.get(k).getAsInt() == actual.get(k).getAsInt()));
    }
    
    @Test
    public void getSingleAAWeightTest() {
        String histideAbbreviation = "H";
        Double histidineMw = 155.156 - WATER_MW;
        Double actual = ProteinUtils.getSingleAAWeight(histideAbbreviation);
        assertEquals(histidineMw, actual, 0.01);
    }
    
    @Test
    public void getSubunitWeightTest() {
        Subunit unit1 = new Subunit();
        unit1.sequence="MKKLVIALCLMMVLAVMVEEAEAKWCFRVCYRGICYRRCRGKRNEVRQYRDRGYDVRAIPEETFFTRQDEDEDDDEE";
        Set<String> unknowns = new HashSet<>();
        Double expected = 9349.0;
        Double actual = ProteinUtils.getSubunitWeight(unit1, unknowns);
        assertEquals(expected, actual, 0.5);
        assertEquals(0, unknowns.size());
    }
    
    @Test
    public void getSubunitWeightTest2() {
        Subunit unit1 = new Subunit();
        unit1.sequence="MKKLVIALCLMMVLAVMVEEAEAKWCFRVCYRGICYRRCRGKRNEVRQYRDRGYDVRAIPEETFFTRQDEDEDDDEE@";
        Set<String> unknowns = new HashSet<>();
        Double expected = 9349.0;
        Double actual = ProteinUtils.getSubunitWeight(unit1, unknowns);
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
    public void getSubunitFormulaInfoTinyTest(){
        System.out.println("starting in getSubunitFormulaInfoTinyTest");
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
        Double expectedMwValue = 23900.0;
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

        System.out.println("mw: " + tryptophan.getMolecularWeight());

        return tryptophan;
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
    
    private Substance getPolymerFromFile() {
        try {
            File polymerFile =new ClassPathResource("testJSON/8OZM26QV2C.json").getFile();
            SubstanceBuilder builder =SubstanceBuilder.from(polymerFile);
            return builder.build();
        } catch (IOException ex) {
            Logger.getLogger(ProtCalculationTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
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

}
