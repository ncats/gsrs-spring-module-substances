package gsrs.module.substance.exporters;

import ix.ginas.exporters.*;
import ix.ginas.models.v1.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

/**
 * Substance Exporter that writes out data to a Spreadsheet.
 * Created by katzelda on 8/19/16.
 */
@Slf4j
public class SubstanceSpreadsheetExporter implements Exporter<Substance> {

    private final Spreadsheet spreadsheet;

    private int row=1;

    private final List<ColumnValueRecipe<Substance>> recipeMap;


    private SubstanceSpreadsheetExporter(Builder builder){
        this.spreadsheet = builder.spreadsheet;
        this.recipeMap = builder.columns;
        int j=0;
        Spreadsheet.SpreadsheetRow header = spreadsheet.getRow(0);
        for(ColumnValueRecipe<Substance> col : recipeMap){
            j+= col.writeHeaderValues(header, j);
        }
    }
    @Override
    public void export(Substance s) throws IOException {
        Spreadsheet.SpreadsheetRow row = spreadsheet.getRow( this.row++);

        int j=0;
        for(ColumnValueRecipe<Substance> recipe : recipeMap){
            j+= recipe.writeValuesFor(row, j, s);
        }
    }

    @Override
    public void close() throws IOException {
        spreadsheet.close();
    }


    /**
     * Builder class that makes a SpreadsheetExporter.  By basic, the basic columns are used
     * but these may be modified using the add/remove column methods.
     *
     */
    public static class Builder{
        private final List<ColumnValueRecipe<Substance>> columns = new ArrayList<>();
        private final Spreadsheet spreadsheet;

        private boolean publicOnly = false;

        /**
         * Create a new Builder that uses the given Spreadsheet to write to.
         * @param spreadSheet the {@link Spreadsheet} object that will be written to by this exporter. can not be null.
         *
         * @throws NullPointerException if spreadsheet is null.
         */
        public Builder(Spreadsheet spreadSheet, Map<Column, ColumnValueRecipe<Substance>> defaultColumns){

            this.spreadsheet = Objects.requireNonNull(spreadSheet);

            for(Map.Entry<Column, ColumnValueRecipe<Substance>> entry : defaultColumns.entrySet()){
                columns.add(entry.getValue());
            }
        }

        public Builder addColumn(Column column, ColumnValueRecipe<Substance> recipe){
            return addColumn(column.name(), recipe);
        }

        public Builder addColumn(String columnName, ColumnValueRecipe<Substance> recipe){
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
            ListIterator<ColumnValueRecipe<Substance>> iter = columns.listIterator();
            while(iter.hasNext()){

                ColumnValueRecipe<Substance> oldValue = iter.next();
                ColumnValueRecipe<Substance> newValue = oldValue.replaceColumnName(oldName, newName);
                if(oldValue != newValue){
                   iter.set(newValue);
                }
            }
            return this;
        }

        public SubstanceSpreadsheetExporter build(){

            if(publicOnly){
                ListIterator<ColumnValueRecipe<Substance>> iter = columns.listIterator();
                while(iter.hasNext()){

                    ColumnValueRecipe<Substance> value = iter.next();
                    
                    if(value instanceof DefaultSubstanceSpreadsheetExporterFactory.PublicRestrictable){
                        iter.set(((DefaultSubstanceSpreadsheetExporterFactory.PublicRestrictable) value).asPublicOnly());
                    }

                }

            }

            return new SubstanceSpreadsheetExporter(this);
        }

        public Builder includePublicDataOnly(boolean publicOnly){
            this.publicOnly = publicOnly;
            return this;
        }

    }


}