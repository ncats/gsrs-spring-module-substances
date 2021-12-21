package ix.ginas.utils.validation.validators;

import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class DEAValidator extends AbstractValidatorPlugin<Substance> {

    public DEAValidator() {
        try {
            init();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    public final static String DEA_NUMBER_CODE_SYSTEM = "DEA Number";

    public Map<String, String> getInchiKeyToDeaSchedule() {
        return inchiKeyToDeaSchedule;
    }

    public Map<String, String> getInchiKeyToDeaNumber() {
        return inchiKeyToDeaNumber;
    }

    private Map<String, String> inchiKeyToDeaSchedule=null;
    private Map<String, String> inchiKeyToDeaNumber=null;

    private String DeaScheduleFileName = "/src/main/resources/DEA_SCHED_LIST.txt";
    private String DeaNumberFileName= "/src/main/resources/DEA_LIST.txt";

    public void init() throws IOException {
        String currentPath = Paths.get(".").toAbsolutePath().normalize().toString();
        log.debug("currentPath: " + currentPath);
        String deaScheduleFileName = currentPath + DeaScheduleFileName;
        String deaNumberFileName= currentPath + DeaNumberFileName;
        inchiKeyToDeaNumber = new ConcurrentHashMap<>();
        inchiKeyToDeaSchedule= new ConcurrentHashMap<>();

        //File deaNumberFile = new ClassPathResource(DeaNumberFileName).getFile();
        readFromFile(deaNumberFileName, new LineProcessor() {
            @Override
            public boolean process(final String line) {
                String[] tokens =line.split("\t");
                inchiKeyToDeaNumber.put(tokens[2].replace("InChIKey=", ""), tokens[0]);
                return true;
            }
        });

        //File deaScheduleFile = new ClassPathResource(DeaScheduleFileName).getFile();
        readFromFile(deaScheduleFileName, new LineProcessor() {
            @Override
            public boolean process(final String line) {
                String[] tokens =line.split("\t");
                inchiKeyToDeaSchedule.put(tokens[2].replace("InChIKey=", ""), tokens[0]);
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
        String inchiKey = chemical.getStructure().getInChIKey();
        if( inchiKeyToDeaSchedule.containsKey(inchiKey)) {
            String deaSchedule = inchiKeyToDeaSchedule.get(inchiKey);
            GinasProcessingMessage mesWarn = GinasProcessingMessage
                    .WARNING_MESSAGE(
                            String.format("This substance has DEA schedule: %s", deaSchedule));

            String deaNumber = inchiKeyToDeaNumber.get(inchiKey);
            if( deaNumber!=null ) {
                AtomicReference<Code> deaNumberCodeRef = findFirstCode(substanceNew,  DEA_NUMBER_CODE_SYSTEM);
                if( deaNumberCodeRef.get() != null) {
                    deaNumberCodeRef.get().code=deaNumber;
                    log.trace("assigned value to existing code");
                }
                else {
                    Code deaNumberCode = new Code();
                    deaNumberCode.code=deaNumber;
                    deaNumberCode.codeSystem= DEA_NUMBER_CODE_SYSTEM;
                    deaNumberCode.type="PRIMARY";
                    chemical.addCode(deaNumberCode);
                    log.trace("created new code");
                }
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
    public String getDeaScheduleFileName() {
        return DeaScheduleFileName;
    }

    public void setDeaScheduleFileName(String deaScheduleFileName) {
        DeaScheduleFileName = deaScheduleFileName;
    }

    public String getDeaNumberFileName() {
        return DeaNumberFileName;
    }

    public void setDeaNumberFileName(String deaNumberFileName) {
        DeaNumberFileName = deaNumberFileName;
    }

}
