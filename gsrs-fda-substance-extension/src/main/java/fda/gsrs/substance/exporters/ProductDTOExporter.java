package fda.gsrs.substance.exporters;

import gov.hhs.gsrs.products.api.*;

import gsrs.module.substance.SubstanceEntityService;
import gsrs.springUtils.AutowireHelper;
import ix.core.EntityFetcher;
import ix.ginas.exporters.*;
import ix.ginas.models.v1.Substance;

import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.Query;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import java.io.IOException;
import java.util.*;

enum ProdDefaultColumns implements Column {
    INGREDIENT_NUMBER,
    SUBSTANCE_NAME,
    APPROVAL_ID,
    SUBSTANCE_KEY,
    INGREDIENT_TYPE,
    ACTIVE_MOIETY_NAME,
    ACTIVE_MOIETY_UNII,
    PROVENANCE,
    PRODUCT_ID,
    PRODUCT_CODE_TYPE,
    PRODUCT_NAME,
    PRODUCT_STATUS,
    PRODUCT_TYPE,
    ROUTE_OF_ADMINISTRATOR,
    DOSAGE_FORM_NAME,
    MARKETING_CATEGORY_NAME,
    APPLICATION_TYPE_NUMBER,
    IS_LISTED,
    LABELER_NAME,
    LABELER_CODE,
    LABELER_CODE_TYPE,
    LABELER_ADDRESS,
    LABELER_CITY,
    LABELER_STATE,
    LABELER_ZIP,
    LABELER_COUNTRY
}

@Slf4j
public class ProductDTOExporter implements Exporter<ProductDTO> {

    static final String CONST_ACTIVE_MOIETY = "ACTIVE MOIETY";

    private static SubstanceEntityService substanceEntityService;

    private final Spreadsheet spreadsheet;

    private int row = 1;
    private static int ingredientNumber = 0;

    private final List<ColumnValueRecipe<ProductDTO>> recipeMap;

    private static StringBuilder substanceApprovalIdSB;
    private static StringBuilder substanceActiveMoietySB;

    private ProductDTOExporter(Builder builder, SubstanceEntityService substanceEntityService) {

        this.substanceEntityService = substanceEntityService;
        substanceApprovalIdSB = new StringBuilder();
        substanceActiveMoietySB = new StringBuilder();

        this.spreadsheet = builder.spreadsheet;
        this.recipeMap = builder.columns;

        int j = 0;
        Spreadsheet.SpreadsheetRow header = spreadsheet.getRow(0);
        for (ColumnValueRecipe<ProductDTO> col : recipeMap) {
            j += col.writeHeaderValues(header, j);
        }
    }

    @Override
    public void export(ProductDTO p) throws IOException {

        /*****************************************************************************/
        // Export Product records and also display all the ingredients in each row
        /****************************************************************************/
        try {
            // Add one more column called "Ingredient Number" at the beginning.  Have it increment by one.
            // Each of these ingredients be new rows. Can duplicate the other product columns on each row.

            if (p.getProductManufactureItems().size() > 0) {
                for (ProductManufactureItemDTO prodManuItem : p.getProductManufactureItems()) {
                    for (ProductLotDTO prodLot : prodManuItem.getProductLots()) {

                        for (int i = 0; i < prodLot.getProductIngredients().size(); i++) {

                            Spreadsheet.SpreadsheetRow row = spreadsheet.getRow(this.row++);
                            int col = 0;
                            this.ingredientNumber = i;

                            for (ColumnValueRecipe<ProductDTO> recipe : recipeMap) {
                                col += recipe.writeValuesFor(row, col, p);
                            }

                        } // loop ProductIngredient
                    }  // loop ProductLot
                } // loop ProductManufacutureItem
            } // Ingredient size > 0
        } // try
        catch (Exception ex) {
            log.error("Error exporting Product record in Substance for Product ID: " + p.getId(), ex);
        }
    }

    @Override
    public void close() throws IOException {
        spreadsheet.close();
    }

    private static Map<Column, ColumnValueRecipe<ProductDTO>> DEFAULT_RECIPE_MAP;

    static {

        DEFAULT_RECIPE_MAP = new LinkedHashMap<>();

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.INGREDIENT_NUMBER, SingleColumnValueRecipe.create(ProdDefaultColumns.INGREDIENT_NUMBER, (p, cell) -> {
            int ingredNum = ingredientNumber + 1;
            cell.writeInteger((ingredNum));
        }));

