package ix.ginas.utils.validation.validators;

import gsrs.cv.api.CodeSystemTermDTO;
import gsrs.cv.api.ControlledVocabularyApi;
import gsrs.cv.api.GsrsCodeSystemControlledVocabularyDTO;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Code;

import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Mitch Miller
 */
@Slf4j
public class CodeFormatValidator extends AbstractValidatorPlugin<Substance>
{

    @Autowired
    private ControlledVocabularyApi cvApi;

    @Override
    public void validate(Substance s, Substance oldSubstance, ValidatorCallback callback)
    {
        log.trace("CodeFormatValidator.validate");
        Map<String, List<Code>> codesBySystem = s.getCodes().stream()
								.filter(c-> c.codeSystem != null)
                .collect(Collectors.groupingBy(c-> c.codeSystem,
                        Collectors.toList()));


        try {
            Optional<GsrsCodeSystemControlledVocabularyDTO> cvv = cvApi.findByDomain("CODE_SYSTEM");

            for(CodeSystemTermDTO vt1 : cvv.get().getTerms()){

                String codeSystemRegex = vt1.getRegex();
                //codes will be null if does not exist in map
                List<Code> codes =  codesBySystem.get(vt1.getValue());
                if(codeSystemRegex != null && !codeSystemRegex.isEmpty() && codes !=null){
                    Pattern codePattern = Pattern.compile(codeSystemRegex);
                    for(Code c : codes){
                        if(c ==null){
                            continue;
                        }
                        String codeToCheck = c.getCode();
                        if(codeToCheck ==null){
                            continue;
                        }
                        Matcher matcher = codePattern.matcher(c.getCode());
                        //find ? or matches?
                        if(!matcher.find()){
                            callback.addMessage(GinasProcessingMessage
                                    .WARNING_MESSAGE(String.format("Code %s does not match pattern %s for system %s", c.getCode(), codeSystemRegex, vt1.getValue())));
                        }
                    }
                }
            }
        } catch (IOException e) {
            callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE("could not validate CV"));
        }
    }

}
