package gsrs.module.substance.services;

import gsrs.DefaultDataSourceConfig;
import gsrs.substances.dto.SubstanceDTO;
import gsrs.substances.util.SubstanceKeyResolver;
import ix.ginas.models.v1.Substance;

import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
public class EntityManagerSubstanceKeyResolver implements SubstanceKeyResolver {

    @PersistenceContext(unitName =  DefaultDataSourceConfig.NAME_ENTITY_MANAGER)
    public EntityManager entityManager;

    public Optional<Substance> resolveEMSubstance(String substanceKey, String substanceKeyType) throws NotUniqueKeyException, IOException {

        Query query = null;
        // TO DO, convert Substance to SubstanceDTO
        if ((substanceKeyType!= null) && (substanceKeyType.equalsIgnoreCase("UUID"))) {
            query = entityManager.createQuery("SELECT s FROM Substance s WHERE s.uuid=:subKey");
            query.setParameter("subKey", UUID.fromString(substanceKey));

        } else if ((substanceKeyType!= null) && (substanceKeyType.equalsIgnoreCase("APPROVAL_ID"))) {
            query = entityManager.createQuery("SELECT s FROM Substance s WHERE s.approvalID=:subKey");
            query.setParameter("subKey", substanceKey);
        } else {
            query = entityManager.createQuery("SELECT s FROM Substance s JOIN s.codes c WHERE c.type = 'PRIMARY' and c.codeSystem=:subKeyType and c.code=:subKey");
            query.setParameter("subKey", substanceKey);
            query.setParameter("subKeyType", substanceKeyType);
        }
        Substance sub = (Substance) query.getSingleResult();
        return Optional.ofNullable(sub);
    }

    @Override
    public Optional<SubstanceDTO> resolveSubstance(String substanceKey, String substanceKeyType) throws NotUniqueKeyException, IOException {
        // TO DO Call the other method and convert to DTO
        return Optional.empty();
    }
}
