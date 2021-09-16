package gsrs.api.substances;

import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.molwitch.io.ChemFormat;
import lombok.Data;

import java.io.IOException;
import java.util.*;

@Data
public class StructureDTO extends BaseEditableDTO{


    private String molfile;

    private String smiles;
    private String formula;

    private int count;

    private int stereoCenters;
    private int definedStereo;
    private int ezCenters;
    private int charge;

    private Set<String> access = new LinkedHashSet<>();
    private Set<UUID> references = new LinkedHashSet<>();

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
