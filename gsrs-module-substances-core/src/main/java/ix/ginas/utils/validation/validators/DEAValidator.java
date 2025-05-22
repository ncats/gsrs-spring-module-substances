package ix.ginas.utils.validation.validators;

import gsrs.module.substance.utils.DEADataTable;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DEAValidator extends AbstractValidatorPlugin<Substance> {

    public final static String DEA_NUMBER_CODE_SYSTEM = "DEA Number";
    //private String deaScheduleFileName;
    private String deaNumberFileName;

    private DEADataTable deaDataTable;
    
    // There are two DEAValidators in substances module. Use DEAValidator1 here.    
    private final String DEAValidator1ScheduleWarning = "DEAValidator1ScheduleWarning";

    @Override
    public void validate(Substance substanceNew, Substance substanceOld, ValidatorCallback callback) {
        if(deaDataTable == null) {
            deaDataTable  = new DEADataTable(deaNumberFileName);
        }
        if( !(substanceNew instanceof ChemicalSubstance)) {
            return;
        }
        ChemicalSubstance chemical = (ChemicalSubstance) substanceNew;
        String deaNumber=deaDataTable.getDeaNumberForChemical(chemical);
        String deaSchedule = deaDataTable.getDeaScheduleForChemical(chemical);
        if( deaNumber !=null) {
            GinasProcessingMessage mesWarn = GinasProcessingMessage
                    .WARNING_MESSAGE(DEAValidator1ScheduleWarning, "This substance has DEA schedule: " + deaSchedule);
            if( deaDataTable.assignCodeForDea(substanceNew, deaNumber)){
                mesWarn.appliableChange(true);
            }
            callback.addMessage(mesWarn);
        }
    }

    public String getDeaNumberFileName() {
        return deaNumberFileName;
    }

    public void setDeaNumberFileName(String deaNumberFileName) {
        this.deaNumberFileName = deaNumberFileName;
    }

}
