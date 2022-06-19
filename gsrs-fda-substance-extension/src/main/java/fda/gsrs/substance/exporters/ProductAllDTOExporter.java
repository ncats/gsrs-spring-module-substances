package fda.gsrs.substance.exporters;

import gsrs.module.substance.SubstanceEntityService;
import gsrs.springUtils.AutowireHelper;
import org.springframework.beans.factory.annotation.Autowired;
import ix.ginas.exporters.*;

import gov.hhs.gsrs.products.api.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import java.io.IOException;
import java.util.*;

enum ProdAllDefaultColumns implements Column {
    PRODUCT_ID,
    PRODUCT_NAME,
    NON_PROPRIETARY_NAME,
    STATUS,
    ROUTE_OF_ADMINISTRATOR,
    PROVENANCE,
    IS_LISTED,
    LABELER_NAME,
    LABELER_DUNS,
    LABELER_ADDRESS,
    LABELER_CITY,
    LABELER_STATE,
    LABELER_ZIP,
    LABELER_COUNTRY,
    ACTIVE_MOIETY_NAME,
    ACTIVE_MOIETY_UNII,
    INGREDIENT_TYPE,
    APPLICATION_NUMBER,
    DOSAGE_FORM_NAME,
    MARKETING_CATEGORY_NAME,
    PRODUCT_TYPE,
    INGREDIENT_NUMBER,
    SUBSTANCE_NAME,
    APPROVAL_ID,
    SUBSTANCE_KEY
}

public class ProductAllDTOExporter implements Exporter<ProductMainAllDTO> {

    private final Spreadsheet spreadsheet;

    private int row = 1;
    private static int ingredientNumber = 0;

    private final List<ColumnValueRecipe<ProductMainAllDTO>> recipeMap;

    private static SubstanceEntityService substanceEntityService;

    private static StringBuilder substanceApprovalIdSB;
    private static StringBuilder substanceActiveMoietySB;

    private ProductAllDTOExporter(Builder builder, SubstanceEntityService substanceEntityService) {

        this.substanceEntityService = substanceEntityService;
        substanceApprovalIdSB = new StringBuilder();
        substanceActiveMoietySB = new StringBuilder();

        this.spreadsheet = builder.spreadsheet;
        this.recipeMap = builder.columns;

        int j = 0;
        Spreadsheet.SpreadsheetRow header = spreadsheet.getRow(0);
        for (ColumnValueRecipe<ProductMainAllDTO> col : recipeMap) {
            j += col.writeHeaderValues(header, j);
        }
    }

    @Override
    public void export(ProductMainAllDTO s) throws IOException {
        /*****************************************************************************/
        // Export Product records and also display all the ingredients in each row
        /****************************************************************************/
        try {
            // Add one more column called "Ingredient Number" at the beginning.  Have it increment by one.
            // Each of these ingredients be new rows. Can duplicate the other product columns on each row.
            if (s.getProductIngredientAllList().size() > 0) {
                for (int i = 0; i < s.getProductIngredientAllList().size(); i++) {

                    Spreadsheet.SpreadsheetRow row = spreadsheet.getRow(this.row++);
                    int col = 0;
                    this.ingredientNumber = i;

                    for (ColumnValueRecipe<ProductMainAllDTO> recipe : recipeMap) {
                        col += recipe.writeValuesFor(row, col, s);
                    }
                } // for ProductIngredient
            } // Ingredient size > 0
        } // try
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        spreadsheet.close();
    }

    private static Map<Column, ColumnValueRecipe<ProductMainAllDTO>> DEFAULT_RECIPE_MAP;

