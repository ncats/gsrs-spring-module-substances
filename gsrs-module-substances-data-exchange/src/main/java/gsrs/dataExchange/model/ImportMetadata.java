package gsrs.dataexchange.model;

import com.fasterxml.jackson.annotation.JsonView;
import ix.core.EntityMapperOptions;
import ix.core.models.Backup;
import ix.core.models.BeanViews;
import ix.core.models.IndexableRoot;
import ix.ginas.models.utils.JSONEntity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Backup
@JSONEntity(name = "metadata", title = "Metadata")
@Entity
@Table(name = "ix_import_metadata")
@Slf4j
@IndexableRoot
@Data
public class ImportMetadata {

    public enum RecordImportStatus {
        staged,
        accepted,
        imported,
        merged,
        rejected;
    }

    public enum RecordImportType {
        create,
        merge
    }

    public enum RecordVersionStatus {
        current, 
        superseded
    }

    public enum RecordValidationStatus {
        pending,
        valid,
        warning,
        error,
        unparseable
    }

    public enum RecordProcessStatus {
        loaded,
        parsed,
        validated,
        indexed
    }

    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)    private UUID recordId;

    private String sourceName;

    private Date versionCreationDate;

    private int version;

    private RecordImportStatus importStatus;

    private RecordImportType importType;

    private RecordVersionStatus versionStatus;

    private RecordValidationStatus validationStatus;

    private RecordProcessStatus processStatus;

    private String entityClass;

    @JSONEntity(title = "KeyValueMappings")
    @OneToMany(mappedBy = "recordId", cascade = CascadeType.ALL)
    @JsonView(BeanViews.Full.class)
    @EntityMapperOptions(linkoutInCompactView = true)
    public List<KeyValueMapping> KeyValueMappings = new ArrayList<>();

    @JSONEntity(title = "Validations")
    @OneToMany(mappedBy = "recordId, version", cascade = CascadeType.ALL)
    @JsonView(BeanViews.Full.class)
    @EntityMapperOptions(linkoutInCompactView = true)
    public List<Validation> validations = new ArrayList<>();

    public ImportMetadata() {

    }
}
