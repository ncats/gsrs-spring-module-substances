package gsrs.module.substance.exporters;


import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.molwitch.inchi.Inchi;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.EntityFetcher;
import ix.core.chem.Chem;
import ix.core.models.Group;
import ix.core.models.Structure;
import ix.core.util.EntityUtils.Key;
import ix.ginas.exporters.*;
import ix.ginas.exporters.Spreadsheet.SpreadsheetRow;
import ix.ginas.models.v1.*;
import ix.utils.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
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
    private SubstanceSpreadsheetExporterConfiguration substanceExporterConfiguration;

    

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
        Spreadsheet spreadsheet = format.createSpreadsheet(out);

        
        SubstanceSpreadsheetExporter.Builder builder = new SubstanceSpreadsheetExporter.Builder( spreadsheet, createColumnRecipes(params));

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

    public Map<Column, ColumnValueRecipe<Substance>> createColumnRecipes(Parameters params) {
    	Map<Column, ColumnValueRecipe<Substance>> defaultRecipeMap= new LinkedHashMap<>();


    	//UUID, APPROVAL_ID, PT, RN, EC, NCIT, RXCUI PUBCHEM ITIS NCBI PLANTS GRIN MPNS INN_ID MF INCHIKEY SMILES INGREDIENT_TYPE 

    	defaultRecipeMap.put(DefaultColumns.UUID, SingleColumnValueRecipe.create(DefaultColumns.UUID, (s, cell) -> cell.write(s.getOrGenerateUUID())));
    	defaultRecipeMap.put(DefaultColumns.APPROVAL_ID, SingleColumnValueRecipe.create(DefaultColumns.APPROVAL_ID, (s, cell) -> cell.writeString(s.getApprovalID())));

    	defaultRecipeMap.put(DefaultColumns.STD_NAME, createRestrictableRecipe(Substance.class,DefaultColumns.STD_NAME, (s, pubOnly,cell) -> {
    		Optional<Name> opName = ((Substance)s).getDisplayName();
    		boolean wroteName = false;
    		if(opName.isPresent()) {
    			cell.writeString(opName.get().stdName);
    			wroteName=true;
    		}
    		if(!wroteName) {
    			//TODO: Something based on what comes back
    		}
    	}).replaceColumnName(DefaultColumns.STD_NAME.name(), "DISPLAY_NAME"));

    	defaultRecipeMap.put(DefaultColumns.CAS, new CodeSystemRecipe(DefaultColumns.CAS, "CAS").replaceColumnName(DefaultColumns.CAS.name(),"RN"));
    	defaultRecipeMap.put(DefaultColumns.EC, new CodeSystemRecipe(DefaultColumns.EC, "ECHA (EC/EINECS)"));
    	defaultRecipeMap.put(DefaultColumns.NCI_THESAURUS, new CodeSystemRecipe(DefaultColumns.NCI_THESAURUS, "NCI_THESAURUS").replaceColumnName(DefaultColumns.NCI_THESAURUS
    			.name(),"NCIT"));

    	defaultRecipeMap.put(DefaultColumns.RXCUI, new CodeSystemRecipe(DefaultColumns.RXCUI, "RXCUI"));
    	defaultRecipeMap.put(DefaultColumns.PUBCHEM, new CodeSystemRecipe(DefaultColumns.PUBCHEM, "PUBCHEM"));

    	defaultRecipeMap.put(DefaultColumns.ITIS, ParentSourceMaterialRecipeWrapper.wrap(new CodeSystemRecipe(DefaultColumns.ITIS, "ITIS"),params.getScrubber()));
    	defaultRecipeMap.put(DefaultColumns.NCBI, ParentSourceMaterialRecipeWrapper.wrap( new CodeSystemRecipe(DefaultColumns.NCBI, "NCBI TAXONOMY"),params.getScrubber()));
    	defaultRecipeMap.put(DefaultColumns.USDA_PLANTS, ParentSourceMaterialRecipeWrapper.wrap( new CodeSystemRecipe(DefaultColumns.USDA_PLANTS, "USDA PLANTS")
    			.replaceColumnName(DefaultColumns.USDA_PLANTS.name(),"PLANTS"),params.getScrubber()
    			));
    	defaultRecipeMap.put(DefaultColumns.GRIN, ParentSourceMaterialRecipeWrapper.wrap( new CodeSystemRecipe(DefaultColumns.GRIN, "GRIN"),params.getScrubber()));
    	defaultRecipeMap.put(DefaultColumns.MPNS, ParentSourceMaterialRecipeWrapper.wrap( new CodeSystemRecipe(DefaultColumns.MPNS, "MPNS"),params.getScrubber()));

    	defaultRecipeMap.put(DefaultColumns.INN, new CodeSystemRecipe(DefaultColumns.INN, "INN").replaceColumnName(DefaultColumns.INN.name(),"INN_ID"));
    	defaultRecipeMap.put(DefaultColumns.USAN, new CodeSystemRecipe(DefaultColumns.USAN, "USAN").replaceColumnName(DefaultColumns.USAN.name(),"USAN_ID"));

    	defaultRecipeMap.put(DefaultColumns.FORMULA, createRestrictableRecipe(Substance.class,DefaultColumns.FORMULA, (s, pubOnly,cell) -> {
    		if (s instanceof ChemicalSubstance) {
    			ChemicalSubstance chemicalSubstance = (ChemicalSubstance) s;
    			Optional.ofNullable(chemicalSubstance.getStructure())
    			.ifPresent(ss->{
    				cell.writeString(ss.formula);
    			});

    		} else if (s instanceof PolymerSubstance) {
    			//TODO: there's a property that should be the fallback for this  
    			// for proteins, polymers, etc
    			cell.writeString("");
    		} 
    	}).replaceColumnName(DefaultColumns.FORMULA.name(),"MF"));


    	defaultRecipeMap.put(DefaultColumns.STD_INCHIKEY_FORMATTED, createRestrictableRecipe(Substance.class,DefaultColumns.STD_INCHIKEY_FORMATTED, (s, pubOnly, cell) -> {
    		if (s instanceof ChemicalSubstance) {
    			ChemicalSubstance chemicalSubstance = (ChemicalSubstance) s;
    			Structure.Stereo ster = chemicalSubstance.getStereochemistry();
    			if (ster!=null && !ster.equals(Structure.Stereo.ABSOLUTE) && !ster.equals(Structure.Stereo.ACHIRAL) && !substanceExporterConfiguration.isIncludeInChiKeysAnyway()) {
    				return;
    			}


    			try {
    				Chemical chem = s.toChemical();
    				cell.writeString(Inchi.asStdInchi(Chem.RemoveQueryAtomsForPseudoInChI(chem))
    						.getKey()
    						.replace("InChIKey=", ""));
    			} catch (Exception e) {

    			}
    		}
    	}).replaceColumnName(DefaultColumns.STD_INCHIKEY_FORMATTED.name(),"INCHIKEY"));



    	defaultRecipeMap.put(DefaultColumns.SMILES, createRestrictableRecipe(DefaultColumns.SMILES, (s,pubOnly, cell) -> {
    		if (s instanceof ChemicalSubstance) {
    			ChemicalSubstance chemicalSubstance = (ChemicalSubstance) s;

    			Optional.ofNullable(chemicalSubstance.getStructure())
    			.ifPresent(ss->{
    				cell.writeString(ss.smiles);
    			});
    		}
    	}));


    	defaultRecipeMap.put(DefaultColumns.INGREDIENT_TYPE, SingleColumnValueRecipe.create(DefaultColumns.INGREDIENT_TYPE, (s, cell) -> {
    		cell.writeString(getIngredientType(s));
    	}));


    	defaultRecipeMap.put(DefaultColumns.NAME, createRestrictableRecipe(Substance.class,DefaultColumns.NAME, (s, pubOnly,cell) -> {
    		cell.writeString(s.getName());                 
    	}).replaceColumnName(DefaultColumns.NAME.name(), "UTF8_DISPLAY_NAME"));




    	defaultRecipeMap.put(DefaultColumns.SUBSTANCE_TYPE, SingleColumnValueRecipe.create(DefaultColumns.SUBSTANCE_TYPE, (s, cell) -> cell.writeString(s.substanceClass.name())));

    	//Lazy place to put new basic columns
    	defaultRecipeMap.put(DefaultColumns.PROTEIN_SEQUENCE, createRestrictableRecipe(DefaultColumns.PROTEIN_SEQUENCE, (s,pubOnly, cell) -> {
    		if (s instanceof ProteinSubstance) {
    			ProteinSubstance proteinSubstance = (ProteinSubstance) s;
    			if(proteinSubstance.protein==null)return;
    			List<Subunit> subunits = proteinSubstance.protein.getSubunits();
    			if(subunits==null)return;
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

    	defaultRecipeMap.put(DefaultColumns.NUCLEIC_ACID_SEQUENCE, createRestrictableRecipe(DefaultColumns.NUCLEIC_ACID_SEQUENCE, (s, pubOnly, cell) -> {
    		//TODO: perhaps put a limit on this? 4000 chars?
    		if (s instanceof NucleicAcidSubstance) {
    			NucleicAcidSubstance nucleicAcidSubstance = (NucleicAcidSubstance) s;
    			if(nucleicAcidSubstance.nucleicAcid==null)return;
    			List<Subunit> subunits = nucleicAcidSubstance.nucleicAcid.getSubunits();
    			if(subunits==null)return;
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
    	defaultRecipeMap.put(DefaultColumns.RECORD_ACCESS_GROUPS, SingleColumnValueRecipe.create(DefaultColumns.RECORD_ACCESS_GROUPS, (s, cell) -> {
    		StringBuilder sb = new StringBuilder();
    		for (Group g : s.getAccess()) {
    			if (sb.length() != 0) {
    				sb.append("|");
    			}
    			sb.append(g.name);
    		}
    		cell.writeString(sb.toString());
    	}));

    	return defaultRecipeMap;

    }


    
    public interface PublicRestrictable<T extends ColumnValueRecipe<U>, U>{
        T asPublicOnly();
    }
    
    public interface PublicRestrictableColumnRecipe<T extends ColumnValueRecipe<U>, U> extends PublicRestrictable<T,U>, ColumnValueRecipe<U>{
        
    }
    
    public interface PublicRestrictionAwareWriteFunction<T>{
        void writeValue(T object,boolean pubOnly, SpreadsheetCell cell);
    }
   

    private static class PublicRestrictableColumnRecipeImpl<T extends ColumnValueRecipe<U>, U> implements PublicRestrictableColumnRecipe<T,U>{

        private  ColumnValueRecipe<U> defaultRecipe;
        private ColumnValueRecipe<U> publicOnlyDefaultRecipe;

        public PublicRestrictableColumnRecipeImpl(ColumnValueRecipe<U> defaultRecipe, ColumnValueRecipe<U> publicOnlyDefaultRecipe) {
            this.defaultRecipe = defaultRecipe;
            this.publicOnlyDefaultRecipe = publicOnlyDefaultRecipe;
        }

        @Override
        public T asPublicOnly() {
            return (T) publicOnlyDefaultRecipe;
        }

        @Override
        public boolean containsColumnName(String name) {
            return defaultRecipe.containsColumnName(name);
        }

        @Override
        public ColumnValueRecipe<U> replaceColumnName(String oldName,
                                                      String newName) {
            ColumnValueRecipe<U> newDefault = defaultRecipe.replaceColumnName(oldName, newName);
            ColumnValueRecipe<U> newPub = publicOnlyDefaultRecipe.replaceColumnName(oldName, newName);

            if(newDefault != defaultRecipe || newPub != publicOnlyDefaultRecipe){
                return new PublicRestrictableColumnRecipeImpl(newDefault, newPub);
            }
            return this;
        }

        @Override
        public int writeHeaderValues(SpreadsheetRow row,
                                     int currentOffset) {
            return defaultRecipe.writeHeaderValues(row,currentOffset);
        }

        @Override
        public int writeValuesFor(SpreadsheetRow row, int currentOffset,
                                  U obj) {
            return defaultRecipe.writeValuesFor(row,currentOffset,obj);
        }
    }
    public static <T extends ColumnValueRecipe<U>,U> PublicRestrictableColumnRecipe<T, U> createRestrictableRecipe(Enum<?> name,PublicRestrictionAwareWriteFunction<U> writerFunction){
        ColumnValueRecipe<U> defaultRecipe = SingleColumnValueRecipe.create(name.name(), (t,cell)->{
            writerFunction.writeValue(t, false, cell);
        });
        ColumnValueRecipe<U> publicOnlyDefaultRecipe = SingleColumnValueRecipe.create(name.name(), (t,cell)->{
            writerFunction.writeValue(t, true, cell);
        });
        
        return new PublicRestrictableColumnRecipeImpl<>(defaultRecipe, publicOnlyDefaultRecipe);
    }
    public static <T extends ColumnValueRecipe<U>,U> PublicRestrictableColumnRecipe<T, U> createRestrictableRecipe(Class<U>  cl,Enum<?> name,PublicRestrictionAwareWriteFunction<U> writerFunction){
        ColumnValueRecipe<U> defaultRecipe = SingleColumnValueRecipe.create(name.name(), (t,cell)->{
            writerFunction.writeValue(t, false, cell);
        });
        ColumnValueRecipe<U> publicOnlyDefaultRecipe = SingleColumnValueRecipe.create(name.name(), (t,cell)->{
            writerFunction.writeValue(t, true, cell);
        });
        
        return new PublicRestrictableColumnRecipeImpl<>(defaultRecipe, publicOnlyDefaultRecipe);
    }
    
    



    private static class ParentSourceMaterialRecipeWrapper extends SubstanceFetcherRecipeWrapper {

        private final RecordScrubber scrubber;

        public ParentSourceMaterialRecipeWrapper(ColumnValueRecipe<Substance> del,RecordScrubber scrubber) {
            super(del);
            this.scrubber = Objects.requireNonNull(scrubber);
        }

        @Override
        public Substance getSubstance(Substance s) {

            if(s instanceof StructurallyDiverseSubstance){
                StructurallyDiverseSubstance sdiv = (StructurallyDiverseSubstance)s;
                SubstanceReference sr=sdiv.structurallyDiverse.parentSubstance;
                if(sr!=null){
                    Key key = Key.of(Substance.class, UUID.fromString(sr.refuuid));
                    Substance par= (Substance)EntityFetcher.of(key).getIfPossible()
                    		.map(o->scrubber.scrub(o).orElse(null))
                    		.filter(oo->oo!=null)
                    		.orElse(null);
                    
                    //this will return itself if there is no parent
                    //TODO: consider if this is correct behavior
                    if(par==null)return s;
                    
                    return par;
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
        public static ParentSourceMaterialRecipeWrapper wrap(ColumnValueRecipe<Substance> col, RecordScrubber scrub){
            return new ParentSourceMaterialRecipeWrapper( col, scrub);
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



    static class CodeSystemRecipe implements SingleColumnValueRecipe<Substance>, PublicRestrictable<CodeSystemRecipe, Substance>{

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
                return new CodeSystemRecipe(newName, codeSystemToFind, publicOnly);
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
