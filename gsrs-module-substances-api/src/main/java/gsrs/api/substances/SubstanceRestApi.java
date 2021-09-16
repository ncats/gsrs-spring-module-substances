package gsrs.api.substances;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.api.GsrsEntityRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class SubstanceRestApi extends GsrsEntityRestTemplate<SubstanceDTO, UUID> implements SubstanceApi{
    public SubstanceRestApi(RestTemplateBuilder restTemplateBuilder, String baseUrl, ObjectMapper mapper) {
        super(restTemplateBuilder, baseUrl, "substances", mapper);
    }

    @Override
    protected SubstanceDTO parseFromJson(JsonNode node) {
        return getObjectMapper().convertValue(node, SubstanceDTO.class);
    }

    @Override
    protected UUID getIdFrom(SubstanceDTO dto) {
        return dto.getUuid();
    }

    @Override
    public <T extends SubstanceDTO> Optional<T> findByResolvedId(String anyKindOfId) throws IOException {
        return super.findByResolvedId(anyKindOfId);
    }

    @Override
    public Optional<SubstanceDTO> findById(Long id) throws IOException {
        return Optional.empty();
    }

    @Override
    public boolean existsById(Long id) throws IOException {
        return false;
    }

}
