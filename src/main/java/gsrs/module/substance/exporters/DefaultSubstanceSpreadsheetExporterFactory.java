package gsrs.module.substance.exporters;



import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.molwitch.inchi.Inchi;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.models.Group;
import ix.core.models.Structure;
import ix.ginas.exporters.*;
import ix.ginas.models.v1.*;
import ix.utils.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * {@link ExporterFactory} that supports writing spreadsheet data
 * in Excel, tab and comma separated formats.
 *
 * Created by katzelda on 8/23/16.
 */
@Slf4j
public class DefaultSubstanceSpreadsheetExporterFactory implements ExporterFactory {




    private static final Set<OutputFormat> FORMATS;

    static{
        Set<OutputFormat> set = new LinkedHashSet<>();
        set.add(SpreadsheetFormat.TSV);
        set.add(SpreadsheetFormat.CSV);
        set.add(SpreadsheetFormat.XLSX);

        FORMATS = Collections.unmodifiableSet(set);
    }

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    private SubstanceExporterConfiguration substanceExporterConfiguration;

    private Map<Column, ColumnValueRecipe<Substance>> DEFAULT_RECIPE_MAP;

    @Override
    public Set<OutputFormat> getSupportedFormats() {
        return FORMATS;
    }

    @Override
    public boolean supports(Parameters params) {
        return params.getFormat() instanceof SpreadsheetFormat;
    }

    @Override
    public SubstanceSpreadsheetExporter createNewExporter(OutputStream out, Parameters params) throws IOException {

        SpreadsheetFormat format = (SpreadsheetFormat)params.getFormat();
        Spreadsheet spreadsheet = format.createSpeadsheet(out);

        createdDefaultMapIfNeeded();
        SubstanceSpreadsheetExporter.Builder builder = new SubstanceSpreadsheetExporter.Builder( spreadsheet, DEFAULT_RECIPE_MAP);

        configure(builder, params);

        return builder.build();
    }

    protected void configure(SubstanceSpreadsheetExporter.Builder builder, Parameters params){
        builder.includePublicDataOnly(params.publicOnly());
    }


    /**
     *
     *  Has a reflexive type. Looks through the substance to see if it
     *  has any relationships which points back to itself and contains
     *  the given string, returns true.
     *
     **/
    private static boolean hasReflexiveType(Substance s, String typeContains){

        boolean isReflexive = s.relationships
                .stream()
                .filter(r -> r.type.contains(typeContains))
                .filter(r -> r.relatedSubstance.refuuid.equals(s.uuid.toString()))
                .findAny()
                .isPresent();

        return isReflexive;
    }

    /**
     *
     *  Returns the ingredient type classification for
     *  the given substance.
     *
     **/
    private static String getIngredientType(Substance s){

        if(hasReflexiveType(s,"IONIC MOIETY")){
            return "IONIC MOIETY";
        }

        if(hasReflexiveType(s,"MOLECULAR FRAGMENT")){
            return "MOLECULAR FRAGMENT";
        }

        if(hasReflexiveType(s,"UNSPECIFIED INGREDIENT")){
            return "UNSPECIFIED INGREDIENT";
        }

        if(hasReflexiveType(s,"SPECIFIED SUBSTANCE")){
            return "SPECIFIED SUBSTANCE";
        }

        return "INGREDIENT SUBSTANCE";
    }

