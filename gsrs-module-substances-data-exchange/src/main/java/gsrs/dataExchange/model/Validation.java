package gsrs.dataExchange.model;

import ix.core.models.Backup;
import ix.core.models.IndexableRoot;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Backup
@Entity
@Table(name = "ix_ginas_import_validation")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("META")
@Slf4j
@IndexableRoot
public class Validation {

    public enum ImportValidationType {
        info,
        warning,
        error
    }
    public UUID RECORD_ID;

    public int version;

    public UUID ValidationId;

    public ImportValidationType ValidationType;

    public String ValidationMessage;

    public String ValidationJson;

    public Date ValidationDate;
}
