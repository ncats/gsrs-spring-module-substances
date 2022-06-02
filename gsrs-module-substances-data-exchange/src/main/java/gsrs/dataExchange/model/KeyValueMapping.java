package gsrs.dataExchange.model;

import ix.core.models.Backup;
import ix.core.models.IndexableRoot;
import ix.ginas.models.utils.JSONEntity;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.UUID;

@Backup
@Entity
@Table(name = "ix_ginas_import_mapping")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("META")
@Slf4j
@IndexableRoot
public class KeyValueMapping {

    public UUID RECORD_ID;

    public String key;

    public String value;

    public String qualifier;


}