     synchronized void createdDefaultMapIfNeeded() {

         if (DEFAULT_RECIPE_MAP == null) {
             Map<Column, ColumnValueRecipe<Substance>> DEFAULT_RECIPE_MAP = new LinkedHashMap<>();

             DEFAULT_RECIPE_MAP.put(DefaultColumns.UUID, SingleColumnValueRecipe.create(DefaultColumns.UUID, (s, cell) -> cell.write(s.getOrGenerateUUID())));
             //TODO preferred TERM ?
             DEFAULT_RECIPE_MAP.put(DefaultColumns.NAME, SingleColumnValueRecipe.create(DefaultColumns.NAME, (s, cell) -> cell.writeString(s.getName())));
             DEFAULT_RECIPE_MAP.put(DefaultColumns.APPROVAL_ID, SingleColumnValueRecipe.create(DefaultColumns.APPROVAL_ID, (s, cell) -> cell.writeString(s.getApprovalID())));

             DEFAULT_RECIPE_MAP.put(DefaultColumns.SMILES, SingleColumnValueRecipe.create(DefaultColumns.SMILES, (s, cell) -> {
                 if (s instanceof ChemicalSubstance) {
                     cell.writeString(((ChemicalSubstance) s).structure.smiles);
                 }
             }));

             DEFAULT_RECIPE_MAP.put(DefaultColumns.FORMULA, SingleColumnValueRecipe.create(DefaultColumns.FORMULA, (s, cell) -> {
                 if (s instanceof ChemicalSubstance) {
                     cell.writeString(((ChemicalSubstance) s).structure.formula);
                 } else if (s instanceof PolymerSubstance) {
                     cell.writeString("Polymer substance not supported");
                 }
             }));
             DEFAULT_RECIPE_MAP.put(DefaultColumns.SUBSTANCE_TYPE, SingleColumnValueRecipe.create(DefaultColumns.SUBSTANCE_TYPE, (s, cell) -> cell.writeString(s.substanceClass.name())));

             //DEFAULT_RECIPE_MAP.put(DefaultColumns.STD_INCHIKEY, new  ChemicalExportRecipe(Chemical.FORMAT_STDINCHIKEY));

//            boolean includeInChiKeysAnyway = ConfigHelper.getBoolean("ix.gsrs.delimitedreports.inchikeysforambiguousstereo", false);
             log.debug("includeInChiKeysAnyway: " + substanceExporterConfiguration.isIncludeInChiKeysAnyway());
             DEFAULT_RECIPE_MAP.put(DefaultColumns.STD_INCHIKEY_FORMATTED, SingleColumnValueRecipe.create(DefaultColumns.STD_INCHIKEY_FORMATTED, (s, cell) -> {
                 if (s instanceof ChemicalSubstance) {
                     Structure.Stereo ster = ((ChemicalSubstance) s).getStereochemistry();
                     if (!ster.equals(Structure.Stereo.ABSOLUTE) && !ster.equals(Structure.Stereo.ACHIRAL) && !substanceExporterConfiguration.isIncludeInChiKeysAnyway()) {
                         return;
                     }

                     try {
                         Chemical chem = s.toChemical();
                         cell.writeString(Inchi.asStdInchi(chem).getKey().replace("InChIKey=", ""));
                     } catch (Exception e) {

                     }
                 }
             }));

             // DEFAULT_RECIPE_MAP.put(DefaultColumns.STD_INCHI, new  ChemicalExportRecipe(Chemical.FORMAT_STDINCHI));


             DEFAULT_RECIPE_MAP.put(DefaultColumns.CAS, new CodeSystemRecipe(DefaultColumns.CAS, "CAS"));
             DEFAULT_RECIPE_MAP.put(DefaultColumns.EC, new CodeSystemRecipe(DefaultColumns.EC, "ECHA (EC/EINECS)"));
             DEFAULT_RECIPE_MAP.put(DefaultColumns.ITIS, ParentSourceMaterialRecipeWrapper.wrap(substanceRepository, new CodeSystemRecipe(DefaultColumns.ITIS, "ITIS")));
             DEFAULT_RECIPE_MAP.put(DefaultColumns.NCBI, ParentSourceMaterialRecipeWrapper.wrap(substanceRepository, new CodeSystemRecipe(DefaultColumns.NCBI, "NCBI TAXONOMY")));
             DEFAULT_RECIPE_MAP.put(DefaultColumns.USDA_PLANTS, ParentSourceMaterialRecipeWrapper.wrap(substanceRepository, new CodeSystemRecipe(DefaultColumns.USDA_PLANTS, "USDA PLANTS")));
             DEFAULT_RECIPE_MAP.put(DefaultColumns.INN, new CodeSystemRecipe(DefaultColumns.INN, "INN"));
             DEFAULT_RECIPE_MAP.put(DefaultColumns.NCI_THESAURUS, new CodeSystemRecipe(DefaultColumns.NCI_THESAURUS, "NCI_THESAURUS"));

             DEFAULT_RECIPE_MAP.put(DefaultColumns.RXCUI, new CodeSystemRecipe(DefaultColumns.RXCUI, "RXCUI"));
             DEFAULT_RECIPE_MAP.put(DefaultColumns.PUBCHEM, new CodeSystemRecipe(DefaultColumns.PUBCHEM, "PUBCHEM"));
             DEFAULT_RECIPE_MAP.put(DefaultColumns.MPNS, ParentSourceMaterialRecipeWrapper.wrap(substanceRepository, new CodeSystemRecipe(DefaultColumns.MPNS, "MPNS")));
             DEFAULT_RECIPE_MAP.put(DefaultColumns.GRIN, ParentSourceMaterialRecipeWrapper.wrap(substanceRepository, new CodeSystemRecipe(DefaultColumns.GRIN, "GRIN")));


             DEFAULT_RECIPE_MAP.put(DefaultColumns.INGREDIENT_TYPE, SingleColumnValueRecipe.create(DefaultColumns.INGREDIENT_TYPE, (s, cell) -> {
                 cell.writeString(getIngredientType(s));
             }));


             //Lazy place to put new default columns
             DEFAULT_RECIPE_MAP.put(DefaultColumns.PROTEIN_SEQUENCE, SingleColumnValueRecipe.create(DefaultColumns.PROTEIN_SEQUENCE, (s, cell) -> {
                 if (s instanceof ProteinSubstance) {
                     List<Subunit> subunits = ((ProteinSubstance) s).protein.getSubunits();
                     StringBuilder sb = new StringBuilder();
                     for (Subunit su : subunits) {
                         if (sb.length() != 0) {
                             sb.append("|");
                         }
                         sb.append(su.sequence);
                     }
                     cell.writeString(sb.toString());
                 }
             }));

             DEFAULT_RECIPE_MAP.put(DefaultColumns.NUCLEIC_ACID_SEQUENCE, SingleColumnValueRecipe.create(DefaultColumns.NUCLEIC_ACID_SEQUENCE, (s, cell) -> {
                 if (s instanceof NucleicAcidSubstance) {
                     List<Subunit> subunits = ((NucleicAcidSubstance) s).nucleicAcid.getSubunits();

                     StringBuilder sb = new StringBuilder();

                     for (Subunit su : subunits) {
                         if (sb.length() != 0) {
                             sb.append("|");
                         }
                         sb.append(su.sequence);
                     }
                     cell.writeString(sb.toString());
                 }
             }));
             DEFAULT_RECIPE_MAP.put(DefaultColumns.RECORD_ACCESS_GROUPS, SingleColumnValueRecipe.create(DefaultColumns.RECORD_ACCESS_GROUPS, (s, cell) -> {
                 StringBuilder sb = new StringBuilder();
                 for (Group g : s.getAccess()) {
                     if (sb.length() != 0) {
                         sb.append("|");
                     }
                     sb.append(g.name);
                 }
                 cell.writeString(sb.toString());
             }));


         }
     }

