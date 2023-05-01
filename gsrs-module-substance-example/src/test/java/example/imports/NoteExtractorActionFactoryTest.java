package example.imports;

import gov.nih.ncats.molwitch.Chemical;
import gsrs.dataexchange.model.MappingAction;
import gsrs.importer.DefaultPropertyBasedRecordContext;
import gsrs.importer.PropertyBasedDataRecordContext;
import gsrs.module.substance.importers.importActionFactories.NameExtractorActionFactory;
import gsrs.module.substance.importers.importActionFactories.NotesExtractorActionFactory;
import gsrs.module.substance.importers.model.ChemicalBackedSDRecordContext;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Note;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class NoteExtractorActionFactoryTest {

    @Test
    public void testCreateNoteFromParameters() throws Exception {
        NotesExtractorActionFactory notesExtractorActionFactory = new NotesExtractorActionFactory();
        String smilesForDiclofenac="O=C(O)Cc1ccccc1Nc2c(Cl)cccc2Cl";
        String noteSubstance= "Here is some information";
        Chemical c = Chemical.parse(smilesForDiclofenac);

        ChemicalBackedSDRecordContext ctx = new ChemicalBackedSDRecordContext(c);
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("note", noteSubstance);
        inputParams.put("parameterToIgnore", "nonsense");

        ChemicalSubstanceBuilder chemicalSubstance = new ChemicalSubstanceBuilder();
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.smiles=smilesForDiclofenac;

        chemicalSubstance.setStructure(structure);
        MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> action= notesExtractorActionFactory.create(inputParams);
        action.act(chemicalSubstance, ctx);
        ChemicalSubstance newChem = chemicalSubstance.build();
        Note firstNote = newChem.notes.get(0);
        Assertions.assertEquals(noteSubstance, firstNote.note);
        Assertions.assertFalse(firstNote.note.contains("nonsense"));
    }

    @Test
    public void testCreateNoteFromData() throws Exception {
        NotesExtractorActionFactory notesExtractorActionFactory = new NotesExtractorActionFactory();
        String smilesForDiclofenac="O=C(O)Cc1ccccc1Nc2c(Cl)cccc2Cl";
        String noteSubstance= "Here is some information";
        Chemical c = Chemical.parse(smilesForDiclofenac);

        DefaultPropertyBasedRecordContext ctx = new DefaultPropertyBasedRecordContext();
        ctx.setProperty("note_field", noteSubstance);
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("note", "{{note_field}}");
        inputParams.put("parameterToIgnore", "nonsense");

        ChemicalSubstanceBuilder chemicalSubstance = new ChemicalSubstanceBuilder();
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.smiles=smilesForDiclofenac;

        chemicalSubstance.setStructure(structure);
        MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> action= notesExtractorActionFactory.create(inputParams);
        action.act(chemicalSubstance, ctx);
        ChemicalSubstance newChem = chemicalSubstance.build();
        Note firstNote = newChem.notes.get(0);
        Assertions.assertEquals(noteSubstance, firstNote.note);
        Assertions.assertFalse(firstNote.note.contains("nonsense"));
    }

}
