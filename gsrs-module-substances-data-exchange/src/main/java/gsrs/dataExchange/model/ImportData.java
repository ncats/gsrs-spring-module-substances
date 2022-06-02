package gsrs.dataExchange.model;

import ix.core.models.Backup;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;

import java.util.UUID;

@Backup
@Table(name = "ix_import_data")
@Slf4j
@Data
public class ImportData {

    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)
    private UUID recordId;

    private int version;

    @Lob
    private String data;
}
