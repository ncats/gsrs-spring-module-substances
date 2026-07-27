package gsrs.module.substance.services;

import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ncats.resolvers.PubChemNameListResolver;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class IupacNameService {
    private PubChemNameListResolver resolver = new PubChemNameListResolver();

    public final static String PUBCHEM_REFERENCE_TYPE = "PUBCHEM";

    public boolean ensureIupacName(ChemicalSubstance substance) throws IOException, InterruptedException {
        String iupacName = resolver.getNamesData(substance.getStructure().smiles);
        boolean foundName = substance.names.stream()
                .anyMatch(n->n.name.equalsIgnoreCase(iupacName));
        if( foundName ) {
            log.info("substance already had IUPAC name {}", iupacName);
            return false;
        }
        Name iupacNameObject = new Name();
        iupacNameObject.name = iupacName;
        iupacNameObject.type = "sys";
        iupacNameObject.addReference(findOrCreatePubchemReference(substance));
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
        }
        return pubchemReference;
    }
}
