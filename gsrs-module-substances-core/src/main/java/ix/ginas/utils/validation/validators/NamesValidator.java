package ix.ginas.utils.validation.validators;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.module.substance.repository.ReferenceRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.models.Keyword;
import ix.core.util.LogUtil;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.ValidationUtils;
import ix.ginas.utils.validation.validators.tags.TagUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.swing.text.html.HTML;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by katzelda on 5/11/18.
 */
@Slf4j
public class NamesValidator extends AbstractValidatorPlugin<Substance> {

    @Autowired
    private ReferenceRepository referenceRepository;
    @Autowired
    private SubstanceRepository substanceRepository;
    // Currently, this is false at FDA; it maybe confusing if used together with TagsValidator.
    boolean extractLocators = false;

    // Keep consistent with NamesUtilities
    // This and other replacers should be handled later in a new NameStandardizer class similar to HTMLNameStandardizer
    private static final String PATTERN_SINGLE_LINEFEED_PRECEDED_CERTAIN_CHARACTERS = "(?<=[\\-])[ \\t]*[\\r\\n]+[\\s]*";
    private static boolean replaceSingleLinefeedPrecededByCertainCharactersWithBlank = true;

    public static CachedSupplier<List<Replacer>> replacers = CachedSupplier.of(()->{
        List<Replacer> repList = new ArrayList<>();
        if(replaceSingleLinefeedPrecededByCertainCharactersWithBlank) {
            repList.add(new Replacer(PATTERN_SINGLE_LINEFEED_PRECEDED_CERTAIN_CHARACTERS, "").message("Name \"$0\" has a linefeed preceded by select characters. Whitespace around select characters removed."));
        }
        repList.add(new Replacer("^(\\s+)","" ).message("Name \"$0\" has leading whitespace which was removed"));
        repList.add(new Replacer("(\\s+)$","" ).message("Name \"$0\" has trailing whitespace which was removed"));
        repList.add(new Replacer("[\\t\\n\\r]", " ")
                .message("Name \"$0\" has non-space whitespace characters. They will be replaced with spaces."));
        repList.add(new Replacer("\\s\\s\\s*", " ")
                .message("Name \"$0\" has consecutive whitespace characters. These will be replaced with single spaces."));

        return repList;

    });

    public ReferenceRepository getReferenceRepository() {
        return referenceRepository;
    }

    public void setReferenceRepository(ReferenceRepository referenceRepository) {
        this.referenceRepository = referenceRepository;
    }

    public SubstanceRepository getSubstanceRepository() {
        return substanceRepository;
    }

    public void setSubstanceRepository(SubstanceRepository substanceRepository) {
        this.substanceRepository = substanceRepository;
    }

    private static String CHANGE_REASON_DISPLAYNAME_CHANGED ="Changed Display Name";

    public static class Replacer{
        Pattern p;
        String replace;
        String message = "String \"$0\" matches forbidden pattern";

        public Replacer(String regex, String replace){
            this.p=Pattern.compile(regex);
            this.replace=replace;
        }

        public boolean matches(String test){
            return this.p.matcher(test).find();
        }
        public String fix(String test){
            return test.replaceAll(p.pattern(), replace);
        }

        public Replacer message(String msg){
            this.message=msg;
            return this;
        }

        public String getMessage(String test){
            return message.replace("$0", test);
        }

    }
    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        boolean preferred = false;
        int display = 0;
        Iterator<Name> nameIterator = s.names.iterator();
        while(nameIterator.hasNext()){
            Name n = nameIterator.next();
            if (n == null) {
                GinasProcessingMessage mes = GinasProcessingMessage
                        .WARNING_MESSAGE("Null name objects are not allowed")
                        .appliableChange(true);

                callback.addMessage(mes, () -> {
                    mes.appliedChange = true;
                    nameIterator.remove();
                });

                continue;

            }
            if(n.getName() ==null){
                callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE("name can not be null"));
                continue;
            }
            if (n.preferred) {
                preferred = true;
            }
            if (n.isDisplayName()) {
                display++;
            }

            if (extractLocators) {
                // This is not being used in the FDA GSRS implementation (2022)
                // At one point and maybe still it was/is used in the NCATS public version.
                // This has to be done at the name level rather than the substance level
                // because bracketted tag terms are removed from the Name.name string in the process.
                // The warning may not be sufficiently explanatory to the user as he/she is not
                // shown which name(s) have been changed in the warning.
                TagUtilities.BracketExtraction be = TagUtilities.getBracketExtraction(n.getName());
                List<String> locators = be.getTagTerms();
                if(!locators.isEmpty()){
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE(
                                    "Names of form \"<NAME> [<TEXT>]\" are transformed to locators. The following locators will be added:%s",
                                            locators.toString())
                            .appliableChange(true);
                    callback.addMessage(mes, ()->{
                        for (String loc : locators) {
                            // Name is changed to just the namePart!
                            n.name = be.getNamePart();
                        }
                        for (String loc : locators) {
                            n.addLocator(s, loc);
                        }
                    });
                }
            }

