package fda.gsrs.substance.validators;

import fda.gsrs.substance.FdaSrsUtil;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;

import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BdNumModificationValidator extends AbstractValidatorPlugin<Substance> {

//    @Override
//    public boolean supports(Substance newValue, Substance oldValue, LoadValidatorInitializer.ValidatorConfig.METHOD_TYPE methodType) {
//        return methodType == LoadValidatorInitializer.ValidatorConfig.METHOD_TYPE.UPDATE;
//    }
	
	private final String BdNumModificationValidatorRemoveError = "BdNumModificationValidatorRemoveError";
	private final String BdNumModificationValidatorRemoveWarning = "BdNumModificationValidatorRemoveWarning";
	private final String BdNumModificationValidatorPrimaryError = "BdNumModificationValidatorPrimaryError";
	
    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        if(objold == null){
            //new substance ignore
            return;
        }

        Map<String, Set<String>> oldBdnums= FdaSrsUtil.getBdNumCode(objold)
                .collect(Collectors.groupingBy(c-> c.type, Collectors.mapping(Code::getCode, Collectors.toSet())));

        if(oldBdnums.isEmpty()){
            //no old bdnum ignore
            return;
        }

        /*
        So, we want 3 validation rule parts:
1. If the BDNUM literal that existed in the old record (e.g. XXXXXX) no longer exists in the current record, throw an error unless that BDNUM was marked as type "superseded".

2. If there are 2 or more BDNUMs where both are type "PRIMARY", throw an error.
3. If a BDNUM code is removed, for any reason, throw a warning
So, you should check the BDNUMs not by monitoring the code object itself
 (though you could) but by making a list of tuples of BDNUM (string) + TYPE (string) before the change,
 and one after, and seeing how those two lists have changed
         */
        //there is usually only 1 BDNUM which is the PRIMARY
        //but there may be others that are superseded
        Map<String, Set<String>> currentBdnums= FdaSrsUtil.getBdNumCode(s)
                                            .collect(Collectors.groupingBy(c-> c.type, Collectors.mapping(Code::getCode, Collectors.toSet())));

        Set<String> allCurrentBdNums = new HashSet<>();
        for(Set<String> entry : currentBdnums.values()){
            allCurrentBdNums.addAll(entry);
        }
        for(Map.Entry<String, Set<String>> entry : oldBdnums.entrySet()){
            String bdnumType = entry.getKey();
            for(String bd : entry.getValue()){
                //3. If a BDNUM code is removed, for any reason, throw a warning
                if(!allCurrentBdNums.contains(bd)){
                    if(! ("SUPERSEDED".equalsIgnoreCase(bdnumType) || "SUPERCEDED".equalsIgnoreCase(bdnumType))){
                        //1. If the BDNUM literal that existed in the old record (e.g. XXXXXX) no longer exists in the current record, throw an error unless that BDNUM was marked as type "superseded".
                        callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(BdNumModificationValidatorRemoveError, 
                        		bdnumType + " BDNUM " + bd + " has been removed, but only SUPERSEDED BDNUM codes may be removed"));

                    }else {
                        callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(BdNumModificationValidatorRemoveWarning, 
                        		bdnumType+ " BDNUM " + bd + " has been removed"));
                    }
                }
            }
        }

        Set<String> primaries = currentBdnums.get("PRIMARY");
        //primaries could be null if we haven't added one yet
        //or if it has been removed (in which case we alrady added an error above for that)
        if(primaries !=null && primaries.size() > 1){
           //2. If there are 2 or more BDNUMs where both are type "PRIMARY", throw an error.
           callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(BdNumModificationValidatorPrimaryError,
        		   "Cannot have more than 1 PRIMARY BDNUM but have : " + primaries));
        }

    }
}
