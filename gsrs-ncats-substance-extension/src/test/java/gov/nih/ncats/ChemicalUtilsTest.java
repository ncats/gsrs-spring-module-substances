package gov.nih.ncats;

import gov.nih.ncats.molwitch.Chemical;
import gsrs.module.substance.utils.ChemicalUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class ChemicalUtilsTest {

    private final Map<String, String> saltData = readSaltsFile("src/test/resources/salts.txt");


    @Test
    public void testStripSalts() throws NullPointerException {
/*
        String currentPath = System.getProperty("user.dir");
        System.out.println(currentPath);
*/
        String smiles ="[Na+].[O-]C(=O)c1ccccc1";
        Chemical sodiumBenzoate=readMolfile("src/test/resources/sodium benzoate.mol");
        int atomsBefore =sodiumBenzoate.getAtomCount();
        System.out.println("molecule total atoms: " + atomsBefore);
        Chemical cleanBenzoate= ChemicalUtils.stripSalts3(sodiumBenzoate, saltData);
        int atomsAfter =cleanBenzoate.getAtomCount();
        Assertions.assertEquals(atomsBefore, (atomsAfter+1), "Salt removal leads to smaller molecule");
    }

    @Test
    public void testStripSaltsSmiles() throws IOException {
        String smiles ="[Na+].[O-]C(=O)c1ccccc1";
        Chemical sodiumBenzoate=Chemical.parse(smiles);
        int atomsBefore =sodiumBenzoate.getAtomCount();
        System.out.println("molecule total atoms: " + atomsBefore);
        Chemical cleanBenzoate= ChemicalUtils.stripSalts3(sodiumBenzoate, saltData);
        int atomsAfter =cleanBenzoate.getAtomCount();
        Assertions.assertEquals(atomsBefore, (atomsAfter+1), "Salt removal leads to smaller molecule");
    }

    @Test
    public void testStripMultipleSalts() {
        Chemical potassiumPhthalate=readMolfile2("src/test/resources/sodium potassium phthalate.mol");
        int atomsBefore =potassiumPhthalate.getAtomCount();
        System.out.println("molecule total atoms: " + atomsBefore);
        Chemical cleanBenzoate= ChemicalUtils.stripSalts3(potassiumPhthalate, saltData);
        int atomsAfter=0;
        try {
            atomsAfter = cleanBenzoate.getAtomCount();
        }
        catch(NullPointerException npe) {

        }
        Assertions.assertEquals(atomsBefore, (atomsAfter+2), "Salt removal leads to smaller molecule");
    }

    @Test
    public void testStripNegSaltsSmiles() throws IOException {
        String smiles ="[NH3+]c1ccccc1.[Cl-]";
        Chemical analineChloride=Chemical.parse(smiles);
        int atomsBefore =analineChloride.getAtomCount();
        System.out.println("initial molecule total atoms: " + atomsBefore);
        Chemical cleanAnaline= ChemicalUtils.stripSalts3(analineChloride, saltData);
        int atomsAfter =cleanAnaline.getAtomCount();
        Assertions.assertEquals(atomsBefore, (atomsAfter+1), "Salt removal leads to smaller molecule");
    }

    @Test
    public void testBreakBondsToMetals() throws IOException {
        String smiles ="[Cs][F]";
        String smilesExpected ="[F-].[Cs+]";
        Chemical cesiumFluoride=Chemical.parse(smiles);
        int atomsBefore =cesiumFluoride.getAtomCount();
        System.out.println("initial molecule total atoms: " + atomsBefore);
        boolean result= ChemicalUtils.breakBondsToMetals(cesiumFluoride);
        Assertions.assertTrue(result);
        String smilesAfter = cesiumFluoride.toSmiles();
        Assertions.assertEquals(smilesExpected, smilesAfter, "Metal removal leads to charged SMILES");
    }

    @Test
    public void testRemoveMetals() throws IOException {
        String smiles ="[Cs][F]";
        String smilesExpected ="[F-]";
        Chemical cesiumFluoride=Chemical.parse(smiles);
        int atomsBefore =cesiumFluoride.getAtomCount();
        System.out.println("initial molecule total atoms: " + atomsBefore);
        boolean result= ChemicalUtils.removeMetals(cesiumFluoride);
        Assertions.assertTrue(result);
        String smilesAfter = cesiumFluoride.toSmiles();
        Assertions.assertEquals(smilesExpected, smilesAfter, "Metal removal leads to charged SMILES");
    }

    @Test
    public void testRemoveMetals2() throws IOException {
        String smiles ="[Na]OS(=O)O[K]";
        String smilesExpected ="[O-]S([O-])=O";
        Chemical sodiumPotassiumSulfate=Chemical.parse(smiles);
        int atomsBefore =sodiumPotassiumSulfate.getAtomCount();
        System.out.println("initial molecule total atoms: " + atomsBefore);
        boolean result= ChemicalUtils.removeMetals(sodiumPotassiumSulfate);
        Assertions.assertTrue(result);
        String smilesAfter = sodiumPotassiumSulfate.toSmiles();
        Assertions.assertEquals(smilesExpected, smilesAfter, "Metal removal leads to charged SMILES");
    }


    private Map<String, String> readSaltsFile(String fileName) {
        Map<String, String> salts=new ConcurrentHashMap<>();
        FileInputStream fstream;
        try {
            fstream = new FileInputStream(fileName);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                salts.put(line.split("\t")[0], line.split("\t")[1]);
            }
            br.close();
        } catch ( IOException e) {
            // TODO Auto-generated catch block

            e.printStackTrace();
        }
        return salts;
    }

    @Test
    public void testStringReads() throws IOException {
        String fileName ="src/test/resources/sodium benzoate.mol";
        String contents1 =Files.lines(Paths.get(fileName)).collect(Collectors.joining("\n"));
        System.out.println("method 1: " + contents1);
        String contents2 =readAllBytesJava7(fileName);
        System.out.println("method 2: " + contents2);
        Assertions.assertNotEquals(contents1, contents2);
    }
    private Chemical readMolfile(String fileName) {
        Chemical chem=null;
        try {
            String mol =Files.lines(Paths.get(fileName)).collect(Collectors.joining("\n"));
            chem= Chemical.parseMol(mol);
        } catch ( Exception e) {

            e.printStackTrace();
        }
        return chem;
    }

    private Chemical readMolfile2(String fileName) {
        Chemical chem=null;
        try {
            String mol =readAllBytesJava7(fileName);
            chem= Chemical.parseMol(mol);
        } catch ( Exception e) {

            e.printStackTrace();
        }
        return chem;
    }

    // from https://howtodoinjava.com/java/io/java-read-file-to-string-examples/
    private static String readAllBytesJava7(String filePath)
    {
        String content = "";
        try
        {
            content = new String ( Files.readAllBytes( Paths.get(filePath) ) );
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return content;
    }
}