            if (n.languages == null || n.languages.isEmpty()) {
                GinasProcessingMessage mes = GinasProcessingMessage
                        .WARNING_MESSAGE(
                                "Must specify a language for each name. Defaults to \"English\"")
                        .appliableChange(true);
                callback.addMessage(mes, () -> {
                    if (n.languages == null) {
                        n.languages = new EmbeddedKeywordList();
                    }
                    n.languages.add(new Keyword("en"));
                });
            }
            if (n.type == null) {
                GinasProcessingMessage mes = GinasProcessingMessage
                        .WARNING_MESSAGE(
                                "Must specify a name type for each name. Defaults to \"Common Name\" (cn)")
                        .appliableChange(true);
                callback.addMessage(mes, () -> n.type = "cn");

            }

            for (Replacer r : replacers.get()) {
                //check for Null
                String name = n.getName();
                if(name!=null && r.matches(name)) {
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE(
                                    r.getMessage(name))
                            .appliableChange(true);
                    callback.addMessage(mes, () -> n.setName(r.fix(name)));

                }
            }
            if(n.getAccess().isEmpty()){
                boolean hasPublicReference = n.getReferences().stream()
                        .map(r->r.getValue())
                        .map(r->s.getReferenceByUUID(r))
                        .filter(Objects::nonNull)
                        .filter(r->r.isPublic())
                        .filter(r->r.isPublicDomain())
                        .findAny()
                        .isPresent();

                if(!hasPublicReference){
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .ERROR_MESSAGE("The name :\"%s\" needs an unprotected reference marked \"Public Domain\" in order to be made public.",
                                    n.getName());
                    callback.addMessage(mes);
                }
            }


            ValidationUtils.validateReference(s, n, callback, ValidationUtils.ReferenceAction.FAIL, referenceRepository);
        }

        if (s.names.isEmpty()) {
            GinasProcessingMessage mes = GinasProcessingMessage
                    .ERROR_MESSAGE("Substances must have names");
            callback.addMessage(mes);
        }
        if (display == 0) {
            GinasProcessingMessage mes = GinasProcessingMessage
                    .INFO_MESSAGE("Substances should have exactly one (1) display name, Default to using:%s", s.getName())
                    .appliableChange(true);
            callback.addMessage(mes, () -> {
                if (!s.names.isEmpty()) {
                    Name.sortNames(s.names);
                    s.names.get(0).displayName = true;
                    mes.appliedChange = true;
                }
            });
        }
        if (display > 1) {
            GinasProcessingMessage mes = GinasProcessingMessage
                    .ERROR_MESSAGE("Substance should not have more than one (1) display name. Found %s", display);
            callback.addMessage(mes);
        }

        Map<String, Set<String>> nameSetByLanguage = new HashMap<>();

        Optional<Name> oldDisplayName= (objold!=null && objold.names !=null) ? objold.names.stream().filter(n->n!=null && n.displayName).findFirst() : Optional.empty();
        LogUtil.trace(()->String.format("oldDisplayName: present: %b; value: %s", oldDisplayName.isPresent(),
                oldDisplayName.isPresent() ? oldDisplayName.get().getName() : ""));

        for (Name n : s.names) {
            if( n==null || n.getName() == null) {
                //skip over null names
                continue;
            }
            String name = n.getName();
            Iterator<Keyword> iter = n.languages.iterator();
            String uppercasedName = name.toUpperCase();

            while(iter.hasNext()){
                String language = iter.next().getValue();
//				System.out.println("language for " + n + "  = " + language);
                Set<String> names = nameSetByLanguage.computeIfAbsent(language, k->new HashSet<>());
                if(!names.add(uppercasedName)){
                    GinasProcessingMessage mes;
                    mes = GinasProcessingMessage
                                .WARNING_MESSAGE("Name '%s' is a duplicate name in the record.", name)
                                .markPossibleDuplicate();
                    callback.addMessage(mes);
                }

            }
            //nameSet.add(n.getName());
            try {
                List<SubstanceRepository.SubstanceSummary> sr = substanceRepository.findByNames_NameIgnoreCase(n.name);
                if (sr != null && !sr.isEmpty()) {
                    SubstanceRepository.SubstanceSummary s2 = sr.iterator().next();
                    if (!s2.getUuid().equals(s.getOrGenerateUUID())) {
                        GinasProcessingMessage mes = GinasProcessingMessage
                                .WARNING_MESSAGE("Name '%s' collides (possible duplicate) with existing name for substance:", n.name)
                               //TODO katzelda Feb 2021: add link back
                                . addLink(ValidationUtils.createSubstanceLink(s2.toSubstanceReference()))
                                ;
                        callback.addMessage(mes);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(oldDisplayName.isPresent() && n.displayName && !oldDisplayName.get()..getName().equalsIgnoreCase(n.getName())
                &&  (s.changeReason==null || !s.changeReason.equalsIgnoreCase(CHANGE_REASON_DISPLAYNAME_CHANGED))) {
                GinasProcessingMessage mes = GinasProcessingMessage
                        .WARNING_MESSAGE(
                                "Preferred Name has been changed from '%s' to '%s'. Please confirm that this change is intentional by submitting.",
                                oldDisplayName.get().getName(), n.getName());
                callback.addMessage(mes);
            }
        }

    }

    public void setReplaceSingleLinefeedPrecededByCertainCharactersWithBlank(boolean replaceSingleLinefeedPrecededByCertainCharactersWithBlank) {
        this.replaceSingleLinefeedPrecededByCertainCharactersWithBlank = replaceSingleLinefeedPrecededByCertainCharactersWithBlank;
    }

}