    private interface SubstanceColumnValueRecipe extends SingleColumnValueRecipe<Substance>{

        public default SubstanceFetcherRecipeWrapper wrapped(Function<Substance, Substance> trans){
            return new SubstanceFetcherRecipeWrapper(this){
                @Override
                public Substance getSubstance(Substance s) {
                    return trans.apply(s);
                }
            };

        }
    }



    private static class ParentSourceMaterialRecipeWrapper extends SubstanceFetcherRecipeWrapper {

        private final SubstanceRepository substanceRepository;

        public ParentSourceMaterialRecipeWrapper(SubstanceRepository substanceRepository, ColumnValueRecipe<Substance> del) {
            super(del);
            this.substanceRepository = Objects.requireNonNull(substanceRepository);
        }

        @Override
        public Substance getSubstance(Substance s) {

            if(s instanceof StructurallyDiverseSubstance){
                StructurallyDiverseSubstance sdiv = (StructurallyDiverseSubstance)s;
                SubstanceReference sr=sdiv.structurallyDiverse.parentSubstance;
                if(sr!=null){
                    Substance full = substanceRepository.findBySubstanceReference(sr);
                    if(full!=null){
                        return full;
                    }
                }
            }
            return s;
        }

        /**
         * Fetches the parent substance (if one exists) rather than the given substance
         * for use in column recipes.
         * @param col
         * @return
         */
        public static ParentSourceMaterialRecipeWrapper wrap(SubstanceRepository substanceRepository, ColumnValueRecipe<Substance> col){
            return new ParentSourceMaterialRecipeWrapper(substanceRepository, col);
        }

    }



