package gsrs.module.substance.exporters;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.module.substance.utils.SplitFunction;
import gsrs.module.substance.utils.UniqueFunction;

import io.burt.jmespath.JmesPath;
import io.burt.jmespath.Expression;
import io.burt.jmespath.RuntimeConfiguration;
import io.burt.jmespath.function.FunctionRegistry;
import io.burt.jmespath.jackson.JacksonRuntime;
import ix.ginas.exporters.*;
import java.util.Date;
import java.util.Objects;
import java.text.SimpleDateFormat;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Egor Puzanov on 11/8/21.
 */
@Slf4j
public class JmespathColumnValueRecipe<T> implements ColumnValueRecipe<T> {

    private final String columnName;
    private final Expression<JsonNode> expression;
    private final String delimiter;
    private final SimpleDateFormat datetime;

    public JmespathColumnValueRecipe(String columnName, Expression<JsonNode> expression, String delimiter, SimpleDateFormat datetime) {
        Objects.requireNonNull(columnName);
        Objects.requireNonNull(expression);
        Objects.requireNonNull(delimiter);
        this.columnName = columnName;
        this.expression = expression;
        this.delimiter = delimiter;
        this.datetime = datetime;
    }

    static <T>  ColumnValueRecipe<T> create(Enum<?> enumValue, String expression, String delimiter, String datetime) {
        return create(enumValue.name(), expression, delimiter, datetime);
    }

    static <T>  ColumnValueRecipe<T> create(String columnName, String expression, String delimiter, String datetime) {
        FunctionRegistry customFunctions = FunctionRegistry.defaultRegistry().extend(
                                                       new SplitFunction(),
                                                       new UniqueFunction());
        RuntimeConfiguration configuration = new RuntimeConfiguration.Builder()
                                   .withFunctionRegistry(customFunctions)
                                   .build();
        JmesPath<JsonNode> jmespath = new JacksonRuntime(configuration);
        SimpleDateFormat dtf = null;
        try {
            dtf = new SimpleDateFormat(datetime);
        } catch (Exception ex) {
        }
        return new JmespathColumnValueRecipe<T>(columnName, jmespath.compile(expression), delimiter, dtf);
    }

    @Override
    public int writeValuesFor(Spreadsheet.SpreadsheetRow row, int currentOffset, T obj) {
        JsonNode results = expression.search((JsonNode) obj);
        if (results.isValueNode() && ! results.isNull()) {
            String value = results.asText();
            if (datetime != null) {
                try {
                    value = datetime.format(new Date(Long.valueOf(value)));
                } catch (Exception ex) {
                }
            }
            row.getCell(currentOffset).writeString(value);
        } else if (results.isArray()) {
            StringBuilder sb = new StringBuilder();
            for(JsonNode result: results){
                if (result.isValueNode() && ! result.isNull()) {
                    String value = result.asText();
                    if (value != null && !value.isEmpty()) {
                        if(sb.length()!=0){
                            sb.append(delimiter);
                        }
                        sb.append(value);
                    }
                }
            }
            if (sb.length() != 0) {
                row.getCell(currentOffset).writeString(sb.toString());
            }
        }
        return 1;
    }

    @Override
    public int writeHeaderValues(Spreadsheet.SpreadsheetRow row, int currentOffset) {
        row.getCell(currentOffset).writeString(columnName);
        return 1;
    }

    @Override
    public boolean containsColumnName(String name) {
        return Objects.equals(columnName, name);
    }

    @Override
    public JmespathColumnValueRecipe<T> replaceColumnName(String oldName, String newName) {
        Objects.requireNonNull(oldName);
        Objects.requireNonNull(newName);

        if(containsColumnName(oldName)){
            return new JmespathColumnValueRecipe<>(newName, expression, delimiter, datetime);
        }
        return this;
    }
}
