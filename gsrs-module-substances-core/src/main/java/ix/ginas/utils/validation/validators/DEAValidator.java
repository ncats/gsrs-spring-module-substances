package ix.ginas.utils.validation.validators;

import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class DEAValidator extends AbstractValidatorPlugin<Substance> {

    public final static String DEA_NUMBER_CODE_SYSTEM = "DEA Number";
    private Map<String, String> inchiKeyToDeaSchedule=null;
    private Map<String, String> inchiKeyToDeaNumber=null;
    //private String deaScheduleFileName;
    private String deaNumberFileName;

    public static void readFromFile(String filePath, LineProcessor eachLine) {
        log.trace("readFromFile using filePath " + filePath);
        FileInputStream fstream =null;
        try {
            fstream = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            if( fstream!=null) {
                DataInputStream in = new DataInputStream(fstream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String strLine;
                while ((strLine = br.readLine()) != null) {
                    if (!eachLine.process(strLine))
                        break;
                }
                br.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> getInchiKeyToDeaSchedule() {
        return inchiKeyToDeaSchedule;
    }

    public Map<String, String> getInchiKeyToDeaNumber() {
        return inchiKeyToDeaNumber;
    }

    public void initialize()  {
        log.trace(String.format("init DeaNumberFileName: %s", deaNumberFileName));
        inchiKeyToDeaNumber = new ConcurrentHashMap<>();
        inchiKeyToDeaSchedule= new ConcurrentHashMap<>();

        readFromFile(deaNumberFileName, new LineProcessor() {
            @Override
            public boolean process(final String line) {
                String[] tokens =line.split("\t");
                inchiKeyToDeaNumber.put(tokens[2].replace("InChIKey=", ""), tokens[0]);
                return true;
            }
        });

        readFromFile(deaNumberFileName, new LineProcessor() {
            @Override
            public boolean process(final String line) {
                String[] tokens =line.split("\t");
                inchiKeyToDeaSchedule.put(tokens[2].replace("InChIKey=", ""), tokens[12]);
                return true;
            }
        });
    }

    @Override
    public void validate(Substance substanceNew, Substance substanceOld, ValidatorCallback callback) {
        if( !(substanceNew instanceof ChemicalSubstance)) {
            return;
        }
        ChemicalSubstance chemical = (ChemicalSubstance) substanceNew;
        String deaNumber=getDeaNumberForChemical(chemical);
        String deaSchedule = getDeaScheduleForChemical(chemical);
        if( deaNumber !=null) {
            GinasProcessingMessage mesWarn = GinasProcessingMessage
                    .WARNING_MESSAGE(
                            String.format("This substance has DEA schedule: %s", deaSchedule));
            if( assignCodeForDea(substanceNew, deaNumber)){
                mesWarn.appliableChange(true);
            }
            callback.addMessage(mesWarn);
        }
    }

    public AtomicReference<Code> findFirstCode(Substance substance, String codeSystem) {
        AtomicReference<Code> matchingCode =new AtomicReference<>();
        substance.codes.forEach(cd->{
            if( cd.codeSystem.equals(codeSystem)) {
                matchingCode.set(cd);
            }
        });
        return matchingCode;
    }

    public String getDeaScheduleForChemical( ChemicalSubstance chemicalSubstance) {
        String inchiKey = chemicalSubstance.getStructure().getInChIKey();
        if( inchiKey== null || inchiKey.length()<=0) {
            log.warn("No InChIKey for structure!");
            return null;
        }
        String deaSchedule = inchiKeyToDeaSchedule.get(inchiKey);
        return deaSchedule;
    }

    public String getDeaNumberForChemical( ChemicalSubstance chemicalSubstance) {
        String inchiKey = chemicalSubstance.getStructure().getInChIKey();
        if( inchiKey== null || inchiKey.length()<=0) {
            log.warn("No InChIKey for structure!");
            return null;
        }
        String deaSchedule = inchiKeyToDeaNumber.get(inchiKey);
        return deaSchedule;
    }

    public boolean assignCodeForDea(Substance substance, String deaNumber) {
        AtomicReference<Code> deaNumberCodeRef = findFirstCode(substance,  DEA_NUMBER_CODE_SYSTEM);
        if( deaNumberCodeRef.get() != null) {
            deaNumberCodeRef.get().code=deaNumber;
            log.trace("assigned value to existing code");
            return false;
        }
        Code deaNumberCode = new Code();
        deaNumberCode.code=deaNumber;
        deaNumberCode.codeSystem= DEA_NUMBER_CODE_SYSTEM;
        deaNumberCode.type="PRIMARY";
        substance.addCode(deaNumberCode);
        log.trace("created new code");
        return true;
    }

    public String getDeaNumberFileName() {
        return deaNumberFileName;
    }

    public void setDeaNumberFileName(String deaNumberFileName) {
        this.deaNumberFileName = deaNumberFileName;
    }

}
