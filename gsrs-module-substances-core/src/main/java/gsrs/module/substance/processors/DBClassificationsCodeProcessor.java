package gsrs.module.substance.processors;

import gsrs.springUtils.StaticContextAccessor;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Reference;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.jdbc.DataSourceBuilder;

/**
 * This EntityProcessor will create a Classifications comments string
 * for the Code using Database as the source
 *
 * @author Egor Puzanov
 */
@Data
@Slf4j
public class DBClassificationsCodeProcessor implements EntityProcessor<Code> {

    private final DataSource datasource;
    private final String codeSystem;
    private final String query;

    public DBClassificationsCodeProcessor() {
        this(new HashMap<String, Object>());
    }

    public DBClassificationsCodeProcessor(Map<String, Object> m) {
        String dataSourceQualifier = (String) m.get("dataSourceQualifier");
        if (dataSourceQualifier != null && !dataSourceQualifier.isEmpty()) {
            this.datasource = StaticContextAccessor.getBeanQualified(DataSource.class, dataSourceQualifier);
        } else if (m.get("datasource") instanceof Map) {
            Map<?, ?> dsconfig = (Map<?, ?>) m.get("datasource");
            this.datasource = DataSourceBuilder.create()
                                                .url((String)dsconfig.get("url"))
                                                .username((String)dsconfig.get("username"))
                                                .password((String)dsconfig.get("password"))
                                                .build();
        } else {
            this.datasource = StaticContextAccessor.getBean(DataSource.class);
        }
        this.codeSystem = m.getOrDefault("codeSystem", "").toString();
        this.query = m.getOrDefault("query", "").toString();
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
