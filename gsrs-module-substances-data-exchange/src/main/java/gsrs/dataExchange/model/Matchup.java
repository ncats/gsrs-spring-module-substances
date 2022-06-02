package gsrs.dataExchange.model;

import ix.core.models.Backup;
import ix.core.models.IndexableRoot;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.UUID;

@Backup
@Table(name = "ix_import_matchup")
@Slf4j
public class Matchup {

    public enum RecordMatchType {
        staging,
        permanent,
        external /*not sure if we'll use this*/
    }
    public UUID RECORD_ID;

    public int Version;

    public String Key;

    public String Value;

    public String Qualifier;

    public UUID MatchedRecord;

    public RecordMatchType MatchType;

    public int MatchLevel;
}
