package gsrs.module.substance.exporters;

import ix.ginas.exporters.Column;

/**
 * Created by katzelda on 8/19/16.
 */
public enum DefaultColumns implements Column {

    UUID,
    NAME,
    STD_NAME,
    APPROVAL_ID,
    SMILES,
    FORMULA,
    SUBSTANCE_TYPE,
    STD_INCHIKEY,
    STD_INCHIKEY_FORMATTED,
    //STD_INCHI,
    CAS,
    ITIS,
    NCBI,
    USDA_PLANTS,
    POWO,
    INN,
    USAN,
    EC,
    NCI_THESAURUS,
    PROTEIN_SEQUENCE,
    NUCLEIC_ACID_SEQUENCE,
    RECORD_ACCESS_GROUPS,
    RXCUI,
    PUBCHEM,
    SMSID,
    MPNS,
    GRIN,
    INGREDIENT_TYPE,
    DAILYMED,
    EPA_CompTox,
    CATALOGUE_OF_LIFE,
}