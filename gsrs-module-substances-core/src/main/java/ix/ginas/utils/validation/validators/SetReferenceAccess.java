package ix.ginas.utils.validation.validators;

import gsrs.repository.GroupRepository;
import ix.core.models.Group;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

/**
 * Set access on references according to configured rules
 *
 * @author mitch
 */
@Slf4j
@Component
public class SetReferenceAccess extends AbstractValidatorPlugin<Substance>
{

    @Autowired
    private GroupRepository groupRepository;

    //temporarily instantiate from hard-coded strings
    private LinkedHashMap<Integer, String> alwaysPublic = new LinkedHashMap<>();
    private LinkedHashMap<Integer, String> alwaysPrivate  = new LinkedHashMap<>();
    private LinkedHashMap<Integer, String> suggestedPublic = new LinkedHashMap<>();

    private LinkedHashMap<Integer, Pattern> referenceCitationPatterns  = new LinkedHashMap<>();

    public SetReferenceAccess() {
        log.debug("in SetReferenceAccess ctor" );
    }

    @Override
    public void validate(Substance substance, Substance oldSubstance, ValidatorCallback callback) {
        log.trace("Starting in SetReferenceAccess.validate");
        log.trace("alwaysPublic: " + alwaysPublic);
        log.trace("alwaysPrivate: " + alwaysPrivate);
        log.trace("suggestedPublic: " + suggestedPublic);

        substance.references.forEach(r -> {
            String msg = String.format("doc type: %s; isPublic: %b; isPublicDomain: %b; isPublicReleaseReference: %b",
                    r.docType, r.isPublic(), r.isPublicDomain(), r.isPublicReleaseReference());
            log.debug(msg);

            if ((alwaysPrivate.values().contains(r.docType))
                    && (r.isPublic() || r.isPublicDomain() || r.isPublicReleaseReference())) {
                GinasProcessingMessage mes = GinasProcessingMessage
                        .WARNING_MESSAGE(
                                "Protected reference:\""
                                        + r.docType + ":" + r.citation + "\" cannot be public. Setting to protected.")
                        .appliableChange(true);
                callback.addMessage(mes, () -> makeReferenceProtected(r));
            }else if (referenceCitationPatterns.values().stream().anyMatch(p -> p.matcher((" " + r.citation).toUpperCase()).find()) ) {
							if (r.isPublic() || r.isPublicDomain() || r.isPublicReleaseReference()) {
                GinasProcessingMessage mes = GinasProcessingMessage
                        .WARNING_MESSAGE(
                                "Reference:\""
                                        + r.docType + ":" + r.citation + "\" appears to be non-public. Setting to protected.")
                        .appliableChange(true);
                callback.addMessage(mes, () -> makeReferenceProtected(r));
							}
            }else if (alwaysPublic.values().contains(r.docType)
                    && (!r.isPublic() || !r.isPublicDomain())) {
                GinasProcessingMessage mes = GinasProcessingMessage
                        .WARNING_MESSAGE(
                                "Public reference:\""
                                        + r.docType + ":" + r.citation + "\" cannot be private. Setting to public.")
                        .appliableChange(true);
                callback.addMessage(mes, () -> makeReferencePublic(r));
            }else if(suggestedPublic.containsValue(r.docType) && (!r.isPublic() || !r.isPublicDomain())) {
                String messageText =String.format("References of type %s, such as \"%s:%s,\" are typically public. Consider modifying the access and public domain flag, unless there is an explicit reason to keep it restricted.", 
                        r.docType, r.docType, r.citation);
                if(!substanceNotesContainWarning(substance, messageText)){
                    GinasProcessingMessage mes = GinasProcessingMessage
                        .WARNING_MESSAGE(messageText);
                    callback.addMessage(mes);
                }else {
                    log.debug("warning already noted: " + messageText);
                }
            }
        });
    }


    protected void makeReferenceProtected(Reference r) {
        r.publicDomain = false;

        Group g = groupRepository.findByName("protected");
        if (g ==null) {
            g =  new Group("protected");
        }

        EmbeddedKeywordList klist = new EmbeddedKeywordList();

        r.tags.stream()
                .filter(t -> !Reference.PUBLIC_DOMAIN_REF.equals(t.getValue()))
                .forEach(klist::add);
        r.tags = klist;

        LinkedHashSet<Group> gs = new LinkedHashSet<>();
        gs.add(g);
        r.setAccess(gs);
    }

    protected void makeReferencePublic(Reference r) {
        r.publicDomain = true;
        LinkedHashSet<Group> emptyGroup = new LinkedHashSet<>();
        r.setAccess(emptyGroup);
    }

    public LinkedHashMap<Integer, String> getAlwaysPublic() {
        return alwaysPublic;
    }

    public void setAlwaysPublic(LinkedHashMap<Integer, String> alwaysPublic) {
        this.alwaysPublic = alwaysPublic;
    }

    public LinkedHashMap<Integer, String> getAlwaysPrivate() {
        return alwaysPrivate;
    }

    public void setAlwaysPrivate(LinkedHashMap<Integer, String> alwaysPrivate) {
        this.alwaysPrivate = alwaysPrivate;
    }

    public LinkedHashMap<Integer, Pattern> getReferenceCitationPatterns() {
        return referenceCitationPatterns;
    }

    public void setReferenceCitationPatterns(LinkedHashMap<Integer, Pattern> referenceCitationPatterns) {
        this.referenceCitationPatterns  = referenceCitationPatterns;
    }
    
    public LinkedHashMap<Integer, String> getSuggestedPublic() {
        return suggestedPublic;
    }

    public void setSuggestedPublic(LinkedHashMap<Integer, String> suggestedPublic) {
        this.suggestedPublic = suggestedPublic;
    }

    private boolean substanceNotesContainWarning(Substance s, String warningText) {
     
        log.trace("substanceNotesContainWarning looking for warning " + warningText);
        String textToSearch = "[Validation]WARNING:" + warningText;
        if( s.notes.stream().anyMatch(n->n.note.equals(textToSearch))) {
            log.trace("  going to return true");
            return true;
        }
        log.trace("  going to return false");
        return false;
    }
}
