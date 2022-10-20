package ix.core.models;

import lombok.Data;

/*
parameters for one rendering af a chemical structure
 */
@Data
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
