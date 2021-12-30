package gsrs.module.substance.utils;

import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Note;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.LineProcessor;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class DEADataTable {

    public DEADataTable(String deaNumberFileName ) {
        this.deaNumberFileName =deaNumberFileName;
        initialize();
    }
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

        readFromFile(deaNumberFileName, line -> {
            String[] tokens =line.split("\t");
            inchiKeyToDeaNumber.put(tokens[2].replace("InChIKey=", ""), tokens[0]);
            return true;
        });

        readFromFile(deaNumberFileName, line -> {
            String[] tokens =line.split("\t");
            inchiKeyToDeaSchedule.put(tokens[2].replace("InChIKey=", ""), tokens[12]);
            return true;
        });
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
        return inchiKeyToDeaSchedule.get(inchiKey);
    }

    public String getDeaNumberForChemical( ChemicalSubstance chemicalSubstance) {
        String inchiKey = chemicalSubstance.getStructure().getInChIKey();
        if( inchiKey== null || inchiKey.length()<=0) {
            log.warn("No InChIKey for structure!");
            return null;
        }
        return inchiKeyToDeaNumber.get(inchiKey);
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

    public boolean assignNoteForDea(Substance substance, String deaSchedule) {
        String noteIntro="WARNING:This substance has DEA schedule:";
        boolean added =false;
        Note deaScheduleNote = null;
        for( Note n:  substance.notes){
            if( n.note.startsWith(noteIntro)) {
                deaScheduleNote =n;
            }
        }
        if(deaScheduleNote == null ) {
            deaScheduleNote=new Note();
            substance.notes.add(deaScheduleNote);
            added=true;
        }
        deaScheduleNote.note = noteIntro+ " " + deaSchedule;
        return added;
    }
    public String getDeaNumberFileName() {
        return deaNumberFileName;
    }

}
