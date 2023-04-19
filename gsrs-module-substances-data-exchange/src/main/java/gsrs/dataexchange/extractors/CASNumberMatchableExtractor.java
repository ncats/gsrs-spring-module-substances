package gsrs.dataexchange.extractors;

import java.util.Arrays;
import java.util.List;

public class CASNumberMatchableExtractor extends CodeMatchableExtractor{
    //todo: move the code systems to config
    private final static List<String> casCodeSystems = Arrays.asList("CAS", "CASNum", "CASNo");
    private final static String CAS_TYPE="PRIMARY";
    private final static String CAS_KEY="CAS NUMBER";
    public CASNumberMatchableExtractor() {
        super(casCodeSystems, CAS_TYPE, CAS_KEY);
    }

}
