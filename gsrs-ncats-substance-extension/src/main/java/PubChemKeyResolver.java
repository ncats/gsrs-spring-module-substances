import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate;
import models.PubChemChemical;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.boot.restclient.RestTemplateBuilder;

@Service
public class PubChemKeyResolver extends AbstractLegacySearchGsrsEntityRestTemplate<PubChemChemical, Long> {

    @Autowired
    public PubChemKeyResolver(RestTemplateBuilder restTemplateBuilder, String baseUrl, String context, JsonMapper mapper) {
        super(restTemplateBuilder, baseUrl, "PUBCHEM", mapper);
    }

    @Override
    public Long getIdFrom( PubChemChemical dto) {
        return dto.getId();
    }

    @Override
    protected <S extends PubChemChemical> S parseFromJson(JsonNode node) {
        return null;
    }

}
