package gsrs.api.substances;

import gsrs.substances.dto.CodeDTO;
import gsrs.substances.dto.NameDTO;
import gsrs.substances.dto.ReferenceDTO;
import gsrs.substances.dto.SubstanceDTO;
import gsrs.substances.util.SubstanceKeyResolver;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface SubstanceApi extends SubstanceKeyResolver {


    long count() throws IOException;

    <T extends SubstanceDTO> Optional<T> findByResolvedId(String anyKindOfId) throws IOException;

    <T extends SubstanceDTO> Optional<T> findById(Long id) throws IOException;

    boolean existsById(Long id) throws IOException;

    <T extends SubstanceDTO> T create(T dto) throws IOException;

    <T extends SubstanceDTO> T update(T dto) throws IOException;

    Optional<List<NameDTO>> getNamesOfSubstance(String anyKindOfSubstanceId) throws IOException;
    Optional<List<CodeDTO>> getCodesOfSubstance(String anyKindOfSubstanceId) throws IOException;
    Optional<List<ReferenceDTO>> getReferencesOfSubstance(String anyKindOfSubstanceId) throws IOException;
}