    static {

        DEFAULT_RECIPE_MAP = new LinkedHashMap<>();

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.INGREDIENT_NUMBER, SingleColumnValueRecipe.create(ProdAllDefaultColumns.INGREDIENT_NUMBER, (s, cell) -> {
            int ingredNum = ingredientNumber + 1;
            cell.writeInteger((ingredNum));
        }));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.SUBSTANCE_NAME, SingleColumnValueRecipe.create(ProdAllDefaultColumns.SUBSTANCE_NAME, (s, cell) -> {
            StringBuilder sb = getIngredientDetails(s, ProdAllDefaultColumns.SUBSTANCE_NAME);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.APPROVAL_ID, SingleColumnValueRecipe.create(ProdAllDefaultColumns.APPROVAL_ID, (s, cell) -> {
            StringBuilder sb = getIngredientDetails(s, ProdAllDefaultColumns.APPROVAL_ID);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.SUBSTANCE_KEY, SingleColumnValueRecipe.create(ProdAllDefaultColumns.SUBSTANCE_KEY, (s, cell) -> {
            StringBuilder sb = getIngredientDetails(s, ProdAllDefaultColumns.SUBSTANCE_KEY);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.INGREDIENT_TYPE, SingleColumnValueRecipe.create(ProdAllDefaultColumns.INGREDIENT_TYPE, (s, cell) -> {
            StringBuilder sb = getIngredientDetails(s, ProdAllDefaultColumns.INGREDIENT_TYPE);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.ACTIVE_MOIETY_NAME, SingleColumnValueRecipe.create(ProdAllDefaultColumns.ACTIVE_MOIETY_NAME, (s, cell) -> {
            StringBuilder sb = getIngredientDetails(s, ProdAllDefaultColumns.ACTIVE_MOIETY_NAME);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.ACTIVE_MOIETY_UNII, SingleColumnValueRecipe.create(ProdAllDefaultColumns.ACTIVE_MOIETY_UNII, (s, cell) -> {
            StringBuilder sb = getIngredientDetails(s, ProdAllDefaultColumns.ACTIVE_MOIETY_UNII);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.PRODUCT_ID, SingleColumnValueRecipe.create(ProdAllDefaultColumns.PRODUCT_ID, (s, cell) -> cell.writeString(s.getProductNDC())));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.PRODUCT_NAME, SingleColumnValueRecipe.create(ProdAllDefaultColumns.PRODUCT_NAME, (s, cell) -> {
            StringBuilder sb = getProductNameDetails(s, ProdAllDefaultColumns.PRODUCT_NAME);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.NON_PROPRIETARY_NAME, SingleColumnValueRecipe.create(ProdAllDefaultColumns.NON_PROPRIETARY_NAME, (s, cell) -> cell.writeString(s.getNonProprietaryName())));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.STATUS, SingleColumnValueRecipe.create(ProdAllDefaultColumns.STATUS, (s, cell) -> cell.writeString(s.getStatus())));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.PRODUCT_TYPE, SingleColumnValueRecipe.create(ProdAllDefaultColumns.PRODUCT_TYPE, (s, cell) -> cell.writeString(s.getProductType())));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.ROUTE_OF_ADMINISTRATOR, SingleColumnValueRecipe.create(ProdAllDefaultColumns.ROUTE_OF_ADMINISTRATOR, (s, cell) -> cell.writeString(s.getRouteName())));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.DOSAGE_FORM_NAME, SingleColumnValueRecipe.create(ProdAllDefaultColumns.DOSAGE_FORM_NAME, (s, cell) -> {
            StringBuilder sb = getIngredientDetails(s, ProdAllDefaultColumns.DOSAGE_FORM_NAME);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.MARKETING_CATEGORY_NAME, SingleColumnValueRecipe.create(ProdAllDefaultColumns.MARKETING_CATEGORY_NAME, (s, cell) -> cell.writeString(s.getMarketingCategoryName())));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.APPLICATION_NUMBER, SingleColumnValueRecipe.create(ProdAllDefaultColumns.APPLICATION_NUMBER, (s, cell) -> cell.writeString(s.getAppTypeNumber())));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.IS_LISTED, SingleColumnValueRecipe.create(ProdAllDefaultColumns.IS_LISTED, (s, cell) -> cell.writeString(s.getIsListed())));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.LABELER_NAME, SingleColumnValueRecipe.create(ProdAllDefaultColumns.LABELER_NAME, (s, cell) -> {
            StringBuilder sb = getProductCompanyDetails(s, ProdAllDefaultColumns.LABELER_NAME);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.LABELER_DUNS, SingleColumnValueRecipe.create(ProdAllDefaultColumns.LABELER_DUNS, (s, cell) -> {
            StringBuilder sb = getProductCompanyDetails(s, ProdAllDefaultColumns.LABELER_DUNS);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.LABELER_ADDRESS, SingleColumnValueRecipe.create(ProdAllDefaultColumns.LABELER_ADDRESS, (s, cell) -> {
            StringBuilder sb = getProductCompanyDetails(s, ProdAllDefaultColumns.LABELER_ADDRESS);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.LABELER_CITY, SingleColumnValueRecipe.create(ProdAllDefaultColumns.LABELER_CITY, (s, cell) -> {
            StringBuilder sb = getProductCompanyDetails(s, ProdAllDefaultColumns.LABELER_CITY);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.LABELER_STATE, SingleColumnValueRecipe.create(ProdAllDefaultColumns.LABELER_STATE, (s, cell) -> {
            StringBuilder sb = getProductCompanyDetails(s, ProdAllDefaultColumns.LABELER_STATE);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.LABELER_ZIP, SingleColumnValueRecipe.create(ProdAllDefaultColumns.LABELER_ZIP, (s, cell) -> {
            StringBuilder sb = getProductCompanyDetails(s, ProdAllDefaultColumns.LABELER_ZIP);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.LABELER_COUNTRY, SingleColumnValueRecipe.create(ProdAllDefaultColumns.LABELER_COUNTRY, (s, cell) -> {
            StringBuilder sb = getProductCompanyDetails(s, ProdAllDefaultColumns.LABELER_COUNTRY);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdAllDefaultColumns.PROVENANCE, SingleColumnValueRecipe.create(ProdAllDefaultColumns.PROVENANCE, (s, cell) -> cell.writeString(s.getProvenance())));

    }

    private static StringBuilder getProductNameDetails(ProductMainAllDTO s, ProdAllDefaultColumns fieldName) {
        StringBuilder sb = new StringBuilder();

        if (s.getProductNameAllList().size() > 0) {

            for (ProductNameAllDTO prodName : s.getProductNameAllList()) {
                if (sb.length() != 0) {
                    sb.append("|");
                }
                switch (fieldName) {
                    case PRODUCT_NAME:
                        sb.append((prodName.getProductName() != null) ? prodName.getProductName() : "(No Product Name)");
                        break;
                    default:
                        break;
                }
            }
        }
        return sb;
    }

    private static StringBuilder getProductCompanyDetails(ProductMainAllDTO s, ProdAllDefaultColumns fieldName) {
        StringBuilder sb = new StringBuilder();

        if (s.getProductCompanyAllList().size() > 0) {

            for (ProductCompanyAllDTO prodComp : s.getProductCompanyAllList()) {
                if (sb.length() != 0) {
                    sb.append("|");
                }
                switch (fieldName) {
                    case LABELER_NAME:
                        sb.append((prodComp.getLabelerName() != null) ? prodComp.getLabelerName() : "");
                        break;
                    case LABELER_DUNS:
                        sb.append((prodComp.getLabelerDuns() != null) ? prodComp.getLabelerDuns() : "");
                        break;
                    case LABELER_ADDRESS:
                        sb.append((prodComp.getAddress() != null) ? prodComp.getAddress() : "");
                        break;
                    case LABELER_CITY:
                        sb.append((prodComp.getCity() != null) ? prodComp.getCity() : "");
                        break;
                    case LABELER_STATE:
                        sb.append((prodComp.getState() != null) ? prodComp.getState() : "");
                        break;
                    case LABELER_ZIP:
                        sb.append((prodComp.getZip() != null) ? prodComp.getZip() : "");
                        break;
                    case LABELER_COUNTRY:
                        sb.append((s.getCountryWithoutCode() != null) ? s.getCountryWithoutCode() : "");
                        break;
                    default:
                        break;
                }
            }
        }
        return sb;
    }

    private static StringBuilder getIngredientDetails(ProductMainAllDTO s, ProdAllDefaultColumns fieldName) {
        StringBuilder sb = new StringBuilder();

        try {
            if (s.getProductIngredientAllList().size() > 0) {

                ProductIngredientAllDTO ingred = s.getProductIngredientAllList().get(ingredientNumber);

                switch (fieldName) {
                    case SUBSTANCE_NAME:
                        sb.append((ingred.getSubstanceName() != null) ? ingred.getSubstanceName() : "");
                        break;
                    case APPROVAL_ID:
                        sb.append((ingred.getSubstanceApprovalId() != null) ? ingred.getSubstanceApprovalId() : "");
                        break;
                    case SUBSTANCE_KEY:
                        sb.append((ingred.getSubstanceKey() != null) ? ingred.getSubstanceKey() : "");
                        break;
                    case INGREDIENT_TYPE:
                        sb.append((ingred.getIngredientType() != null) ? ingred.getIngredientType() : "");
                        break;
                    case ACTIVE_MOIETY_NAME:
                        sb.append((ingred.getActiveMoietyName() != null) ? ingred.getActiveMoietyName() : "");
                        break;
                    case ACTIVE_MOIETY_UNII:
                        sb.append((ingred.getActiveMoietyUnii() != null) ? ingred.getActiveMoietyUnii() : "");
                        break;
                    case DOSAGE_FORM_NAME:
                        sb.append((ingred.getDosageFormName() != null) ? ingred.getDosageFormName() : "");
                        break;
                    default:
                        break;
                }
                // }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return sb;
    }

    /**
     * Builder class that makes a SpreadsheetExporter.  By default, the default columns are used
     * but these may be modified using the add/remove column methods.
     */
    public static class Builder {
        private final List<ColumnValueRecipe<ProductMainAllDTO>> columns = new ArrayList<>();
        private final Spreadsheet spreadsheet;

        private boolean publicOnly = false;

        /**
         * Create a new Builder that uses the given Spreadsheet to write to.
         *
         * @param spreadSheet the {@link Spreadsheet} object that will be written to by this exporter. can not be null.
         * @throws NullPointerException if spreadsheet is null.
         */
        public Builder(Spreadsheet spreadSheet) {
            Objects.requireNonNull(spreadSheet);
            this.spreadsheet = spreadSheet;

            for (Map.Entry<Column, ColumnValueRecipe<ProductMainAllDTO>> entry : DEFAULT_RECIPE_MAP.entrySet()) {
                columns.add(entry.getValue());
            }
        }

        public Builder addColumn(Column column, ColumnValueRecipe<ProductMainAllDTO> recipe) {
            return addColumn(column.name(), recipe);
        }

        public Builder addColumn(String columnName, ColumnValueRecipe<ProductMainAllDTO> recipe) {
            Objects.requireNonNull(columnName);
            Objects.requireNonNull(recipe);
            columns.add(recipe);

            return this;
        }

        public Builder renameColumn(Column oldColumn, String newName) {
            return renameColumn(oldColumn.name(), newName);
        }

        public Builder renameColumn(String oldName, String newName) {
            //use iterator to preserve order
            ListIterator<ColumnValueRecipe<ProductMainAllDTO>> iter = columns.listIterator();
            while (iter.hasNext()) {

                ColumnValueRecipe<ProductMainAllDTO> oldValue = iter.next();
                ColumnValueRecipe<ProductMainAllDTO> newValue = oldValue.replaceColumnName(oldName, newName);
                if (oldValue != newValue) {
                    iter.set(newValue);
                }
            }
            return this;
        }

        public ProductAllDTOExporter build(SubstanceEntityService substanceEntityService) {
            return new ProductAllDTOExporter(this, substanceEntityService);
        }

        public Builder includePublicDataOnly(boolean publicOnly) {
            this.publicOnly = publicOnly;
            return this;
        }

    }
}