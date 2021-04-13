package ix.ginas.utils.validation.validators;


import gsrs.repository.GroupRepository;
import ix.core.models.Group;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashSet;
import java.util.regex.Pattern;

/**
 * Created by katzelda on 5/7/18.
 */
public class RemovePublicIndReferences extends AbstractValidatorPlugin<Substance> {
    private static final Pattern IND_PATTERN = Pattern.compile(".*[^A-Z]IND[^A-Z]*[0-9][0-9][0-9]*.*");

    @Autowired
    private GroupRepository groupRepository;

    public GroupRepository getGroupRepository() {
        return groupRepository;
    }

    public void setGroupRepository(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        s.references.stream()
                .filter(r->{
                    if("IND".equals(r.docType)){
                        return true;
                    }
                    return IND_PATTERN.matcher((" " + r.citation).toUpperCase()).find();
                })
                .filter(r->r.isPublic() || r.isPublicDomain() || r.isPublicReleaseReference())
                .forEach(r->{

                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE(
                                    "IND-like reference:\""
                                            + r.docType + ":" + r.citation + "\" cannot be public. Setting to protected.")
                            .appliableChange(true);
                    callback.addMessage(mes, ()-> makeReferenceProtected(r));
                });
    }

    private void makeReferenceProtected(Reference r){
        r.publicDomain=false;
        Group g= groupRepository.findByNameIgnoreCase("protected");
        if(g==null){
            g=new Group("protected");

        }

        EmbeddedKeywordList klist = new EmbeddedKeywordList();


        r.tags.stream()
                .filter(t->!Reference.PUBLIC_DOMAIN_REF.equals(t.getValue()))
                .forEach(klist::add);

        r.tags=klist;

        LinkedHashSet<Group> gs = new LinkedHashSet<>();
        gs.add(g);
        r.setAccess(gs);

    }
}
