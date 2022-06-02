package gsrs.dataExchange.model;

import ix.core.models.Backup;
import ix.core.models.IndexableRoot;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "ix_import_validation")
@Slf4j
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

    @Lob
    public String ValidationJson;

    public Date ValidationDate;
}
