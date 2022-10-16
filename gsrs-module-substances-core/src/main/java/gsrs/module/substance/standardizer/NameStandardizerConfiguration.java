package gsrs.module.substance.standardizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Configuration
@ConfigurationProperties("gsrs.standardizers.substances")
@Data
public class NameStandardizerConfiguration {

    private Map<String, Object> name;
    private Map<String, Object> stdname;

    @Bean
    public NameStandardizer nameStandardizer() throws InstantiationException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        List<String> search = Arrays.asList("\\s{2,}", "\u00B9", "\u00B2", "\u00B3", "&lt;-", "-&gt;", "\\+\\/-", "<\\/sup><sup>", "<\\/sub><sub>", "<\\/i><i>");
        List<String> replace = Arrays.asList(" ", "<sup>1</sup>", "<sup>2</sup>", "<sup>3</sup>", "\u2190", "\u2192", "\u00B1", "", "", "");
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("search", (Map<Integer, String>) search.stream().collect(Collectors.toMap(e->search.indexOf(e), e->e)));
        final Iterator<Integer> iter = IntStream.range(0, replace.size()).iterator();
        parameters.put("replace", (Map<Integer, String>) replace.stream().collect(Collectors.toMap(e->iter.next(), e->e)));
        return buildStandardizer(name, parameters);
    }

    @Bean
    public NameStandardizer stdNameStandardizer() throws InstantiationException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        List<String> search = Arrays.asList("\u2190", "\u2192", "±", "\u00B1", "\\×", "\u00B9", "\u00B2",
            "\u00B3", "\u2070", "\u2071", "\u2072", "\u2073", "\u2074", "\u2075", "\u2076", "\u2077",
            "\u2078", "\u2079", "\u207A", "\u207B", "\u2080", "\u2081", "\u2082", "\u2083", "\u2084", "\u2085",
            "\u2086", "\u2087", "\u2088", "\u2089", "\u208A", "\u208B", "ʟ", "ᴅ", "[\u200B\u200C\u200D\u2060\uFEFF]",
            "[\u00B4\u02B9\u02BC\u02C8\u0301\u2018\u2019\u201B\u2032\u2034\u2037]",
            "[\u00AB\u00BB\u02BA\u030B\u030E\u201C\u201D\u201E\u201F\u2033\u2036\u3003\u301D\u301E]",
            "[\u00AD\u2010\u2011\u2012\u2013\u2014\u2212\u2015]", "[\u01C3\u2762]", "[\u266F]", "[\u066A\u2052]",
            "[\u066D\u204E\u2217\u2731\u00D7]", "[\u201A\uFE51\uFF64\u3001]", "[\u00F7\u0338\u2044\u2215]",
            "[\u0589\u05C3\u2236]", "[\u203D]", "[\u27E6]", "[\u20E5\u2216]", "[\u301B]",
            "[\u02C4\u02C6\u0302\u2038\u2303]", "[\u02CD\u0331\u0332\u2017]", "[\u02CB\u0300\u2035]",
            "[\u2983]", "[\u01C0\u05C0\u2223\u2758]", "[\u2016]", "[\u02DC\u0303\u2053\u223C\u301C]",
            "[\u2039\u2329\u27E8\u3008]", "[\u2264\u2266]", "[\u203A\u232A\u27E9\u3009]", "[\u2265\u2267]",
            "[\uFEFF]", "\u2153", "\u2154", "\u2155", "\u2156", "\u2157", "\u2158", "\u2159", "\u215A",
            "\u215B", "\u215C", "\u215D", "\u215E", "\u00BC", "\u00BD", "\u2026", "﹘", "\\[", "\\{", "\\]", "\\}",
            "\\s{2,}", "\u03B1", "\u03B2", "\u03B3", "\u03B4", "\u03B5", "\u03B6", "\u03B7", "\u03B8",
            "\u03B9", "\u03BA", "\u03BB", "\u03BC", "\u03BD", "\u03BE", "\u03BF", "\u03C0", "\u03C1",
            "\u03C2", "\u03C3", "\u03C4", "\u03C5", "\u03C6", "\u03C7", "\u03C8", "\u03C9", "\u0391",
            "\u0392", "\u0393", "\u0394", "\u0395", "\u0396", "\u0397", "\u0398", "\u0399", "\u039A",
            "\u039B", "\u039C", "\u039D", "\u039E", "\u039F", "\u03A0", "\u03A1", "\u03A3", "\u03A4",
            "\u03A5", "\u03A6", "\u03A7", "\u03A8", "\u03A9", "<i>", "</i>", "</sup><sup>", "</sub><sub>",
            "<sup>", "</sup>", "<sub>", "</sub>");
        List<String> replace = Arrays.asList("<-", "->", "+/-", "+/-", "X", "1", "2", "3", "0", "1", "2",
            "3", "4", "5", "6", "7", "8", "9", "+", "-", "0" , "1", "2", "3", "4", "5", "6",
            "7", "8", "9", "+", "-", "L", "D", "", "'", "\"", "-", "!", "#", "%", "*", ",",
            "/", ":", "?", "[", "\\", "]", "^", "_", "`", "(", "|", "||", "~", "<", "<=", ">",
            ">=", " ", "1/3", "2/3", "1/5", "2/5", "3/5", "4/5", "1/6", "5/6", "1/8", "3/8",
            "5/8", "7/8", "1/4", "1/2", "...", "-", "(", "(", ")", ")",
            " ", ".ALPHA.", ".BETA.", ".GAMMA.", ".DELTA.", ".EPSILON.",
            ".ZETA.", ".ETA.", ".THETA.", ".IOTA.", ".KAPPA.", ".LAMBDA.", ".MU.", ".NU.",
            ".XI.", ".OMICRON.", ".PI.", ".RHO.", ".SIGMA.", ".SIGMA.", ".TAU.", ".UPSILON.",
            ".PHI.", ".CHI.", ".PSI.", ".OMEGA.", ".ALPHA.", ".BETA.", ".GAMMA.", ".DELTA.",
            ".EPSILON.", ".ZETA.", ".ETA.", ".THETA.", ".IOTA.", ".KAPPA.", ".LAMBDA.",
            ".MU.", ".NU.", ".XI.", ".OMICRON.", ".PI.", ".RHO.", ".SIGMA.",
            ".TAU.", ".UPSILON.", ".PHI.", ".CHI.", ".PSI.", ".OMEGA.", "", "", "", "",
            "SUP(", ")", "SUB(", ")");
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("search", (Map<Integer, String>) search.stream().collect(Collectors.toMap(e->search.indexOf(e), e->e)));
        final Iterator<Integer> iter = IntStream.range(0, replace.size()).iterator();
        parameters.put("replace", (Map<Integer, String>) replace.stream().collect(Collectors.toMap(e->iter.next(), e->e)));
        parameters.put("plainText", false);
        parameters.put("upperCase", true);
        parameters.put("preserveTag", true);
        parameters.put("nkfdNormalize", true);
        parameters.put("removeUnprintables", true);
        return buildStandardizer(stdname, parameters);
    }

    private NameStandardizer buildStandardizer(Map<String, Object> config, Map<String, Object> parameters) throws InstantiationException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        ObjectMapper mapper = new ObjectMapper();
        Class standardizerClass = HtmlNameStandardizer.class;
        if (config != null) {
            if (config.containsKey("standardizerClass")) {
                standardizerClass = Class.forName((String) config.get("standardizerClass"));
            }
            if (config.containsKey("parameters")) {
                parameters.putAll((Map<String, Object>) config.get("parameters"));
            }
        }
        if(parameters.isEmpty()) {
            return (NameStandardizer) mapper.convertValue(Collections.emptyMap(), standardizerClass);
        }
        if (parameters.containsKey("replacements")) {
            Map<String, String> replacements = (Map<String, String>) parameters.remove("replacements");
            List<String> searchStrings = ((Map<String, String>) parameters.getOrDefault("search", Collections.emptyMap())).entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->e.getValue()).collect(Collectors.toList());
            List<String> replaceStrings = ((Map<String, String>) parameters.getOrDefault("replace", Collections.emptyMap())).entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->e.getValue()).collect(Collectors.toList());
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                int idx = searchStrings.indexOf(entry.getKey());
                if (idx > -1) {
                    replaceStrings.set(idx, entry.getValue());
                } else {
                    searchStrings.add(entry.getKey());
                    replaceStrings.add(entry.getValue());
                }
            }
            parameters.put("search", (Map<Integer, String>) searchStrings.stream().collect(Collectors.toMap(e->searchStrings.indexOf(e), e->e)));
            final Iterator<Integer> iter = IntStream.range(0, replaceStrings.size()).iterator();
            parameters.put("replace", (Map<Integer, String>) replaceStrings.stream().collect(Collectors.toMap(e->iter.next(), e->e)));
        }
        return (NameStandardizer) mapper.convertValue(parameters, standardizerClass);
    }
}
