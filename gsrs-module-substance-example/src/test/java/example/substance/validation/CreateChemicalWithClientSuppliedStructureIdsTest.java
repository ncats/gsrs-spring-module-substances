package example.substance.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Moiety;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CreateChemicalWithClientSuppliedStructureIdsTest extends AbstractSubstanceJpaEntityTest {

    private static final UUID CLIENT_STRUCTURE_ID = UUID.fromString("6bdc786d-809e-4da4-b635-5077970e61a9");
    private static final UUID CLIENT_MOIETY_STRUCTURE_ID = UUID.fromString("b75478e8-b7bb-4d92-8ab3-994b51bdb708");

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void createChemicalWithClientSuppliedStructureIds() throws Exception {
        JsonNode json = new ObjectMapper().readTree("""
                {
                  "substanceClass": "chemical",
                  "references": [
                    {
                      "tags": [],
                      "access": [],
                      "docType": "ISO",
                      "citation": "29281",
                      "publicDomain": true,
                      "uuid": "ba459ffe-4fd9-4be3-8c11-98a79d0da0ca"
                    }
                  ],
                  "names": [
                    {
                      "references": [
                        "ba459ffe-4fd9-4be3-8c11-98a79d0da0ca"
                      ],
                      "access": [],
                      "languages": [
                        "en"
                      ],
                      "type": "sys",
                      "name": "1,5-naphthyridin-3-ol"
                    }
                  ],
                  "structure": {
                    "molfile": "\\n   JSDraw204162619112D\\n\\n 11 12  0  0  0  0              0 V2000\\n   12.3760   -7.9040    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   11.0250   -7.1240    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   11.0250   -5.5640    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   13.7270   -7.1240    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   13.7270   -5.5640    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   12.3760   -4.7840    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\\n   15.0780   -4.7840    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   15.0780   -7.9040    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\\n   16.4290   -7.1240    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   16.4290   -5.5640    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   17.7800   -4.7840    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n  1  2  2  0  0  0  0\\n  2  3  1  0  0  0  0\\n  1  4  1  0  0  0  0\\n  4  5  2  0  0  0  0\\n  5  6  1  0  0  0  0\\n  6  3  2  0  0  0  0\\n  5  7  1  0  0  0  0\\n  4  8  1  0  0  0  0\\n  8  9  2  0  0  0  0\\n  9 10  1  0  0  0  0\\n 10  7  2  0  0  0  0\\n 10 11  1  0  0  0  0\\nM  END\\n",
                    "references": [
                      "ba459ffe-4fd9-4be3-8c11-98a79d0da0ca"
                    ],
                    "id": "6bdc786d-809e-4da4-b635-5077970e61a9",
                    "deprecated": false,
                    "digest": "603d3f51cb119e96381182eee215f6698b64724f",
                    "smiles": "c1cc2c(cc(cn2)O)nc1",
                    "formula": "C8H6N2O",
                    "opticalActivity": "none",
                    "atropisomerism": "No",
                    "stereoCenters": 0,
                    "definedStereo": 0,
                    "ezCenters": 0,
                    "charge": 0,
                    "mwt": 146.1463,
                    "count": 1,
                    "stereochemistry": "ACHIRAL",
                    "_inchi": "InChI=1S/C8H6N2O/c11-6-4-8-7(10-5-6)2-1-3-9-8/h1-5,11H",
                    "_inchiKey": "VIZYFHZVBGCOAH-UHFFFAOYSA-N",
                    "hash": "VIZYFHZVBGCOAH_UHFFFAOYSA_N",
                    "uuid": "6bdc786d-809e-4da4-b635-5077970e61a9"
                  },
                  "codes": [],
                  "relationships": [],
                  "properties": [],
                  "access": [
                    "protected"
                  ],
                  "definitionLevel": "COMPLETE",
                  "definitionType": "PRIMARY",
                  "tags": [],
                  "moieties": [
                    {
                      "id": "b75478e8-b7bb-4d92-8ab3-994b51bdb708",
                      "deprecated": false,
                      "digest": "671f000059355633c1bf24029f718ecdb4dd1743",
                      "molfile": "\\n  CDK     04162619112D\\n\\n 11 12  0  0  0  0  0  0  0  0999 V2000\\n   12.3760   -7.9040    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   11.0250   -7.1240    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   11.0250   -5.5640    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   13.7270   -7.1240    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   13.7270   -5.5640    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   12.3760   -4.7840    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\\n   15.0780   -4.7840    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   15.0780   -7.9040    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\\n   16.4290   -7.1240    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   16.4290   -5.5640    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   17.7800   -4.7840    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n  1  2  2  0  0  0  0\\n  2  3  1  0  0  0  0\\n  1  4  1  0  0  0  0\\n  4  5  2  0  0  0  0\\n  5  6  1  0  0  0  0\\n  6  3  2  0  0  0  0\\n  5  7  1  0  0  0  0\\n  4  8  1  0  0  0  0\\n  8  9  2  0  0  0  0\\n  9 10  1  0  0  0  0\\n 10  7  2  0  0  0  0\\n 10 11  1  0  0  0  0\\nM  END",
                      "smiles": "c1cc2c(cc(cn2)O)nc1",
                      "formula": "C8H6N2O",
                      "opticalActivity": "none",
                      "atropisomerism": "No",
                      "stereoCenters": 0,
                      "definedStereo": 0,
                      "ezCenters": 0,
                      "charge": 0,
                      "mwt": 146.1463,
                      "count": 1,
                      "stereochemistry": "ACHIRAL",
                      "_inchi": "InChI=1S/C8H6N2O/c11-6-4-8-7(10-5-6)2-1-3-9-8/h1-5,11H",
                      "_inchiKey": "VIZYFHZVBGCOAH-UHFFFAOYSA-N",
                      "hash": "VIZYFHZVBGCOAH_UHFFFAOYSA_N",
                      "countAmount": {
                        "deprecated": false,
                        "type": "MOL RATIO",
                        "average": 1,
                        "units": "MOL RATIO",
                        "references": [],
                        "access": []
                      },
                      "uuid": "b75478e8-b7bb-4d92-8ab3-994b51bdb708"
                    }
                  ],
                  "notes": []
                }
                """);

        ChemicalSubstance created = (ChemicalSubstance) ensurePass(substanceEntityService.createEntity(json, true));

        assertNotNull(created);
        assertNotNull(created.getStructure());
        assertNotEquals(CLIENT_STRUCTURE_ID, created.getStructure().id);
        assertEquals(1, created.getMoieties().size());

        Moiety moiety = created.getMoieties().get(0);
        assertNotNull(moiety.structure);
        assertNotEquals(CLIENT_MOIETY_STRUCTURE_ID, moiety.structure.id);
    }
}
