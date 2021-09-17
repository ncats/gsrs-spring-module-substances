package gsrs.api.substances;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.molwitch.io.ChemFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StructureDTO extends SubstanceComponentBaseDTO{


    private UUID id;
    private String molfile;

    private String smiles;
    private String formula;

    private int count;

    private int stereoCenters;
    private int definedStereo;
    private int ezCenters;
    private int charge;


    public synchronized Optional<Chemical> asChemical() throws IOException {
        if(molfile !=null){
            return Optional.of(Chemical.parseMol(molfile));
        }
        if(smiles !=null){
            return Optional.of(Chemical.parse(smiles));
        }
        return Optional.empty();
    }

    public synchronized void updateFromChemical(Chemical chemical) throws IOException {
        String tmpMol = chemical.toMol();
        String tmpSmiles = chemical.toSmiles();

        smiles = tmpSmiles;
        molfile = tmpMol;
    }
}
