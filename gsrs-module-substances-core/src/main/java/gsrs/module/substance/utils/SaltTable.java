package gsrs.module.substance.utils;

import gov.nih.ncats.molwitch.Chemical;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.utils.validation.validators.LineProcessor;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;

@Slf4j
public class SaltTable {

    private String saltFilePath;

    public SaltTable(String saltFilePath) {
        this.saltFilePath=saltFilePath;
    }

    public void initialize() {
        log.trace("in initialize");

        initSalts(saltFilePath);
        log.trace("saltInChiKeyToName size: " + saltInChiKeyToName.size());
    }


    private void initSalts(String saltFileFullPath) {
        saltInChiKeyToName = new HashMap<>();
        readFromFile(saltFilePath, new LineProcessor() {
            @Override
            public boolean process(final String line) {
                String[] tokens = line.split("\t");
                if (tokens[0] != null && tokens[0].length() > 0) {
                    String inChIKey = tokens[0];
                    if (inChIKey.contains("=")) {
                        inChIKey = inChIKey.split("=")[1];
                    }
                    saltInChiKeyToName.put(inChIKey, tokens[1]);
                }

                return true;
            }
        });
    }


    private Map<String, String> saltInChiKeyToName = null;

    public String getSaltFilePath() {
        return saltFilePath;
    }

    public static void readFromFile(String filePath, LineProcessor eachLine) {
        FileInputStream fstream = null;
        try {
            fstream = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            if (fstream != null) {
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

    public List<String> getSalts(ChemicalSubstance chemical) {
        List<String> salts = new ArrayList<>();
        Iterable<Chemical> fragments = chemical.getStructure().toChemical().getConnectedComponents();
        for (Chemical c : fragments) {
            try {
                String inChIKey = c.toInchi().getKey();
                if (saltInChiKeyToName.get(inChIKey) != null) {
                    salts.add(saltInChiKeyToName.get(inChIKey));
                }
            } catch (IOException e) {
                log.error("Error calculating InChIKey");
                e.printStackTrace();
            }
        }
        return salts;
    }

    public Chemical removeSalts(ChemicalSubstance chemical) {
        Chemical cleanMol = chemical.getStructure().toChemical().copy();
        cleanMol.setAtomMapToPosition();
        Iterable<Chemical> fragments = cleanMol.getConnectedComponents();
        for (Chemical c : fragments) {
            try {

                String inChIKey = c.toInchi().getKey();
                if (saltInChiKeyToName.get(inChIKey) != null) {
                    log.trace("will remove atoms in " + c.toSmiles());
                    c.getAtoms().forEach(a -> cleanMol.removeAtom(a));
                }
            } catch (IOException e) {
                log.error("Error calculating InChIKey");
                e.printStackTrace();
            }
        }
        return cleanMol;
    }


    public void setSaltInChiKeyToName(Map<String, String> saltInChiKeyToName) {
        this.saltInChiKeyToName = saltInChiKeyToName;
    }

    public Map<String, String> getSaltInChiKeyToName() {
        return saltInChiKeyToName;
    }


}
