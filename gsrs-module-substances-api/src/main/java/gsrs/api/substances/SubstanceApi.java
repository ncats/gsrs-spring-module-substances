package gsrs.api.substances;

import java.io.IOException;
import java.util.Optional;

public interface SubstanceApi {


    long count() throws IOException;

    <T extends SubstanceDTO> Optional<T> findByResolvedId(String anyKindOfId) throws IOException;

    <T extends SubstanceDTO> Optional<T> findById(Long id) throws IOException;

    boolean existsById(Long id) throws IOException;

    <T extends SubstanceDTO> T create(T dto) throws IOException;

    <T extends SubstanceDTO> T update(T dto) throws IOException;
}
