package fda.gsrs.substance.exporters;

import gov.hhs.gsrs.clinicaltrial.europe.api.ClinicalTrialEuropeDTO;
import gov.hhs.gsrs.clinicaltrial.europe.api.ClinicalTrialEuropeDrugDTO;
import gov.hhs.gsrs.clinicaltrial.europe.api.ClinicalTrialEuropeMeddraDTO;
import gov.hhs.gsrs.clinicaltrial.europe.api.ClinicalTrialEuropeProductDTO;
import gsrs.cache.GsrsCache;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.springUtils.StaticContextAccessor;
import ix.ginas.exporters.*;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

@Slf4j
public class ClinicalTrialEuropeDTOExporter implements Exporter<ClinicalTrialEuropeDTO> {

    enum ClinicalTrialEuropeDefaultColumns implements Column {
        TRIAL_NUMBER,
        TITLE,
        SUBSTANCE_NAME,
        SUBSTANCE_UUID,
        CONDITIONS,
        SPONSOR_NAME,
        RESULTS
    }

    private final Spreadsheet spreadsheet;

    private int row = 1;

    private final List<ColumnValueRecipe<ClinicalTrialEuropeDTO>> recipeMap;

    private static SubstanceEntityService staticSubstanceEntityService;

    private static GsrsCache gsrsCache;

    private ClinicalTrialEuropeDTOExporter(Builder builder, SubstanceEntityService substanceEntityService) {

        if(staticSubstanceEntityService ==null){
            staticSubstanceEntityService = substanceEntityService;
        }

        this.spreadsheet = builder.spreadsheet;
        this.recipeMap = builder.columns;

        int j = 0;
        Spreadsheet.SpreadsheetRow header = spreadsheet.getRow(0);
        for (ColumnValueRecipe<ClinicalTrialEuropeDTO> col : recipeMap) {
            j += col.writeHeaderValues(header, j);
        }
    }

    @Override
    public void export(ClinicalTrialEuropeDTO s) throws IOException {
        Spreadsheet.SpreadsheetRow row = spreadsheet.getRow(this.row++);

        int j = 0;
        for (ColumnValueRecipe<ClinicalTrialEuropeDTO> recipe : recipeMap) {
            j += recipe.writeValuesFor(row, j, s);
        }
    }

    @Override
    public void close() throws IOException {
        spreadsheet.close();
    }

    private static Map<Column, ColumnValueRecipe<ClinicalTrialEuropeDTO>> DEFAULT_RECIPE_MAP;

