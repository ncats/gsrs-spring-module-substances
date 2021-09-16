package gsrs.api.substances;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LazyFetchedCollection {

    private int count;
    private String href;
}
