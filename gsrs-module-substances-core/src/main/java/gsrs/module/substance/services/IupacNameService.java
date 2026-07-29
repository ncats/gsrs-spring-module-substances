package gsrs.module.substance.services;

import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ncats.resolvers.PubChemNameListResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@Slf4j
public class IupacNameService {
    @Autowired
    private PubChemNameListResolver resolver;

    public final static String PUBCHEM_REFERENCE_TYPE = "PUBCHEM";
    private final static String IUPAC_NAME_LANGUAGE = "en";

    public boolean ensureIupacName(ChemicalSubstance substance, String nameType, String nameLanguage)
            throws IOException, InterruptedException {
        String iupacName = resolver.getNamesData(substance.getStructure().smiles);
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
