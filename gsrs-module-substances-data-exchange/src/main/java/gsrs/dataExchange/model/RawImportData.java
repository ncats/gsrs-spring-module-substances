package gsrs.dataExchange.model;

import lombok.extern.slf4j.Slf4j;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "ix_import_raw")
@Slf4j
public class RawImportData {
    public UUID RECORD_ID;

    public byte[] RawData;

    public String RecordFormat;
}
