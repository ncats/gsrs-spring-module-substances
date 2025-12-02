package gsrs.module.substance.processors;

import gsrs.springUtils.StaticContextAccessor;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Reference;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.jdbc.DataSourceBuilder;

/**
 * This EntityProcessor will create a Classifications comments string
 * for the Code using Database as the source
 *
 * @author Egor Puzanov
 */

 @Slf4j
public class DBClassificationsCodeProcessor implements EntityProcessor<Code> {

    private DataSource datasource = StaticContextAccessor.getBean(DataSource.class);
    private String codeSystem;
    private String query;

    public DBClassificationsCodeProcessor() {
        this(new HashMap<String, Object>());
    }

    public DBClassificationsCodeProcessor(Map<String, Object> m) {
        if (m.containsKey("dataSourceQualifier")) {
            setDataSourceQualifier((String) m.get("dataSourceQualifier"));
        }
        if (m.get("datasource") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> dsconfig = (Map<String, String>) m.get("datasource");
            setDatasource(dsconfig);
        }
        if (m.containsKey("codeSystem")) {
            setCodeSystem((String) m.get("codeSystem"));
        }
        if (m.containsKey("query")) {
            setQuery((String) m.get("query"));
        }
    }

    public void setDataSourceQualifier(String dataSourceQualifier) {
        this.datasource = StaticContextAccessor.getBeanQualified(DataSource.class, dataSourceQualifier);
    }

    public void setDatasource(Map<String, String> dsconfig) {
        this.datasource = DataSourceBuilder.create()
            .url((String)dsconfig.get("url"))
            .username((String)dsconfig.get("username"))
            .password((String)dsconfig.get("password"))
            .build();
    }

    public void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public void prePersist(Code obj) throws EntityProcessor.FailProcessingException {
        if (codeSystem.equals(obj.codeSystem) && obj.code != null && !obj.code.isEmpty()) {
            try (Connection c = datasource.getConnection()) {
                try (PreparedStatement s = c.prepareStatement(query)) {
                    s.setString(1, obj.getCode());
                    try (ResultSet rs = s.executeQuery()) {
                        while (rs.next()) {
                            String value = rs.getString(1);
                            if (value != null && value.contains("|") && !value.equals(obj.comments)) {
                                obj.comments = value;
                                value = rs.getString(2);
                                if (value != null && value.startsWith("http") && !value.equals(obj.url)) {
                                   obj.url = value;
                                }
                                value = rs.getString(3);
                                if (value != null && !value.isEmpty() && obj.getReferences().size() == 0) {
                                    Reference r = new Reference();
                                    r.docType = value;
                                    value = rs.getString(4);
                                    if (value != null && !value.isEmpty()) {
                                        r.citation = value;
                                        obj.addReference(r, obj.getOwner());
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                log.error(ex.toString());
            } finally {
            }
        }
    }

    @Override
    public void preUpdate(Code obj) throws EntityProcessor.FailProcessingException {
        prePersist(obj);
    }

    @Override
    public Class<Code> getEntityClass() {
        return Code.class;
    }
}
