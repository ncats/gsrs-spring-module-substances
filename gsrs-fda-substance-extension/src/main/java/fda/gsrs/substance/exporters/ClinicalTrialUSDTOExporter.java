package fda.gsrs.substance.exporters;

import gov.hhs.gsrs.clinicaltrial.us.api.ClinicalTrialUSDTO;
import gov.hhs.gsrs.clinicaltrial.us.api.ClinicalTrialUSDrugDTO;
import gsrs.cache.GsrsCache;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.springUtils.StaticContextAccessor;
import ix.ginas.exporters.*;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

@Slf4j
public class ClinicalTrialUSDTOExporter implements Exporter<ClinicalTrialUSDTO> {

    enum ClinicalTrialUSDefaultColumns implements Column {
        TRIAL_NUMBER,
        TITLE,
        SUBSTANCE_NAME,
        SUBSTANCE_UUID,
        CONDITIONS,
        SPONSOR_NAME,
        OUTCOME_MEASURES
    }

    private final Spreadsheet spreadsheet;

    private int row = 1;

    private final List<ColumnValueRecipe<ClinicalTrialUSDTO>> recipeMap;

    private static SubstanceEntityService staticSubstanceEntityService;

    private static GsrsCache gsrsCache;

    private ClinicalTrialUSDTOExporter(Builder builder, SubstanceEntityService substanceEntityService) {

        if(staticSubstanceEntityService ==null){
            staticSubstanceEntityService = substanceEntityService;
        }

        this.spreadsheet = builder.spreadsheet;
        this.recipeMap = builder.columns;

        int j = 0;
        Spreadsheet.SpreadsheetRow header = spreadsheet.getRow(0);
        for (ColumnValueRecipe<ClinicalTrialUSDTO> col : recipeMap) {
            j += col.writeHeaderValues(header, j);
        }
    }

    @Override
    public void export(ClinicalTrialUSDTO s) throws IOException {
        Spreadsheet.SpreadsheetRow row = spreadsheet.getRow(this.row++);

        int j = 0;
        for (ColumnValueRecipe<ClinicalTrialUSDTO> recipe : recipeMap) {
            j += recipe.writeValuesFor(row, j, s);
        }
    }

    @Override
    public void close() throws IOException {
        spreadsheet.close();
    }

    private static Map<Column, ColumnValueRecipe<ClinicalTrialUSDTO>> DEFAULT_RECIPE_MAP;

