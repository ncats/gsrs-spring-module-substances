package gsrs.module.substance.indexers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SmartsIndexable {
    private String name;
    private List<String> SMARTSList;
}
