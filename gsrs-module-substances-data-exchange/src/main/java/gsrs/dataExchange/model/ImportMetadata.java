package gsrs.dataExchange.model;

import com.fasterxml.jackson.annotation.JsonView;
import ix.core.EntityMapperOptions;
import ix.core.models.Backup;
import ix.core.models.BeanViews;
import ix.core.models.IndexableRoot;
import ix.ginas.models.utils.JSONEntity;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Backup
@JSONEntity(name = "metadata", title = "Metadata")
@Entity
@Table(name = "ix_ginas_import_metadata")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("META")
@Slf4j
@IndexableRoot
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
        current, superseded
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

    public UUID RECORD_ID;

    public String SourceName;

    public Date VersionCreationDate;

    public int Version;

    public RecordImportStatus ImportStatus;

    public RecordImportType ImportType;

    public RecordVersionStatus VersionStatus;

    public RecordValidationStatus ValidationStatus;

    public RecordProcessStatus ProcessStatus;

    @JSONEntity(title = "KeyValueMappings")
    @OneToMany(mappedBy = "record_id", cascade = CascadeType.ALL)
    @JsonView(BeanViews.Full.class)
    @EntityMapperOptions(linkoutInCompactView = true)
    public List<KeyValueMapping> KeyValueMappings = new ArrayList<>();

    @JSONEntity(title = "Validations")
    @OneToMany(mappedBy = "record_id, version", cascade = CascadeType.ALL)
    @JsonView(BeanViews.Full.class)
    @EntityMapperOptions(linkoutInCompactView = true)
    public List<Validation> validations = new ArrayList<>();

    public ImportMetadata() {

    }
}
