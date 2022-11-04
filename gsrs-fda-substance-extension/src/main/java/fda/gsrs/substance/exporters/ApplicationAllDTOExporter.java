package fda.gsrs.substance.exporters;

import gsrs.module.substance.SubstanceEntityService;
import gsrs.springUtils.AutowireHelper;
import org.springframework.beans.factory.annotation.Autowired;
import ix.ginas.exporters.*;
import gsrs.module.substance.controllers.SubstanceController;
import ix.ginas.models.v1.Substance;

import gov.hhs.gsrs.applications.api.ApplicationAllDTO;
import gov.hhs.gsrs.applications.api.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import java.io.IOException;
import java.util.*;

enum AppAllDefaultColumns implements Column {
    ID,
    APP_TYPE,
    APP_NUMBER,
    CENTER,
    TITLE,
    SPONSOR_NAME,
    APP_STATUS,
    APP_SUB_TYPE,
    DIVISION_CLASS_DESC,
    STATUS_DATE,
    SUBMIT_DATE,
    NONPROPRIETARY_NAME,
    DOSAGE_FORM,
    ROUTE_OF_ADMINISTRATION,
    PROVENANCE,
    PRODUCT_NAME,
    APPLICANT_INGREDIENT_NAME,
    SUBSTANCE_KEY,
    APPROVAL_ID,
    SUBSTANCE_NAME,
    ACTIVE_MOIETY,
    INGREDIENT_TYPE,
    INDICATION,
    FROM_TABLE
}

public class ApplicationAllDTOExporter implements Exporter<ApplicationAllDTO> {


    private final Spreadsheet spreadsheet;

    private int row = 1;

    private final List<ColumnValueRecipe<ApplicationAllDTO>> recipeMap;

    private static SubstanceEntityService substanceEntityService;

    private static StringBuilder substanceApprovalIdSB;
    private static StringBuilder substanceActiveMoietySB;

    private ApplicationAllDTOExporter(Builder builder, SubstanceEntityService substanceEntityService) {

        this.substanceEntityService = substanceEntityService;
        substanceApprovalIdSB = new StringBuilder();
        substanceActiveMoietySB = new StringBuilder();

        this.spreadsheet = builder.spreadsheet;
        this.recipeMap = builder.columns;

        int j = 0;
        Spreadsheet.SpreadsheetRow header = spreadsheet.getRow(0);
        for (ColumnValueRecipe<ApplicationAllDTO> col : recipeMap) {
            j += col.writeHeaderValues(header, j);
        }
    }

    @Override
    public void export(ApplicationAllDTO s) throws IOException {
        Spreadsheet.SpreadsheetRow row = spreadsheet.getRow(this.row++);

        int j = 0;
        for (ColumnValueRecipe<ApplicationAllDTO> recipe : recipeMap) {
            j += recipe.writeValuesFor(row, j, s);
        }
    }

    @Override
    public void close() throws IOException {
        spreadsheet.close();
    }

    private static Map<Column, ColumnValueRecipe<ApplicationAllDTO>> DEFAULT_RECIPE_MAP;

