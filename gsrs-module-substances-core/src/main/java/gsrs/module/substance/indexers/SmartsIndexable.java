package gsrs.module.substance.indexers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class SmartsIndexable {
    private String name;
    private List<String> SMARTSList;

    public SmartsIndexable(Map<String, String> map){
        this.name = map.getOrDefault("indexableName", "");
        this.SMARTSList = map.get("smarts") != null && map.get("smarts").length()> 0
                ? Arrays.asList(map.get("smarts").split(SmartsIndexValueMaker.NAME_TO_VALUE_DELIM))
                : Collections.EMPTY_LIST;
    }

    public SmartsIndexable(String indexableName, Map<String, String> rawSmarts){
        this.name = indexableName;
        this.SMARTSList = rawSmarts.values().stream().collect(Collectors.toList());
    }

    public boolean isValid() {
        return this.name != null && this.name.length() > 0 && this.SMARTSList != null && !this.SMARTSList.isEmpty();
    }
}
