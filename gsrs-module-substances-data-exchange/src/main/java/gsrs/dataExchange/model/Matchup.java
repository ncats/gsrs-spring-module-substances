package gsrs.dataExchange.model;

import java.util.UUID;

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
