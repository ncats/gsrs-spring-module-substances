package gsrs.module.substance.utils;

import gov.nih.ncats.molwitch.Chemical;
import ix.core.models.Structure;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ChemicalSubstanceUtils {

    private static final String STRUCTURE_FIELD_NAME ="MOLFILE";
    private static final String FILE_REFERENCE_TYPE = "SDFile";
    private static final String ID_FIELD_NAME = "ID";
    private static final String MAIN_NAME_FIELD= "Sample Name";
    private static final String CAS_RN_FIELD= "CAS Number";
    private static final String SALT_CODE_FIELD= "Salt Code";
    private static final String SALT_EQUIV_FIELD= "Salt Equivalents";
    private static final String SUPPLIER_FIELD = "Supplier";
    private static final String SUPPLIER_CODE_FIELD = "Supplier Code";
    private static final String SD_FILE_DELIMITER= "$$$$";

    Set<String> importableFields = new HashSet<>(Arrays.asList(MAIN_NAME_FIELD, CAS_RN_FIELD, SALT_CODE_FIELD,
            SALT_EQUIV_FIELD, SUPPLIER_CODE_FIELD, SUPPLIER_FIELD));
    private static final Pattern sdFileFieldPattern = Pattern.compile("> +<(.*)>");
    private static final Pattern molfileEndPattern = Pattern.compile("M +END");

    public List<ChemicalSubstance> parseSdFile(String sdFilePath, Map<String, String> fieldMappings) throws IOException {
        List<ChemicalSubstance> chemicals = new ArrayList<>();

        FileInputStream fstream = new FileInputStream(sdFilePath);
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        int recordsRead=0;
        Map<String, String> fieldSet = new HashMap<>();
        String fieldName="";
        List<String> molfileLines = new ArrayList<>();
        List<String> dataLines =new ArrayList<>();
        boolean readingMol=true;
        String idFieldName;
        if( !fieldMappings.containsValue(ID_FIELD_NAME)) {
            idFieldName= fieldMappings.keySet().stream().filter(k->fieldMappings.get(k).equalsIgnoreCase(MAIN_NAME_FIELD)).findFirst().orElse("");
        } else {
            idFieldName= fieldMappings.keySet().stream().filter(k->fieldMappings.get(k).equalsIgnoreCase(ID_FIELD_NAME)).findFirst().orElse("");
        }
        log.debug("idFieldName: " + idFieldName);

        while((line =br.readLine()) != null) {
            if(line.startsWith(SD_FILE_DELIMITER)) {
                recordsRead++;
                molfileLines.clear();
                //need some ID
                fieldSet.put(ID_FIELD_NAME, fieldSet.get(idFieldName));
                ChemicalSubstance newChemical = makeChemicalSubstance(fieldSet);
                chemicals.add(newChemical);
                readingMol=true;
                fieldName="";
                dataLines.clear();
                fieldSet.clear();
            } else if( readingMol) {
                molfileLines.add(line);
                if( molfileEndPattern.matcher(line).find()) {
                    readingMol=false;
                    fieldSet.put(STRUCTURE_FIELD_NAME, String.join("\n", molfileLines));
                    molfileLines.clear();
                }
            }
            else {
                Matcher matcher = sdFileFieldPattern.matcher(line);
                if(matcher.find()) {
                    fieldName= matcher.group(1);
                } else {
                    //data lines
                    if(line.length()==0 ) {
                        if( fieldName.length()>0 && fieldMappings.containsKey(fieldName)) {
                            fieldSet.put(fieldMappings.get(fieldName), String.join("\\n", dataLines));
                        }
                        dataLines.clear();
                        fieldName="";
                    } else {
                        dataLines.add(line);
                    }
                }
            }
        }
        return chemicals;
    }

    private ChemicalSubstance makeChemicalSubstance(Map<String, String> fields){
        if( !fields.containsKey(STRUCTURE_FIELD_NAME) || !fields.containsKey(ID_FIELD_NAME)
            || !fields.containsKey(MAIN_NAME_FIELD)) {
            log.warn("makeChemicalSubstance skipping record because required fields are missing");
            return null;
        }
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile=fields.get(STRUCTURE_FIELD_NAME);
        builder.setStructure(structure);

        Reference nameReference = createNameReference( fields.get(ID_FIELD_NAME));
        Name mainName = new Name();
        mainName.displayName=true;
        mainName.name=fields.get(MAIN_NAME_FIELD);
        mainName.addReference(nameReference);
        builder.addName(mainName);
        if(fields.get(CAS_RN_FIELD) != null) {
            Code casCode = new Code();
            casCode.code=fields.get(CAS_RN_FIELD);
            casCode.codeSystem="CAS";
            casCode.type="PRIMARY";
            builder.addCode(casCode);
        }
        if( fields.get(SALT_CODE_FIELD) !=null ) {
            Code saltCode = new Code();
            saltCode.type="GENERIC (FAMILY)";
            saltCode.codeSystem=SALT_CODE_FIELD;
            saltCode.code=fields.get(SALT_CODE_FIELD);
            builder.addCode(saltCode);
        }
        if( fields.get(SALT_EQUIV_FIELD) !=null ) {
            Code saltEquivCode = new Code();
            saltEquivCode.type="GENERIC (FAMILY)";
            saltEquivCode.code=fields.get(SALT_EQUIV_FIELD);
            saltEquivCode.codeSystem=SALT_EQUIV_FIELD;
            builder.addCode(saltEquivCode);
        }
        if( fields.get(SUPPLIER_FIELD) !=null ) {
            Code supplierCode = new Code();
            supplierCode.type="GENERIC (FAMILY)";
            supplierCode.code=fields.get(SUPPLIER_FIELD);
            supplierCode.codeSystem=SUPPLIER_FIELD;
            builder.addCode(supplierCode);
        }
        if( fields.get(SUPPLIER_CODE_FIELD) !=null ) {
            Code supplierIDCode = new Code();
            supplierIDCode.type="GENERIC (FAMILY)";
            supplierIDCode.code=fields.get(SUPPLIER_CODE_FIELD);
            supplierIDCode.codeSystem=SUPPLIER_CODE_FIELD;
            builder.addCode(supplierIDCode);
        }

        return builder.build();
    }

     private Reference createNameReference(String id) {
        Reference reference = new Reference();
        reference.docType= FILE_REFERENCE_TYPE;
        reference.citation=id;
        reference.uuid= UUID.randomUUID();
        return reference;
    }
}