    static {

        DEFAULT_RECIPE_MAP = new LinkedHashMap<>();

        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.APPLICANT_INGREDIENT_NAME, SingleColumnValueRecipe.create(AppAllDefaultColumns.APPLICANT_INGREDIENT_NAME ,(s, cell) ->{
            StringBuilder sb = getIngredientDetails(s, AppAllDefaultColumns.APPLICANT_INGREDIENT_NAME);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.SUBSTANCE_NAME, SingleColumnValueRecipe.create(AppAllDefaultColumns.SUBSTANCE_NAME ,(s, cell) ->{
            StringBuilder sb = getIngredientName(s);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.APPROVAL_ID, SingleColumnValueRecipe.create(AppAllDefaultColumns.APPROVAL_ID ,(s, cell) ->{
            cell.writeString(substanceApprovalIdSB.toString());
        }));

        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.SUBSTANCE_KEY, SingleColumnValueRecipe.create( AppAllDefaultColumns.SUBSTANCE_KEY ,(s, cell) ->{
            StringBuilder sb = getIngredientDetails(s, AppAllDefaultColumns.SUBSTANCE_KEY);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.INGREDIENT_TYPE, SingleColumnValueRecipe.create( AppAllDefaultColumns.INGREDIENT_TYPE ,(s, cell) ->{
            StringBuilder sb = getIngredientDetails(s, AppAllDefaultColumns.INGREDIENT_TYPE);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.CENTER, SingleColumnValueRecipe.create(AppAllDefaultColumns.CENTER, (s, cell) -> cell.writeString(s.getCenter())));
        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.APP_TYPE, SingleColumnValueRecipe.create(AppAllDefaultColumns.APP_TYPE, (s, cell) -> cell.writeString(s.getAppType())));
        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.APP_NUMBER, SingleColumnValueRecipe.create(AppAllDefaultColumns.APP_NUMBER, (s, cell) -> cell.writeString(s.getAppNumber())));
        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.TITLE, SingleColumnValueRecipe.create(AppAllDefaultColumns.TITLE, (s, cell) -> cell.writeString(s.getTitle())));
        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.SPONSOR_NAME, SingleColumnValueRecipe.create(AppAllDefaultColumns.SPONSOR_NAME, (s, cell) -> cell.writeString(s.getSponsorName())));
        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.APP_STATUS, SingleColumnValueRecipe.create(AppAllDefaultColumns.APP_STATUS, (s, cell) -> cell.writeString(s.getAppStatus())));
        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.STATUS_DATE, SingleColumnValueRecipe.create( AppAllDefaultColumns.STATUS_DATE ,(s, cell) -> cell.writeString(s.getStatusDate())));
        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.SUBMIT_DATE, SingleColumnValueRecipe.create( AppAllDefaultColumns.SUBMIT_DATE ,(s, cell) -> cell.writeString(s.getSubmitDate())));
        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.NONPROPRIETARY_NAME, SingleColumnValueRecipe.create( AppAllDefaultColumns.NONPROPRIETARY_NAME ,(s, cell) -> cell.writeString(s.getNonProprietaryName())));

        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.PRODUCT_NAME, SingleColumnValueRecipe.create(AppAllDefaultColumns.PRODUCT_NAME, (s, cell) -> {
            StringBuilder sb = getProductDetails(s, AppAllDefaultColumns.PRODUCT_NAME);
            cell.writeString(sb.toString());
        }));
        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.DOSAGE_FORM, SingleColumnValueRecipe.create( AppAllDefaultColumns.DOSAGE_FORM ,(s, cell) ->{
            StringBuilder sb = getProductDetails(s, AppAllDefaultColumns.DOSAGE_FORM);
            cell.writeString(sb.toString());
        }));
        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.ROUTE_OF_ADMINISTRATION, SingleColumnValueRecipe.create( AppAllDefaultColumns.ROUTE_OF_ADMINISTRATION ,(s, cell) ->{
            StringBuilder sb = getProductDetails(s, AppAllDefaultColumns.ROUTE_OF_ADMINISTRATION);
            cell.writeString(sb.toString());
        }));

        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.APP_SUB_TYPE, SingleColumnValueRecipe.create(AppAllDefaultColumns.APP_SUB_TYPE, (s, cell) -> cell.writeString(s.getAppSubType())));
        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.DIVISION_CLASS_DESC, SingleColumnValueRecipe.create(AppAllDefaultColumns.DIVISION_CLASS_DESC, (s, cell) -> cell.writeString(s.getDivisionClassDesc())));
        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.PROVENANCE, SingleColumnValueRecipe.create(AppAllDefaultColumns.PROVENANCE, (s, cell) -> cell.writeString(s.getProvenance())));

        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.FROM_TABLE, SingleColumnValueRecipe.create( AppAllDefaultColumns.FROM_TABLE ,(s, cell) -> {
            String fromTable = null;
            if (s.getFromTable() != null) {
                if (s.getFromTable().equalsIgnoreCase("Integrity")) {
                    fromTable = "Integrity/DARRTS";
                }
                else {
                    fromTable = s.getFromTable();
                }
            }
            cell.writeString(fromTable);
        }));

        DEFAULT_RECIPE_MAP.put(AppAllDefaultColumns.INDICATION, SingleColumnValueRecipe.create(AppAllDefaultColumns.INDICATION, (s, cell) -> {
            StringBuilder sb = getIndicationDetails(s);
            cell.writeString(sb.toString());
        }));
    }

    private static StringBuilder getProductDetails(ApplicationAllDTO s, AppAllDefaultColumns fieldName) {
        StringBuilder sb = new StringBuilder();

        if (s.getApplicationProductList().size() > 0) {
            List<ProductSrsAllDTO> prodList = s.getApplicationProductList();

            for (ProductSrsAllDTO prod : prodList) {

                for (ProductNameSrsAllDTO prodName : prod.getApplicationProductNameList()) {
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
                } // for ProductNameSrsAll

                switch (fieldName) {
                    case DOSAGE_FORM:
                        sb.append((prod.getDosageForm() != null) ? prod.getDosageForm() : "(No Dosage Form)");
                        break;
                    case ROUTE_OF_ADMINISTRATION:
                        sb.append((prod.getRouteAdmin() != null) ? prod.getRouteAdmin() : "(No Route of Admin)");
                        break;
                    default:
                        break;
                } // Product switch
            } // for ProductSrsAll
        }
        return sb;
    }

    private static StringBuilder getIngredientName(ApplicationAllDTO s) {
        StringBuilder sb = new StringBuilder();
        substanceApprovalIdSB.setLength(0);
        String substanceName = null;
        String approvalId = null;

        try {
            if (s.getApplicationProductList().size() > 0) {
                List<ProductSrsAllDTO> prodList = s.getApplicationProductList();

                for (ProductSrsAllDTO prod : prodList) {

                    for (AppIngredientAllDTO ingred : prod.getApplicationIngredientList()) {

                        if (sb.length() != 0) {
                            sb.append("|");
                        }

                        if (substanceApprovalIdSB.length() != 0) {
                            substanceApprovalIdSB.append("|");
                        }
                        if (substanceActiveMoietySB.length() != 0) {
                            substanceActiveMoietySB.append("|");
                        }

                        // Get Substance Details
                        if (ingred.getSubstanceKey() != null) {

                            // Call Substance Entity Service to get Substance Object by Substance Key
                            if (substanceEntityService != null) {
                                Optional<Substance> substance = substanceEntityService.flexLookup(ingred.getSubstanceKey());

                                if (substance != null) {

                                    substanceName = substance.get().getName();
                                    approvalId = substance.get().approvalID;

                                    // Get Substance Name
                                    sb.append((substanceName != null) ? substanceName : "(No Ingredient Name)");

                                    // Storing in static variable so do not have to call the same Substance API twice just to get
                                    // approval Id.
                                    substanceApprovalIdSB.append((approvalId != null) ? approvalId : "(No Approval ID)");
                                }
                            }
                        } else {
                            sb.append("(No Ingredient Name)");
                            substanceApprovalIdSB.append("(No Approval ID)");
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return sb;
    }

    private static StringBuilder getIngredientDetails(ApplicationAllDTO s, AppAllDefaultColumns fieldName) {
        StringBuilder sb = new StringBuilder();

        try {
            if (s.getApplicationProductList().size() > 0) {
                List<ProductSrsAllDTO> prodList = s.getApplicationProductList();

                for (ProductSrsAllDTO prod : prodList) {

                    for (AppIngredientAllDTO ingred : prod.getApplicationIngredientList()) {
                        if (sb.length() != 0) {
                            sb.append("|");
                        }

                        try {
                            switch (fieldName) {
                                case APPLICANT_INGREDIENT_NAME:
                                    sb.append((ingred.getApplicantIngredName() != null) ? ingred.getApplicantIngredName() : "");
                                    break;
                                case SUBSTANCE_KEY:
                                    sb.append((ingred.getSubstanceKey() != null) ? ingred.getSubstanceKey() : "(No Substance Key)");
                                    break;
                                case INGREDIENT_TYPE:
                                    sb.append((ingred.getIngredientType() != null) ? ingred.getIngredientType() : "(No Ingredient Type)");
                                    break;
                                default:
                                    break;
                            }

                        } catch (Exception ex) {
                            System.out.println("*** Error Occured in ApplicationAllDTOExporter.java for Substance Code : " + ingred.getSubstanceKey());
                            ex.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return sb;
    }

    private static StringBuilder getIndicationDetails(ApplicationAllDTO s) {
        StringBuilder sb = new StringBuilder();

        if (s.getIndicationList().size() > 0) {
            List<AppIndicationAllDTO> indList = s.getIndicationList();

            for (AppIndicationAllDTO ind : indList) {
                if (sb.length() != 0) {
                    sb.append("|");
                }
                sb.append((ind.getIndication() != null) ? ind.getIndication() : "");
            }
        }
        return sb;
    }

    /**
     * Builder class that makes a SpreadsheetExporter.  By basic, the basic columns are used
     * but these may be modified using the add/remove column methods.
     */
    public static class Builder {
        private final List<ColumnValueRecipe<ApplicationAllDTO>> columns = new ArrayList<>();
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

            for (Map.Entry<Column, ColumnValueRecipe<ApplicationAllDTO>> entry : DEFAULT_RECIPE_MAP.entrySet()) {
                columns.add(entry.getValue());
            }
        }

        public Builder addColumn(Column column, ColumnValueRecipe<ApplicationAllDTO> recipe) {
            return addColumn(column.name(), recipe);
        }

        public Builder addColumn(String columnName, ColumnValueRecipe<ApplicationAllDTO> recipe) {
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
            ListIterator<ColumnValueRecipe<ApplicationAllDTO>> iter = columns.listIterator();
            while (iter.hasNext()) {

                ColumnValueRecipe<ApplicationAllDTO> oldValue = iter.next();
                ColumnValueRecipe<ApplicationAllDTO> newValue = oldValue.replaceColumnName(oldName, newName);
                if (oldValue != newValue) {
                    iter.set(newValue);
                }
            }
            return this;
        }

        /*
        public ApplicationAllDTOExporter build(ApplicationAllController applicationController) {
            return new ApplicationAllDTOExporter(this, applicationController);
        }
        */
        public ApplicationAllDTOExporter build(SubstanceEntityService substanceEntityService) {
            return new ApplicationAllDTOExporter(this, substanceEntityService);
        }

        public Builder includePublicDataOnly(boolean publicOnly) {
            this.publicOnly = publicOnly;
            return this;
        }

    }
}
