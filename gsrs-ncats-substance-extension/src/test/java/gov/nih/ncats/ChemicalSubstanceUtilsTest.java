package gov.nih.ncats;

import gsrs.module.substance.utils.ChemicalSubstanceUtils;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Reference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ChemicalSubstanceUtilsTest {

    @Test
    public void testCreateNameReference() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ChemicalSubstanceUtils utils = new ChemicalSubstanceUtils();
        String methodNameToTest ="createNameReference";
        Method method= utils.getClass().getDeclaredMethod(methodNameToTest, String.class);
        method.setAccessible(true);
        String id= "1A";
        Object result= method.invoke(utils, id);
        Reference nameRef = (Reference) result;
        Assertions.assertEquals(id, nameRef.citation);
    }

    @Test
    public void testMakeChemicalSubstanceNull() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ChemicalSubstanceUtils utils = new ChemicalSubstanceUtils();
        String methodNameToTest ="makeChemicalSubstance";
        Method method= utils.getClass().getDeclaredMethod(methodNameToTest, Map.class);
        method.setAccessible(true);
        Map<String, String> fieldsForTest=new HashMap<>();
        fieldsForTest.put("something", "value");
        //required fields are missing from the Map -> expected null;
        Assertions.assertNull(method.invoke(utils, fieldsForTest));
    }

    @Test
    public void testMakeChemicalSubstance() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ChemicalSubstanceUtils utils = new ChemicalSubstanceUtils();
        String methodNameToTest ="makeChemicalSubstance";
        Method method= utils.getClass().getDeclaredMethod(methodNameToTest, Map.class);
        method.setAccessible(true);
        Map<String, String> fieldsForTest=new HashMap<>();
        fieldsForTest.put("MOLFILE", "\\n  ACCLDraw11222118002D\\n\\n 10  9  0  0  0  0  0  0  0  0999 V2000\\n    7.3750   -7.9063    0.0000 Na  0  3  0  0  0  0  0  0  0  0  0  0\\n    5.3528   -9.1276    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.3951   -9.1271    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    6.3759   -8.5373    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    7.3951  -10.3085    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    5.3528  -10.3138    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    6.3784  -10.8981    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    8.4183  -10.8992    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    9.4414  -10.3085    0.0000 O   0  5  0  0  0  0  0  0  0  0  0  0\\n    8.4183  -12.0807    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n  7  5  1  0  0  0  0\\n  6  7  2  0  0  0  0\\n  3  4  1  0  0  0  0\\n  2  6  1  0  0  0  0\\n  5  3  2  0  0  0  0\\n  4  2  2  0  0  0  0\\n  5  8  1  0  0  0  0\\n  8  9  1  0  0  0  0\\n  8 10  2  0  0  0  0\\nM  CHG  2   1   1   9  -1\\nM  END\\n");
        String id = "582-25-2";
        String substanceName ="Potassium benzoate";
        fieldsForTest.put("ID", id);
        fieldsForTest.put("CAS Number", id);
        fieldsForTest.put("Sample Name", substanceName);
        fieldsForTest.put("Supplier", "TCRIS");
        fieldsForTest.put("Supplier Code", "T-20201");//made-up

        ChemicalSubstance chemical= (ChemicalSubstance) method.invoke(utils, fieldsForTest);
        Assertions.assertEquals(substanceName, chemical.names.get(0).name);
    }

    @Test
    public void testParseSdFile0() throws IOException {
        String sdFilePath ="src/test/resources/nlm4-dup-rem-small-set.sdf";
        Map<String, String> fieldMappings = new HashMap<>();
        fieldMappings.put( "CAS", "CAS Number");
        fieldMappings.put( "main name", "Sample Name");
        ChemicalSubstanceUtils utils = new ChemicalSubstanceUtils();
        List<ChemicalSubstance> chemicals= utils.parseSdFile(sdFilePath, fieldMappings);

        String nameToMatch ="Yggflgp(KA'LA')2-ome";
        String casToMatch ="117743-73-4";
        AtomicInteger count = new AtomicInteger(0);
        chemicals.forEach(c->
            System.out.printf("chemical %d: ; name: %s; codes: %s%n",
            count.incrementAndGet(),
            c.names.get(0).name,
            c.codes.stream().map(cc->"system: " + cc.codeSystem + "; code: " + cc.code).collect(Collectors.joining("+")))
        );
        Assertions.assertEquals(16, chemicals.size());
        Assertions.assertTrue(chemicals.stream().anyMatch(c->c.names.get(0).name.equals(nameToMatch)));
        Assertions.assertTrue(chemicals.stream().anyMatch(c->c.codes.stream().anyMatch(cc->cc.codeSystem.equals("CAS")&&cc.code.equals(casToMatch))));

    }

    @Test
    public void testParseSdFile() throws IOException {
        String sdFilePath ="src/test/resources/nlm_5 first-enhanced.sdf";
        Map<String, String> fieldMappings = new HashMap<>();
        fieldMappings.put( "CAS", "CAS Number");
        fieldMappings.put( "main name", "Sample Name");
        fieldMappings.put( "Salt", "Salt Code");
        fieldMappings.put( "Salt Equiv", "Salt Equivalents");
        ChemicalSubstanceUtils utils = new ChemicalSubstanceUtils();
        List<ChemicalSubstance> chemicals= utils.parseSdFile(sdFilePath, fieldMappings);

        String saltToMatch ="Na+";
        AtomicInteger count = new AtomicInteger(0);
        chemicals.forEach(c-> System.out.printf("chemical %d: ; name: %s; codes: %s",
                count.incrementAndGet(),
                c.names.get(0).name,
                c.codes.stream().map(cc->" system: " + cc.codeSystem + "; code: " + cc.code).collect(Collectors.joining("+"))));
        Assertions.assertTrue(chemicals.stream().anyMatch(c->c.codes.stream().anyMatch(cc->cc.codeSystem.equalsIgnoreCase("salt code")&&cc.code.equals(saltToMatch))));

        long expectedNumberOfSaltCodes =4;
        long actualCount = chemicals.stream().map(c->c.codes).flatMap(cc->cc.stream().filter(c2->c2.codeSystem.equals("Salt Code"))).count();
        Assertions.assertEquals(expectedNumberOfSaltCodes, actualCount);
    }

    @Test
    public void testParseSdFile2() throws IOException {
        String sdFilePath ="src/test/resources/nlm_5 first-enhanced2.sdf";
        Map<String, String> fieldMappings = new HashMap<>();
        fieldMappings.put( "CAS", "CAS Number");
        fieldMappings.put( "main name", "Sample Name");
        fieldMappings.put( "Salt", "Salt Code");
        fieldMappings.put( "Salt Equiv", "Salt Equivalents");
        fieldMappings.put("Supplier", "Supplier");
        fieldMappings.put("Supplier Code", "Supplier Code");
        ChemicalSubstanceUtils utils = new ChemicalSubstanceUtils();
        List<ChemicalSubstance> chemicals= utils.parseSdFile(sdFilePath, fieldMappings);

        AtomicInteger count = new AtomicInteger(0);
        chemicals.forEach(c-> System.out.printf("chemical %d: ; name: %s; codes: %s",
                count.incrementAndGet(),
                c.names.get(0).name,
                c.codes.stream().map(cc->" system: " + cc.codeSystem + "; code: " + cc.code).collect(Collectors.joining("+"))));

        long expectedNumberOfSuppliers =2;
        long actualCount = chemicals.stream().map(c->c.codes).flatMap(cc->cc.stream().filter(c2->c2.codeSystem.equals("Supplier"))).count();
        Assertions.assertEquals(expectedNumberOfSuppliers, actualCount);
    }

}
