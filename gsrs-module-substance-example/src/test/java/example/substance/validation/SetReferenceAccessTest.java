package example.substance.validation;

import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.models.Group;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.SetReferenceAccess;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author mitch
 */
public class SetReferenceAccessTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    private TestGsrsValidatorFactory factory;

    @BeforeEach
    public void setup() {
        ValidatorConfig config = new DefaultValidatorConfig();
        config.setValidatorClass(SetReferenceAccess.class);
        config.setNewObjClass(Substance.class);
        Map<String, Object> parameters = new HashMap<>();

        LinkedHashMap<Integer, String> privateAlways = new LinkedHashMap<>();
        privateAlways.put(1, "ANDA");
        privateAlways.put(2, "BLA");
        privateAlways.put(3, "IND");
        parameters.put("alwaysPrivate", privateAlways);

        LinkedHashMap<Integer, String> generallyPublic = new LinkedHashMap<>();
        generallyPublic.put(1, "ACD");
        generallyPublic.put(2, "CLINICAL_TRIALS.GOV");
        generallyPublic.put(3, "WIKI");
        parameters.put("suggestedPublic", generallyPublic);
        config.setParameters(parameters);
        factory.addValidator("substances", config);
    }

    @Test
    public void singlePublicTypeSetToPrivate() throws Exception {

        Substance s1 = new SubstanceBuilder()
                .addName("sub3")
                .generateNewUUID()
                .build();
        assertCreated(s1.toFullJsonNode());
        Reference r = new Reference();
        r.citation = "something meant to be public";
        r.docType = "ACD";
        r.publicDomain = false;

        Group restricted = new Group("restricted");
        Set<Group> access = new HashSet<>();
        access.add(restricted);
        r.setAccess(access);
        s1.addReference(r);
        ValidationResponse response = substanceEntityService.validateEntity(s1.toFullJsonNode());

        //this is split up and stored as a variable for java 8 type inference to work...
        Stream<ValidationMessage> messageStream = response.getValidationMessages().stream();

        long total = messageStream.filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .filter(m2 -> m2.getMessage().contains("are typically public. Consider modifying the access and public domain flag"))
                .count();
        Assertions.assertEquals(1, total);
    }

    @Test
    public void singlePrivateTypeSetToPublic() throws Exception {

        Substance s1 = new SubstanceBuilder()
                .addName("sub3")
                .generateNewUUID()
                .build();
        assertCreated(s1.toFullJsonNode());
        Reference r = new Reference();
        r.citation = "something meant to be private";
        r.docType = "IND";
        r.publicDomain = true;
        s1.addReference(r);
        ValidationResponse response = substanceEntityService.validateEntity(s1.toFullJsonNode());

        //this is split up and stored as a variable for java 8 type inference to work...
        Stream<ValidationMessage> messageStream = response.getValidationMessages().stream();

        long total = messageStream.filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .filter(m2 -> m2.getMessage().contains("cannot be public. Setting to protected"))
                .count();
        Assertions.assertEquals(1, total);
    }

    @Test
    public void singlePrivateTypeSetToPublic2() throws Exception {

        Substance s1 = new SubstanceBuilder()
                .addName("sub3")
                .generateNewUUID()
                .build();
        assertCreated(s1.toFullJsonNode());
        Reference r = new Reference();
        r.citation = "something meant to be private";
        r.docType = "IND";
        r.publicDomain = true;
        s1.addReference(r);
        
        //really public
        Reference r2 = new Reference();
        r2.citation = "50-00-0";
        r2.docType = "ChemID";
        r2.publicDomain = true;
        s1.addReference(r2);
        ValidationResponse response = substanceEntityService.validateEntity(s1.toFullJsonNode());

        //this is split up and stored as a variable for java 8 type inference to work...
        Stream<ValidationMessage> messageStream = response.getValidationMessages().stream();

        long total = messageStream.filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .filter(m2 -> m2.getMessage().contains("cannot be public. Setting to protected"))
                .count();
        Assertions.assertEquals(1, total);
    }


    @Test
    public void twoPrivateTypeSetToPublic() throws Exception {

        Substance s1 = new SubstanceBuilder()
                .addName("sub3")
                .generateNewUUID()
                .build();
        assertCreated(s1.toFullJsonNode());
        Reference r = new Reference();
        r.citation = "something meant to be private";
        r.docType = "IND";
        r.publicDomain = true;
        s1.addReference(r);
        Reference r2 = new Reference();
        r2.citation = "something else meant to be private";
        r2.docType = "ANDA";
        r2.publicDomain = true;
        s1.addReference(r2);
        ValidationResponse response = substanceEntityService.validateEntity(s1.toFullJsonNode());

        //this is split up and stored as a variable for java 8 type inference to work...
        Stream<ValidationMessage> messageStream = response.getValidationMessages().stream();

        long total = messageStream.filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .filter(m2 -> m2.getMessage().contains("cannot be public. Setting to protected"))
                .count();
        Assertions.assertEquals(2, total);
    }

    
}