    /**
     * Wraps a {@link ColumnValueRecipe} to fetch a (possibly) different object before applying
     * the recipe.
     *
     * @author tyler
     *
     */
    private static abstract class SubstanceFetcherRecipeWrapper implements ColumnValueRecipe<Substance>{

        ColumnValueRecipe<Substance> _delegate;

        public SubstanceFetcherRecipeWrapper(ColumnValueRecipe<Substance>  del){
            this._delegate=del;

        }

        public abstract Substance getSubstance(Substance s);


        @Override
        public int writeValuesFor(Spreadsheet.SpreadsheetRow row, int currentOffset, Substance obj) {
            return _delegate.writeValuesFor(row, currentOffset, getSubstance(obj));
        }

        @Override
        public int writeHeaderValues(Spreadsheet.SpreadsheetRow row, int currentOffset) {
            return _delegate.writeHeaderValues(row, currentOffset);
        }

        @Override
        public boolean containsColumnName(String name) {
            return _delegate.containsColumnName(name);
        }

        @Override
        public ColumnValueRecipe<Substance> replaceColumnName(String oldName, String newName) {
            _delegate = _delegate.replaceColumnName(oldName, newName);
            return this;
        }
    }



    static class CodeSystemRecipe implements SingleColumnValueRecipe<Substance>{

        private final String columnName;

        private final String codeSystemToFind;
        private final boolean publicOnly;

        public CodeSystemRecipe(Enum<?> columnName, String codeSystemToFind) {
            this(columnName, codeSystemToFind, false);
        }



        public CodeSystemRecipe(Enum<?> columnName, String codeSystemToFind, boolean publicOnly) {
            this.codeSystemToFind = codeSystemToFind;
            this.publicOnly = publicOnly;
            this.columnName = columnName.name();
        }
        private CodeSystemRecipe(String columnName, String codeSystemToFind, boolean publicOnly) {
            this.codeSystemToFind = codeSystemToFind;
            this.publicOnly = publicOnly;
            this.columnName = columnName;
        }

        @Override
        public int writeHeaderValues(Spreadsheet.SpreadsheetRow row, int currentOffset) {
            row.getCell(currentOffset).writeString(columnName);
            return 1;
        }

        public CodeSystemRecipe asPublicOnly(){
            return new CodeSystemRecipe(columnName, codeSystemToFind, true);
        }

        @Override
        public boolean containsColumnName(String name) {
            return Objects.equals(name, columnName);
        }

        @Override
        public ColumnValueRecipe<Substance> replaceColumnName(String oldName, String newName) {
            if(containsColumnName(oldName)){
                return new CodeSystemRecipe(newName, codeSystemToFind, true);
            }
            return this;
        }

        @Override
        public void writeValue(Substance s, SpreadsheetCell cell) {

            s.codes
                    .stream()
                    .filter(cd->!(publicOnly && !cd.isPublic()))
                    .filter(cd->codeSystemToFind.equalsIgnoreCase(cd.codeSystem))
                    .sorted(codePriority.get())
                    .findFirst()
                    .map(cd->{
                        if("PRIMARY".equals(cd.type)){
                            return cd.code;
                        }else{
                            return cd.code + " [" + cd.type + "]";
                        }
                    })
                    .ifPresent(cdstr->{
                        cell.writeString(cdstr);
                    });
        }

        static CachedSupplier<Comparator<Code>> codePriority = CachedSupplier.of(()->{
            return Util.comparator(c->c.type, Stream.of("PRIMARY",
                    "MAJOR COMPONENT STRUCTURE/SEQUENCE",
                    "NON-SPECIFIC STEREOCHEMISTRY",
                    "NON-SPECIFIC STOICHIOMETRY",
                    "GENERIC (FAMILY)",
                    "NON-SPECIFIC SUBSTITUTION",
                    "ALTERNATIVE",
                    "NO STRUCTURE GIVEN",
                    "SUPERCEDED"));
        });




    }

}