        // Get Substance Name, Approval ID (UNII), Active Moiety, Substance Key, Ingredient Type
        getSubstanceKeyDetails();

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.PROVENANCE, SingleColumnValueRecipe.create(ProdDefaultColumns.PROVENANCE, (p, cell) -> {
            StringBuilder sb = getProductProvenanceDetails(p, ProdDefaultColumns.PROVENANCE);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.PRODUCT_ID, SingleColumnValueRecipe.create(ProdDefaultColumns.PRODUCT_ID, (p, cell) -> {
            StringBuilder sb = getProductCodeDetails(p, ProdDefaultColumns.PRODUCT_ID);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.PRODUCT_CODE_TYPE, SingleColumnValueRecipe.create(ProdDefaultColumns.PRODUCT_CODE_TYPE, (p, cell) -> {
            StringBuilder sb = getProductCodeDetails(p, ProdDefaultColumns.PRODUCT_CODE_TYPE);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.PRODUCT_NAME, SingleColumnValueRecipe.create(ProdDefaultColumns.PRODUCT_NAME, (s, cell) -> {
            StringBuilder sb = getProductNameDetails(s, ProdDefaultColumns.PRODUCT_NAME);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.PRODUCT_STATUS, SingleColumnValueRecipe.create(ProdDefaultColumns.PRODUCT_STATUS, (s, cell) -> {
            StringBuilder sb = getProductProvenanceDetails(s, ProdDefaultColumns.PRODUCT_STATUS);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.PRODUCT_TYPE, SingleColumnValueRecipe.create(ProdDefaultColumns.PRODUCT_TYPE, (s, cell) -> {
            StringBuilder sb = getProductProvenanceDetails(s, ProdDefaultColumns.PRODUCT_TYPE);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.ROUTE_OF_ADMINISTRATOR, SingleColumnValueRecipe.create(ProdDefaultColumns.ROUTE_OF_ADMINISTRATOR, (p, cell) -> cell.writeString(p.getRouteAdmin())));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.DOSAGE_FORM_NAME, SingleColumnValueRecipe.create(ProdDefaultColumns.DOSAGE_FORM_NAME, (s, cell) -> {
            StringBuilder sb = getManufactureItemDetails(s, ProdDefaultColumns.DOSAGE_FORM_NAME);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.MARKETING_CATEGORY_NAME, SingleColumnValueRecipe.create(ProdDefaultColumns.MARKETING_CATEGORY_NAME, (s, cell) -> {
            StringBuilder sb = getProductProvenanceDetails(s, ProdDefaultColumns.MARKETING_CATEGORY_NAME);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.APPLICATION_TYPE_NUMBER, SingleColumnValueRecipe.create(ProdDefaultColumns.APPLICATION_TYPE_NUMBER, (s, cell) -> {
            StringBuilder sb = getProductProvenanceDetails(s, ProdDefaultColumns.APPLICATION_TYPE_NUMBER);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.IS_LISTED, SingleColumnValueRecipe.create(ProdDefaultColumns.IS_LISTED, (s, cell) -> {
            StringBuilder sb = getProductProvenanceDetails(s, ProdDefaultColumns.IS_LISTED);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.LABELER_NAME, SingleColumnValueRecipe.create(ProdDefaultColumns.LABELER_NAME, (s, cell) -> {
            StringBuilder sb = getProductCompanyDetails(s, ProdDefaultColumns.LABELER_NAME);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.LABELER_CODE, SingleColumnValueRecipe.create(ProdDefaultColumns.LABELER_CODE, (s, cell) -> {
            StringBuilder sb = getProductCompanyDetails(s, ProdDefaultColumns.LABELER_CODE);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.LABELER_CODE_TYPE, SingleColumnValueRecipe.create(ProdDefaultColumns.LABELER_CODE_TYPE, (s, cell) -> {
            StringBuilder sb = getProductCompanyDetails(s, ProdDefaultColumns.LABELER_CODE_TYPE);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.LABELER_ADDRESS, SingleColumnValueRecipe.create(ProdDefaultColumns.LABELER_ADDRESS, (s, cell) -> {
            StringBuilder sb = getProductCompanyDetails(s, ProdDefaultColumns.LABELER_ADDRESS);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.LABELER_CITY, SingleColumnValueRecipe.create(ProdDefaultColumns.LABELER_CITY, (s, cell) -> {
            StringBuilder sb = getProductCompanyDetails(s, ProdDefaultColumns.LABELER_CITY);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.LABELER_STATE, SingleColumnValueRecipe.create(ProdDefaultColumns.LABELER_STATE, (s, cell) -> {
            StringBuilder sb = getProductCompanyDetails(s, ProdDefaultColumns.LABELER_STATE);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.LABELER_ZIP, SingleColumnValueRecipe.create(ProdDefaultColumns.LABELER_ZIP, (s, cell) -> {
            StringBuilder sb = getProductCompanyDetails(s, ProdDefaultColumns.LABELER_ZIP);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.LABELER_COUNTRY, SingleColumnValueRecipe.create(ProdDefaultColumns.LABELER_COUNTRY, (s, cell) -> {
            StringBuilder sb = getProductCompanyDetails(s, ProdDefaultColumns.LABELER_COUNTRY);
            cell.writeString(sb.toString());
        }));
    }

    private static StringBuilder getProductProvenanceDetails(ProductDTO p, ProdDefaultColumns fieldName) {
        StringBuilder sb = new StringBuilder();

        if (p.getProductProvenances().size() > 0) {
            for (ProductProvenanceDTO prodProv : p.getProductProvenances()) {

                if (sb.length() != 0) {
                    sb.append("|");
                }

                switch (fieldName) {
                    case PROVENANCE:
                        sb.append((prodProv.getProvenance() != null) ? prodProv.getProvenance() : "");
                        break;
                    case PRODUCT_STATUS:
                        sb.append((prodProv.getProductStatus() != null) ? prodProv.getProductStatus() : "");
                        break;
                    case PRODUCT_TYPE:
                        sb.append((prodProv.getProductType() != null) ? prodProv.getProductType() : "");
                        break;
                    case MARKETING_CATEGORY_NAME:
                        sb.append((prodProv.getMarketingCategoryName() != null) ? prodProv.getMarketingCategoryName() : "");
                        break;
                    case IS_LISTED:
                        sb.append((prodProv.getIsListed() != null) ? prodProv.getIsListed() : "");
                        break;
                    case APPLICATION_TYPE_NUMBER:
                        sb.append((prodProv.getApplicationType() != null) ? prodProv.getApplicationType() + " " : "");
                        sb.append((prodProv.getApplicationNumber() != null) ? prodProv.getApplicationNumber() : "");
                        break;
                    default:
                        break;
                }
            }
        }
        return sb;
    }

    private static StringBuilder getProductNameDetails(ProductDTO p, ProdDefaultColumns fieldName) {
        StringBuilder sb = new StringBuilder();

        if (p.getProductProvenances().size() > 0) {
            for (ProductProvenanceDTO prodProv : p.getProductProvenances()) {
                for (ProductNameDTO prodName : prodProv.getProductNames()) {

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
                } // Product Names
            } // Product Provenance
        }
        return sb;
    }

    private static StringBuilder getProductCodeDetails(ProductDTO p, ProdDefaultColumns fieldName) {
        StringBuilder sb = new StringBuilder();

        if (p.getProductProvenances().size() > 0) {
            for (ProductProvenanceDTO prodProv : p.getProductProvenances()) {
                for (ProductCodeDTO prodCode : prodProv.getProductCodes()) {

                    if (sb.length() != 0) {
                        sb.append("|");
                    }

                    switch (fieldName) {
                        case PRODUCT_ID:
                            sb.append((prodCode.getProductCode() != null) ? prodCode.getProductCode() : "");
                            break;
                        case PRODUCT_CODE_TYPE:
                            sb.append((prodCode.getProductCodeType() != null) ? prodCode.getProductCodeType() : "");
                            break;
                        default:
                            break;
                    } // switch
                }
            }
        }
        return sb;
    }

    private static StringBuilder getProductCompanyDetails(ProductDTO p, ProdDefaultColumns fieldName) {
        StringBuilder sb = new StringBuilder();

        if (p.getProductProvenances().size() > 0) {
            for (ProductProvenanceDTO prodProv : p.getProductProvenances()) {
                for (ProductCompanyDTO prodComp : prodProv.getProductCompanies()) {

                    if (sb.length() != 0) {
                        sb.append("|");
                    }

                    switch (fieldName) {
                        case LABELER_NAME:
                            sb.append((prodComp.getCompanyName() != null) ? prodComp.getCompanyName() : "");
                            break;
                        case LABELER_ADDRESS:
                            sb.append((prodComp.getCompanyAddress() != null) ? prodComp.getCompanyAddress() : "");
                            break;
                        case LABELER_CITY:
                            sb.append((prodComp.getCompanyCity() != null) ? prodComp.getCompanyCity() : "");
                            break;
                        case LABELER_STATE:
                            sb.append((prodComp.getCompanyState() != null) ? prodComp.getCompanyState() : "");
                            break;
                        case LABELER_ZIP:
                            sb.append((prodComp.getCompanyZip() != null) ? prodComp.getCompanyZip() : "");
                            break;
                        case LABELER_COUNTRY:
                            sb.append((prodComp.getCompanyCountry() != null) ? prodComp.getCompanyCountry() : "");
                            break;
                        case LABELER_CODE:
                            for (ProductCompanyCodeDTO prodCompCode : prodComp.getProductCompanyCodes()) {
                                sb.append((prodCompCode.getCompanyCode() != null) ? prodCompCode.getCompanyCode() : "(No Labeler Code)");
                            }
                            break;
                        case LABELER_CODE_TYPE:
                            for (ProductCompanyCodeDTO prodCompCode : prodComp.getProductCompanyCodes()) {
                                sb.append((prodCompCode.getCompanyCodeType() != null) ? prodCompCode.getCompanyCodeType() : "(No Labeler Code Type)");
                            }
                            break;
                        default:
                            break;
                    } // switch
                } // loop Product Companies
            } // loop Product Provenance
        }

        return sb;
    }

    private static StringBuilder getManufactureItemDetails(ProductDTO p, ProdDefaultColumns fieldName) {
        StringBuilder sb = new StringBuilder();

        try {
            if (p.getProductManufactureItems().size() > 0) {
                for (ProductManufactureItemDTO prodManuItem : p.getProductManufactureItems()) {
                    switch (fieldName) {
                        case DOSAGE_FORM_NAME:
                            sb.append((prodManuItem.getDosageForm() != null) ? prodManuItem.getDosageForm() : "");
                            break;
                        default:
                            break;
                    } // switch
                }
            }
        } catch (
                Exception ex) {
            ex.printStackTrace();
        }

        return sb;
    }

    private static void getSubstanceKeyDetails() {

        StringBuilder nameSb = new StringBuilder();
        StringBuilder approvalIdSb = new StringBuilder();
        StringBuilder substanceKeySb = new StringBuilder();
        StringBuilder ingredientTypeSb = new StringBuilder();
        StringBuilder substanceNameSb = new StringBuilder();
        StringBuilder substanceApprovalIdSb = new StringBuilder();
        StringBuilder substanceActiveMoietySb = new StringBuilder();
        StringBuilder substanceActiveMoietyApprovalIdSb = new StringBuilder();

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.SUBSTANCE_NAME, SingleColumnValueRecipe.create(ProdDefaultColumns.SUBSTANCE_NAME, (p, cell) -> {

            nameSb.setLength(0);
            approvalIdSb.setLength(0);
            substanceKeySb.setLength(0);
            ingredientTypeSb.setLength(0);
            substanceNameSb.setLength(0);
            substanceApprovalIdSb.setLength(0);
            substanceActiveMoietySb.setLength(0);

            try {
                if (p.getProductManufactureItems().size() > 0) {
                    for (int i = 0; i < p.getProductManufactureItems().size(); i++) {
                        ProductManufactureItemDTO prodManuItem = p.getProductManufactureItems().get(i);
                        for (int j = 0; j < prodManuItem.getProductLots().size(); j++) {
                            ProductLotDTO prodLot = prodManuItem.getProductLots().get(j);

                            ProductIngredientDTO ingred = prodLot.getProductIngredients().get(ingredientNumber);

                            // Get Substance Details
                            if (ingred.getSubstanceKey() != null) {

                                substanceKeySb.append((ingred.getSubstanceKey() != null) ? ingred.getSubstanceKey() : "");
                                ingredientTypeSb.append((ingred.getIngredientType() != null) ? ingred.getIngredientType() : "");

                                // Call Substance Entity Service to get Substance Object by Substance Key
                                if (substanceEntityService != null) {
                                    Optional<Substance> substance = substanceEntityService.flexLookup(ingred.getSubstanceKey());

                                    if (substance != null) {

                                        nameSb.append(substance.get().getName());
                                        approvalIdSb.append(substance.get().approvalID);

                                        // Get Substance Name
                                        substanceNameSb.append((nameSb.toString() != null) ? nameSb.toString() : "");

                                        // Storing in static variable so do not have to call the same Substance API twice just to get
                                        // approval Id.
                                        substanceApprovalIdSb.append((approvalIdSb.toString() != null) ? approvalIdSb.toString() : "");

                                        if (substance.get().getActiveMoieties().size() > 0) {
                                            substance.get().getActiveMoieties().forEach(relationship -> {
                                                if ((relationship.type != null) && (relationship.type.equalsIgnoreCase(CONST_ACTIVE_MOIETY))) {
                                                    if (relationship.relatedSubstance != null) {
                                                        substanceActiveMoietySb.append(relationship.relatedSubstance.refPname != null ? relationship.relatedSubstance.refPname : "");
                                                        substanceActiveMoietyApprovalIdSb.append(relationship.relatedSubstance.approvalID != null ? relationship.relatedSubstance.approvalID : "");
                                                    }
                                                }
                                            });
                                        }
                                    } // Substance is not null
                                } // substanceEntityService is not null

                            } else {   // No Substance Key
                                substanceKeySb.append("");
                                substanceNameSb.append("");
                                substanceApprovalIdSb.append("");
                            }
                        }  // for loop productLots
                    } // for loop productManufactureItems
                } // if size > 0
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            cell.writeString(substanceNameSb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.APPROVAL_ID, SingleColumnValueRecipe.create(ProdDefaultColumns.APPROVAL_ID, (p, cell) -> {
            cell.writeString(substanceApprovalIdSb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.SUBSTANCE_KEY, SingleColumnValueRecipe.create(ProdDefaultColumns.SUBSTANCE_KEY, (p, cell) -> {
            cell.writeString(substanceKeySb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.INGREDIENT_TYPE, SingleColumnValueRecipe.create(ProdDefaultColumns.INGREDIENT_TYPE, (p, cell) -> {
            cell.writeString(ingredientTypeSb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.ACTIVE_MOIETY_NAME, SingleColumnValueRecipe.create(ProdDefaultColumns.ACTIVE_MOIETY_NAME, (s, cell) -> {
            cell.writeString(substanceActiveMoietySb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(ProdDefaultColumns.ACTIVE_MOIETY_UNII, SingleColumnValueRecipe.create(ProdDefaultColumns.ACTIVE_MOIETY_UNII, (s, cell) -> {
            cell.writeString(substanceActiveMoietyApprovalIdSb.toString());
        }));
    }

    /**
     * Builder class that makes a SpreadsheetExporter.  By basic, the basic columns are used
     * but these may be modified using the add/remove column methods.
     */
    public static class Builder {
        private final List<ColumnValueRecipe<ProductDTO>> columns = new ArrayList<>();
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

            for (Map.Entry<Column, ColumnValueRecipe<ProductDTO>> entry : DEFAULT_RECIPE_MAP.entrySet()) {
                columns.add(entry.getValue());
            }
        }

        public Builder addColumn(Column column, ColumnValueRecipe<ProductDTO> recipe) {
            return addColumn(column.name(), recipe);
        }

        public Builder addColumn(String columnName, ColumnValueRecipe<ProductDTO> recipe) {
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
            ListIterator<ColumnValueRecipe<ProductDTO>> iter = columns.listIterator();
            while (iter.hasNext()) {

                ColumnValueRecipe<ProductDTO> oldValue = iter.next();
                ColumnValueRecipe<ProductDTO> newValue = oldValue.replaceColumnName(oldName, newName);
                if (oldValue != newValue) {
                    iter.set(newValue);
                }
            }
            return this;
        }

        public ProductDTOExporter build(SubstanceEntityService substanceEntityService) {
            return new ProductDTOExporter(this, substanceEntityService);
        }

        public Builder includePublicDataOnly(boolean publicOnly) {
            this.publicOnly = publicOnly;
            return this;
        }

    }
}