package gsrs.module.substance.datasource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.UrlResource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gsrs.module.substance.processors.CodeSystemUrlGenerator;
import ix.ginas.models.v1.Code;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultCodeSystemUrlGenerator implements DataSet<CodeSystemMeta>, CodeSystemUrlGenerator {

	// This ensures that the url is parsable from classpath protocol even
	// if there's no registered handler for classpath
	private static URL getUrl(String fname) throws MalformedURLException {
		if(fname.startsWith("classpath:")) {
			return DefaultCodeSystemUrlGenerator.class.getClassLoader().getResource(fname.split(":")[1]);	
		}
		return new URL(fname);
	}
	
    @JsonIgnore
    private final Map<String, CodeSystemMeta> map = new LinkedHashMap<>();

    @JsonCreator
    public DefaultCodeSystemUrlGenerator(@JsonProperty("codeSystems") Map<String, Map<String, String>> codeSystems,
                                         @JsonProperty("filename") String filename) throws IOException {
        if (filename != null) {
            if (!filename.contains(":")) {
                filename = "classpath:" + filename;
            }
            codeSystems = new LinkedHashMap<String, Map<String, String>>();
            try (InputStream is = new UrlResource(getUrl(filename)).getInputStream();) {
                ObjectMapper mapper = new ObjectMapper();
                for (JsonNode item : mapper.readTree(is)) {
                    Map<String, String> itemMap = mapper.convertValue(item, new TypeReference<Map<String, String>>(){});
                    codeSystems.put(itemMap.get("codeSystem"), itemMap);
                }
            }
        }
        for (Map<String, String> entry : codeSystems.values()) {
            CodeSystemMeta csmap = new CodeSystemMeta(entry.get("codeSystem"), entry.get("url"));
            map.put(csmap.codeSystem.toLowerCase(), csmap);
        }
    }

    @Override
    public Iterator<CodeSystemMeta> iterator() {
        return map.values().iterator();
    }

    @Override
    public boolean contains(CodeSystemMeta k) {
        return map.containsKey(k.codeSystem.toLowerCase());
    }

    public CodeSystemMeta fetch(String codesystem) {
        return this.map.get(codesystem.toLowerCase());
    }

    @Override
    public Optional<String> generateUrlFor(Code code) {
        log.trace("DefaultCodeSystemUrlGenerator generateUrlFor");
        if (code.code == null) {
            return Optional.empty();
        }
        CodeSystemMeta meta = fetch(code.codeSystem);
        if (meta == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(meta.generateUrlFor(code));
    }

    //used for testing --making sure the Map gets instantiated after a call to the constructor
    public Map getMap() {
        return map;
    }

}
