package gsrs.dataExchange.model;

import ix.core.models.Backup;
import ix.core.models.IndexableRoot;
import ix.ginas.models.utils.JSONEntity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.UUID;

@Backup
@Entity
@Table(name = "ix_import_mapping")
@Slf4j
@IndexableRoot
@Data
public class KeyValueMapping {

    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)
    private UUID recordId;

    private String key;

    private String value;

    private String qualifier;
}
