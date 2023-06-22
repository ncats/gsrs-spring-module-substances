package example.imports;

import gsrs.module.substance.importers.importActionFactories.NameExtractorActionFactory;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class NameExtractorActionFactoryTests {

    @Test
    public void testNameNeg(){
        String input="Aspirin";
        List<String> pieces = new ArrayList<>();
        boolean result =NameExtractorActionFactory.looksLikeGsrsExportedName(input, pieces);
        Assertions.assertFalse(result);
        Assertions.assertEquals(0, pieces.size());
    }

    @Test
    public void testNamePos1(){
        String input="IMIDAZOLE|en|true";
        List<String> pieces = new ArrayList<>();
        boolean result =NameExtractorActionFactory.looksLikeGsrsExportedName(input, pieces);
        Assertions.assertTrue(result);
        Assertions.assertEquals(3, pieces.size());
        Assertions.assertEquals("IMIDAZOLE", pieces.get(0));
        Assertions.assertEquals("en", pieces.get(1));
        Assertions.assertEquals("true", pieces.get(2));
    }

    @Test
    public void testNamePos2(){
        String input="Paclitaxel trevatide [WHO-DD]|en|false";
        List<String> pieces = new ArrayList<>();
        boolean result =NameExtractorActionFactory.looksLikeGsrsExportedName(input, pieces);
        Assertions.assertTrue(result);
        Assertions.assertEquals(3, pieces.size());
        Assertions.assertEquals("Paclitaxel trevatide [WHO-DD]", pieces.get(0));
        Assertions.assertEquals("en", pieces.get(1));
        Assertions.assertEquals("false", pieces.get(2));
    }

    @Test
    public void testNamePos3(){
        String input="IMIDAZOLE [WHO-DD]|en|";
        List<String> pieces = new ArrayList<>();
        boolean result =NameExtractorActionFactory.looksLikeGsrsExportedName(input, pieces);
        Assertions.assertTrue(result);
        Assertions.assertEquals(2, pieces.size());
        Assertions.assertEquals("IMIDAZOLE [WHO-DD]", pieces.get(0));
        Assertions.assertEquals("en", pieces.get(1));
    }

}
