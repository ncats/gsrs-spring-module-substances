package ix.ginas.utils.validation.validators;


import gsrs.module.substance.repository.ReferenceRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.models.Keyword;
import ix.core.util.LogUtil;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by katzelda on 5/14/18.
 */
@Slf4j
public class CodesValidator extends AbstractValidatorPlugin<Substance> {

    
    @Autowired
    private ReferenceRepository referenceRepository;


    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        log.trace("starting in validate. " );
        Iterator<Code> codesIter = s.codes.iterator();
        while(codesIter.hasNext()){
            Code cd = codesIter.next();
            if (cd == null) {
                GinasProcessingMessage mes = GinasProcessingMessage
                        .WARNING_MESSAGE("Null code objects are not allowed")
                        .appliableChange(true);
                callback.addMessage(mes, ()->codesIter.remove());
                continue;
            }

                if (ValidationUtils.isEffectivelyNull(cd.code)) {
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .ERROR_MESSAGE(
                                    "'Code' should not be null in code objects")
                            .appliableChange(true);
                    callback.addMessage(mes, ()-> cd.code="<no code>");

                }else if (!(cd.code+"").trim().equals(cd.code+"")) {
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE(
                                    "'Code' '" + cd.code + "' should not have trailing or leading whitespace. Code will be trimmed to '" + cd.code.trim() + "'")
                            .appliableChange(true);
                    callback.addMessage(mes, ()-> cd.code=(cd.code+"").trim());

                }
                
                if (!ValidationUtils.isEffectivelyNull(cd.codeText) && !(cd.codeText+"").trim().equals(cd.codeText+"")) {
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE(
                                    "'Code comment' '" + cd.codeText + "' should not have trailing or leading whitespace. Code will be trimmed to '" 
                                            + cd.codeText.trim() + "'")
                            .appliableChange(true);
                    callback.addMessage(mes, ()-> cd.codeText=(cd.codeText+"").trim());
                }

                
            if (ValidationUtils.isEffectivelyNull(cd.codeSystem)) {
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .ERROR_MESSAGE(
                                    "'Code System' should not be null in code objects")
                            .appliableChange(true);
                    callback.addMessage(mes, ()->cd.codeSystem="<no system>");

                } else if (!(cd.codeSystem+"").trim().equals(cd.codeSystem+"")) {
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE(
                                    "'Code system' '" + cd.codeSystem + "' should not have trailing or leading whitespace. Code will be trimmed to '" 
                                            + cd.codeSystem.trim() + "'")
                            .appliableChange(true);
                    callback.addMessage(mes, ()-> cd.codeSystem=(cd.codeSystem+"").trim());
                }

                if (ValidationUtils.isEffectivelyNull(cd.type)) {
                    GinasProcessingMessage mes = GinasProcessingMessage
                            .WARNING_MESSAGE(
                                    "Must specify a code type for each name. Defaults to \"PRIMARY\" (PRIMARY)")
                            .appliableChange(true);
                    callback.addMessage(mes, ()-> cd.type="PRIMARY");

                }


            if (!ValidationUtils.validateReference(s, cd, callback, ValidationUtils.ReferenceAction.ALLOW, referenceRepository)) {
                return;
            }

        }

        for (Code cd : s.codes) {
//            String debug = String.format("code system: %s, code: %s; comments: %s; type: %s", 
//                    cd.codeSystem, cd.code, cd.comments, cd.type);
//            log.trace(debug);
            
            try {
                 if( containsLeadingTrailingSpaces(cd.comments) ) {
                     cd.comments=cd.comments.trim();
                     GinasProcessingMessage mes = GinasProcessingMessage
                                .WARNING_MESSAGE(
                                        "Code '"
                                                + cd.code
                                                + "'[" +cd.codeSystem
                                                + "] "
                                        + "code text: " +  cd.comments  +" contains one or more leading/trailing blanks that will be removed")
                                .appliableChange(true);
                     callback.addMessage(mes);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static boolean containsLeadingTrailingSpaces(String comment) {
        if( comment==null || comment.length()==0){
            return false;
        }
        if( !comment.equals(comment.trim())){
            return true;
        }
        String[] lines = comment.split("\\|");;
        for(String line : lines) {
            if( line!=null && line.length()>0 && !line.equals(line.trim())) {
                return true;
            }
        }
        return false;
    }
}
