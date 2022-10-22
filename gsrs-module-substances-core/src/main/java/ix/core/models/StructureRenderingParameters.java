package ix.core.models;

import lombok.Data;
import lombok.ToString;

/*
parameters for one rendering af a chemical structure
 */
@Data
@ToString
public class StructureRenderingParameters {
    private Integer minHeight;
    private Integer minWidth;
    private Integer maxHeight;
    private Integer maxWidth;
    private Double bondLength;

    public boolean hasValuesForAll() {
        return (minHeight!=null && maxHeight != null && minWidth!=null && maxWidth!=null && bondLength!=null);
    }
}
