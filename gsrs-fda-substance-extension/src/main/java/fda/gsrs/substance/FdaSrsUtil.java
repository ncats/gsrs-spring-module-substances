package fda.gsrs.substance;

import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FdaSrsUtil {

    //methods copied from GinasSrsApp

    public static Stream<Code> getBdNumCode(Substance s) {
        return s.getCodes().stream().filter(c -> "BDNUM".equals(c.codeSystem));
    }

    public static Optional<String> getBdNum(Substance s) {
        if (s != null) {
            List<String> bdnums = getBdNumCode(s).map(Code::getCode).collect(Collectors.toList());
            // TODO fix logic here when we support multiple bdnums
            if (!bdnums.isEmpty()) {
                return Optional.of(bdnums.get(0));
            }
        }
        return Optional.empty();
    }
}
