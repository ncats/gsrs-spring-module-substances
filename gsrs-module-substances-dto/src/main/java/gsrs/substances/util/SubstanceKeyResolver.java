package gsrs.substances.util;

import gsrs.substances.dto.SubstanceDTO;

import java.io.IOException;
import java.util.Optional;

public interface SubstanceKeyResolver {

    default Optional<SubstanceDTO> resolveSubstance(SubstanceKey key) throws IOException{
        return resolveSubstance(key.getValue(), key.getType());
    }
    Optional<SubstanceDTO> resolveSubstance(String substanceKey, String substanceKeyType) throws IOException;
}
