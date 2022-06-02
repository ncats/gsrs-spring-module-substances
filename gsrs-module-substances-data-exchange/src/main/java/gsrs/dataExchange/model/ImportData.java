package gsrs.dataExchange.model;

import ix.core.models.Backup;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Lob;
import javax.persistence.Table;
import java.util.UUID;

@Backup
@Table(name = "ix_import_data")
@Slf4j
public class ImportData {

    public UUID RECORD_ID;

    public int Version;

    @Lob
    public String Data;
}