    static{

        DEFAULT_RECIPE_MAP = new LinkedHashMap<>();

        DEFAULT_RECIPE_MAP.put(ClinicalTrialUSDefaultColumns.TRIAL_NUMBER, SingleColumnValueRecipe.create( ClinicalTrialUSDefaultColumns.TRIAL_NUMBER ,(s, cell) ->{
            cell.writeString(s.getTrialNumber());
        }));
        DEFAULT_RECIPE_MAP.put(ClinicalTrialUSDefaultColumns.TITLE, SingleColumnValueRecipe.create( ClinicalTrialUSDefaultColumns.TITLE ,(s, cell) ->{
            cell.writeString(s.getTitle());
        }));

        DEFAULT_RECIPE_MAP.put(ClinicalTrialUSDefaultColumns.SUBSTANCE_NAME, SingleColumnValueRecipe.create( ClinicalTrialUSDefaultColumns.SUBSTANCE_NAME ,(s, cell) ->{
            StringBuilder sb = getClinicalTrialUSDrugDetails(s, ClinicalTrialUSDefaultColumns.SUBSTANCE_NAME);
            cell.writeString(sb.toString());
        }));
        DEFAULT_RECIPE_MAP.put(ClinicalTrialUSDefaultColumns.SUBSTANCE_UUID, SingleColumnValueRecipe.create( ClinicalTrialUSDefaultColumns.SUBSTANCE_UUID ,(s, cell) ->{
            StringBuilder sb = getClinicalTrialUSDrugDetails(s, ClinicalTrialUSDefaultColumns.SUBSTANCE_UUID);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ClinicalTrialUSDefaultColumns.CONDITIONS, SingleColumnValueRecipe.create( ClinicalTrialUSDefaultColumns.CONDITIONS ,(s, cell) ->{
            cell.writeString(s.getConditions());
        }));

        DEFAULT_RECIPE_MAP.put(ClinicalTrialUSDefaultColumns.SPONSOR_NAME, SingleColumnValueRecipe.create( ClinicalTrialUSDefaultColumns.SPONSOR_NAME ,(s, cell) ->{
            cell.writeString(s.getSponsor());
        }));


        DEFAULT_RECIPE_MAP.put(ClinicalTrialUSDefaultColumns.OUTCOME_MEASURES, SingleColumnValueRecipe.create( ClinicalTrialUSDefaultColumns.OUTCOME_MEASURES ,(s, cell) ->{
            cell.writeString(s.getOutcomeMeasures());
        }));
    }



    private static StringBuilder getClinicalTrialUSDrugDetails(ClinicalTrialUSDTO s, ClinicalTrialUSDefaultColumns fieldName) {
        StringBuilder sb = new StringBuilder();

        try {
            if (s.getClinicalTrialUSDrug().size() > 0) {

                for (ClinicalTrialUSDrugDTO substance : s.getClinicalTrialUSDrug()) {

                    if (sb.length() != 0) {
                        sb.append("|");
                    }
                    switch (fieldName) {
                        case SUBSTANCE_UUID:
                            sb.append((substance.getSubstanceKey() != null) ? substance.getSubstanceKey()  : "(No Substance Key)");
                            break;
                        case SUBSTANCE_NAME:
                            Optional<Substance> substanceEntity = staticSubstanceEntityService.get(UUID.fromString(substance.getSubstanceKey()));
                            if(gsrsCache==null){
                                gsrsCache  = StaticContextAccessor.getBean(GsrsCache.class);
                            }
                            String nm = gsrsCache.getOrElse("CTUSDTOEXPSKEY:" + substance.getSubstanceKey(), ()->{
                                Optional<Substance> staticSubstanceEntity = staticSubstanceEntityService.get(UUID.fromString(substance.getSubstanceKey()));
                                return substanceEntity.get().getDisplayName()
                                        .map(n -> n.getName())
                                        .orElse("(No Substance Name)");
                            });
                            sb.append(nm);
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Exception getting CT US substance details.", ex);
        }
        return sb;
    }
    /**
     * Builder class that makes a SpreadsheetExporter.  By default, the default columns are used
     * but these may be modified using the add/remove column methods.
     *
     */
    public static class Builder{
        private final List<ColumnValueRecipe<ClinicalTrialUSDTO>> columns = new ArrayList<>();
        private final Spreadsheet spreadsheet;

        private boolean publicOnly = false;

        /**
         * Create a new Builder that uses the given Spreadsheet to write to.
         * @param spreadSheet the {@link Spreadsheet} object that will be written to by this exporter. can not be null.
         *
         * @throws NullPointerException if spreadsheet is null.
         */
        public Builder(Spreadsheet spreadSheet){
            Objects.requireNonNull(spreadSheet);
            this.spreadsheet = spreadSheet;

            for(Map.Entry<Column, ColumnValueRecipe<ClinicalTrialUSDTO>> entry : DEFAULT_RECIPE_MAP.entrySet()){
                columns.add(entry.getValue());
            }
        }

        public Builder addColumn(Column column, ColumnValueRecipe<ClinicalTrialUSDTO> recipe){
            return addColumn(column.name(), recipe);
        }

        public Builder addColumn(String columnName, ColumnValueRecipe<ClinicalTrialUSDTO> recipe){
            Objects.requireNonNull(columnName);
            Objects.requireNonNull(recipe);
            columns.add(recipe);

            return this;
        }

        public Builder renameColumn(Column oldColumn, String newName){
            return renameColumn(oldColumn.name(), newName);
        }

        public Builder renameColumn(String oldName, String newName){
            //use iterator to preserve order
            ListIterator<ColumnValueRecipe<ClinicalTrialUSDTO>> iter = columns.listIterator();
            while(iter.hasNext()){

                ColumnValueRecipe<ClinicalTrialUSDTO> oldValue = iter.next();
                ColumnValueRecipe<ClinicalTrialUSDTO> newValue = oldValue.replaceColumnName(oldName, newName);
                if(oldValue != newValue){
                    iter.set(newValue);
                }
            }
            return this;
        }

        public ClinicalTrialUSDTOExporter build(SubstanceEntityService substanceEntityService){
            return new ClinicalTrialUSDTOExporter(this, substanceEntityService);
        }

        public Builder includePublicDataOnly(boolean publicOnly){
            this.publicOnly = publicOnly;
            return this;
        }

    }
}