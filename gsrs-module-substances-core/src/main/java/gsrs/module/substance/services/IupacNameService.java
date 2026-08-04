package gsrs.module.substance.services;

import ix.core.models.PubChemResolutionResult;
import ix.ginas.models.v1.*;
import ix.ncats.resolvers.PubChemNameListResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;

@Slf4j
public class IupacNameService {
    @Autowired
    private PubChemNameListResolver resolver;

    public final static String PUBCHEM_REFERENCE_TYPE = "PUBCHEM";
    private final static String IUPAC_NAME_LANGUAGE = "en";
    private final static String PUBCHEM_CODE_SYSTEM = "PUBCHEM";
    private final static String PUBCHEM_CODE_TYPE = "PRIMARY";

    public boolean ensureIupacName(ChemicalSubstance substance, String nameType, String nameLanguage)
            throws IOException, InterruptedException {
        List<String> inchiKeys = substance.getStructure().getInChIKeysAndThrow();
        if( inchiKeys== null || inchiKeys.size()==0) {
            log.warn("No InChIKey found for input");
            return false;
        }
        if(inchiKeys.size() >= 2) {
            log.warn("More than one InChIKey found for input");
            return false;
        }
        PubChemResolutionResult result= resolver.getDataForChemical(inchiKeys.get(0));
        String iupacName = result.getIupacName();
        if(iupacName == null || iupacName.length() ==0) {
            log.info("no IUPAC name found for this substance");
            return false;
        }
        boolean foundName = substance.names.stream()
                .anyMatch(n->n.name.equalsIgnoreCase(iupacName));
        if( foundName ) {
            log.info("substance already had IUPAC name {}", iupacName);
            return false;
        }
        Name iupacNameObject = new Name();
        iupacNameObject.name = iupacName;
        iupacNameObject.type = nameType;
        iupacNameObject.addLanguage(nameLanguage);
        iupacNameObject.assignOwner(substance);
        Reference newReference = findOrCreatePubchemReference(substance);
        iupacNameObject.addReference(newReference);
        substance.references.add(newReference);
        newReference.setOwner(substance);
        substance.names.add(iupacNameObject);
        boolean foundCode = substance.codes.stream()
                .anyMatch(c->c.code.equalsIgnoreCase(result.getCid()) && c.codeSystem.equalsIgnoreCase("pubchem"));
        if(!foundCode) {
            Code cidCode = new Code();
            cidCode.codeSystem= PUBCHEM_CODE_SYSTEM;
            cidCode.code = result.getCid();
            cidCode.type=PUBCHEM_CODE_TYPE;
            cidCode.setOwner(substance);
            substance.codes.add(cidCode);
        } else {
            log.info("Substance already has a PUBCHEM code");
        }

        return true;
    }

    public Reference findOrCreatePubchemReference(Substance substance) {
        Reference pubchemReference= substance.references.stream()
                .filter(r->r.docType.equalsIgnoreCase(PUBCHEM_REFERENCE_TYPE))
                .findFirst().orElse(null);
        if( pubchemReference ==null) {
            pubchemReference = new Reference();
            pubchemReference.docType = PUBCHEM_REFERENCE_TYPE;
            pubchemReference.citation = PUBCHEM_REFERENCE_TYPE;
            pubchemReference.publicDomain = true;
        }
        return pubchemReference;
    }
}
