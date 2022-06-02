package gsrs.dataExchange.model;

import javax.persistence.Lob;
import java.util.UUID;

public class ImportData {

    public UUID RECORD_ID;

    public int Version;

    @Lob
    public String Data;
}