    static{

        DEFAULT_RECIPE_MAP = new LinkedHashMap<>();

        DEFAULT_RECIPE_MAP.put(ClinicalTrialEuropeDefaultColumns.TRIAL_NUMBER, SingleColumnValueRecipe.create( ClinicalTrialEuropeDefaultColumns.TRIAL_NUMBER ,(s, cell) ->{
            cell.writeString(s.getTrialNumber());
        }));
        DEFAULT_RECIPE_MAP.put(ClinicalTrialEuropeDefaultColumns.TITLE, SingleColumnValueRecipe.create( ClinicalTrialEuropeDefaultColumns.TITLE ,(s, cell) ->{
            cell.writeString(s.getTitle());
        }));

        DEFAULT_RECIPE_MAP.put(ClinicalTrialEuropeDefaultColumns.SUBSTANCE_NAME, SingleColumnValueRecipe.create( ClinicalTrialEuropeDefaultColumns.SUBSTANCE_NAME ,(s, cell) ->{
            StringBuilder sb = getClinicalTrialEuropeDrugDetails(s, ClinicalTrialEuropeDefaultColumns.SUBSTANCE_NAME);
            cell.writeString(sb.toString());
        }));
        DEFAULT_RECIPE_MAP.put(ClinicalTrialEuropeDefaultColumns.SUBSTANCE_UUID, SingleColumnValueRecipe.create( ClinicalTrialEuropeDefaultColumns.SUBSTANCE_UUID ,(s, cell) ->{
            StringBuilder sb = getClinicalTrialEuropeDrugDetails(s, ClinicalTrialEuropeDefaultColumns.SUBSTANCE_UUID);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ClinicalTrialEuropeDefaultColumns.CONDITIONS, SingleColumnValueRecipe.create( ClinicalTrialEuropeDefaultColumns.CONDITIONS ,(s, cell) ->{
            StringBuilder sb = getMeddraTermDetails(s, ClinicalTrialEuropeDefaultColumns.CONDITIONS);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ClinicalTrialEuropeDefaultColumns.SPONSOR_NAME, SingleColumnValueRecipe.create( ClinicalTrialEuropeDefaultColumns.SPONSOR_NAME ,(s, cell) ->{
            cell.writeString(s.getSponsorName());
        }));

        DEFAULT_RECIPE_MAP.put(ClinicalTrialEuropeDefaultColumns.RESULTS, SingleColumnValueRecipe.create( ClinicalTrialEuropeDefaultColumns.RESULTS,(s, cell) ->{
            cell.writeString(s.getTrialResults());
        }));
    }

    private static StringBuilder getClinicalTrialEuropeDrugDetails(ClinicalTrialEuropeDTO s, ClinicalTrialEuropeDefaultColumns fieldName) {
        StringBuilder sb = new StringBuilder();
        try {
            List<ClinicalTrialEuropeProductDTO> pList= s.getClinicalTrialEuropeProductList();
            if (pList!=null && !pList.isEmpty()) {
                for (ClinicalTrialEuropeProductDTO p : pList) {
                    List<ClinicalTrialEuropeDrugDTO> dList = p.getClinicalTrialEuropeDrugList();
                    for (ClinicalTrialEuropeDrugDTO d : dList) {
                        if (sb.length() != 0) {
                            sb.append("|");
                        }
                        switch (fieldName) {
                            case SUBSTANCE_UUID:
                                sb.append((d.getSubstanceKey() != null) ? d.getSubstanceKey() : "(No Substance Key)");
                                break;
                            case SUBSTANCE_NAME:
                                if(gsrsCache==null){
                                    gsrsCache  = StaticContextAccessor.getBean(GsrsCache.class);
                                }
                                String nm = gsrsCache.getOrElse("CTEUDTOEXPSKEY:" + d.getSubstanceKey(), ()->{
                                    Optional<Substance> substanceEntity = staticSubstanceEntityService.get(UUID.fromString(d.getSubstanceKey()));
                                    return substanceEntity.get().getDisplayName()
                                            .map(n -> n.getName())
                                            .orElse("(No Substance Name)");

                                });
                                sb.append(nm);
                            default:
                                break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Exception getting CT EU substance details.", ex);
        }
        return sb;
    }

    private static StringBuilder getMeddraTermDetails(ClinicalTrialEuropeDTO s, ClinicalTrialEuropeDefaultColumns fieldName) {
        StringBuilder sb = new StringBuilder();
        List<ClinicalTrialEuropeMeddraDTO> list = s.getClinicalTrialEuropeMeddraList();
        if(list !=null && !list.isEmpty()){
            for(ClinicalTrialEuropeMeddraDTO item : list){
                if(sb.length()!=0) {
                    sb.append("|");
                }
                switch (fieldName) {
                    case CONDITIONS:
                        String value = (item != null && item.getMeddraTerm()!=null) ? item.getMeddraTerm(): "(Null Meddra Term)";
                        sb.append(value);
                        break;
                    default:
                        break;
                }
            }
        }
        return sb;
    }


    /**
     * Builder class that makes a SpreadsheetExporter.  By basic, the basic columns are used
     * but these may be modified using the add/remove column methods.
     *
     */
    public static class Builder{
        private final List<ColumnValueRecipe<ClinicalTrialEuropeDTO>> columns = new ArrayList<>();
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

            for(Map.Entry<Column, ColumnValueRecipe<ClinicalTrialEuropeDTO>> entry : DEFAULT_RECIPE_MAP.entrySet()){
                columns.add(entry.getValue());
            }
        }

        public Builder addColumn(Column column, ColumnValueRecipe<ClinicalTrialEuropeDTO> recipe){
            return addColumn(column.name(), recipe);
        }

        public Builder addColumn(String columnName, ColumnValueRecipe<ClinicalTrialEuropeDTO> recipe){
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
            ListIterator<ColumnValueRecipe<ClinicalTrialEuropeDTO>> iter = columns.listIterator();
            while(iter.hasNext()){

                ColumnValueRecipe<ClinicalTrialEuropeDTO> oldValue = iter.next();
                ColumnValueRecipe<ClinicalTrialEuropeDTO> newValue = oldValue.replaceColumnName(oldName, newName);
                if(oldValue != newValue){
                    iter.set(newValue);
                }
            }
            return this;
        }

        public ClinicalTrialEuropeDTOExporter build(SubstanceEntityService substanceEntityService){
            return new ClinicalTrialEuropeDTOExporter(this, substanceEntityService);
        }

        public Builder includePublicDataOnly(boolean publicOnly){
            this.publicOnly = publicOnly;
            return this;
        }

    }
}