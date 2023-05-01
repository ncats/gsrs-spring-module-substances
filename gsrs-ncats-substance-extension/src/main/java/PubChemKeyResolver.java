import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate;
import gsrs.substances.dto.SubstanceDTO;
import gsrs.substances.util.SubstanceKey;
import gsrs.substances.util.SubstanceKeyResolver;
import models.PubChemChemical;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
public class PubChemKeyResolver extends AbstractLegacySearchGsrsEntityRestTemplate<PubChemChemical, Long> {

    @Autowired
    public PubChemKeyResolver(RestTemplateBuilder restTemplateBuilder, String baseUrl, String context, ObjectMapper mapper) {
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

/*
    @Override
    public Optional<SubstanceDTO> resolveSubstance(SubstanceKey key) throws IOException{
        return resolveSubstance(key.getValue(), key.getType());
    }
*/

    /*@Override
    public Optional<SubstanceDTO> resolveSubstance(String substanceKey, String substanceKeyType) throws NotUniqueKeyException, IOException{

    }*/
}
