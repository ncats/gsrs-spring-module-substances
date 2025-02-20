package example.chem;

import ix.core.models.Structure;
import ix.core.util.pojopointer.extensions.InChIFullRegisteredFunction;
import ix.core.util.pojopointer.extensions.InChIRegisteredFunction;
import ix.ginas.models.v1.GinasChemicalStructure;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class InchiFunctionTest {

    @ParameterizedTest
    @MethodSource("inputData")
    void testInchi(String molname, String description, Structure.Stereo stereo, boolean expectDelimiter) throws IOException {
        String molfileText = IOUtils.toString(
                this.getClass().getResourceAsStream("/molfiles/" + molname + ".mol"),
                "UTF-8"
        );
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile = molfileText;
        structure.stereoChemistry = stereo;
        InChIFullRegisteredFunction functionHolder = new InChIFullRegisteredFunction();
        BiFunction<InChIFullRegisteredFunction.InChIFullPath, Structure, Optional<String>> fun = functionHolder.getOperation();
        Optional<String> result =fun.apply(new InChIFullRegisteredFunction.InChIFullPath(), structure);
        System.out.printf("InChIs for %s: %s\n", description, result.get());
        Assertions.assertEquals(expectDelimiter, result.get().contains(InChIFullRegisteredFunction.MULTIPLE_VALUE_DELIMITER));
    }

    @ParameterizedTest
    @MethodSource("inputData")
    void testInchiKey(String molname, String description, Structure.Stereo stereo, boolean expectDelimiter) throws IOException {
        String molfileText = IOUtils.toString(
                this.getClass().getResourceAsStream("/molfiles/" + molname + ".mol"),
                "UTF-8"
        );
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile = molfileText;
        structure.stereoChemistry = stereo;
        InChIRegisteredFunction functionHolder = new InChIRegisteredFunction();
        BiFunction<InChIRegisteredFunction.InChIPath, Structure, Optional<String>> fun = functionHolder.getOperation();
        Optional<String> result =fun.apply(new InChIRegisteredFunction.InChIPath(), structure);
        System.out.printf("InChIKeys for %s: %s\n", description, result.get());
        Assertions.assertEquals(expectDelimiter, result.get().contains(InChIRegisteredFunction.MULTIPLE_VALUE_DELIMITER));
    }

    private static Stream<Arguments> inputData() {
        return Stream.of(
                Arguments.of("N9Y5G2T2T5", "N9Y5G2T2T5 with unknown stereo", Structure.Stereo.UNKNOWN, false),
                Arguments.of("N9Y5G2T2T5", "N9Y5G2T2T5 with racemic stereo", Structure.Stereo.RACEMIC, true),
                Arguments.of("N9Y5G2T2T5", "N9Y5G2T2T5 with epimeric stereo", Structure.Stereo.EPIMERIC, true),
                Arguments.of("SV1ATP0KYY", "SV1ATP0KYY with unknown stereo", Structure.Stereo.UNKNOWN, false),
                Arguments.of("SV1ATP0KYY", "SV1ATP0KYY with racemic stereo", Structure.Stereo.RACEMIC, true),
                Arguments.of("SV1ATP0KYY", "SV1ATP0KYY with epimeric stereo", Structure.Stereo.EPIMERIC, true)

        );
    }
}
