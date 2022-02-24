package ix.core.models;

import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="ix_core_procrec")
@Indexable(indexed = false)
@SequenceGenerator(name = "LONG_SEQ_ID", sequenceName = "ix_core_procrec_seq", allocationSize = 1)
public class ProcessingRecord extends LongBaseModel {
    public enum Status {
        OK, FAILED, PENDING, UNKNOWN, ADAPTED
    }

    @Column(name="rec_start")
    public Long start;
    @Column(name="rec_stop")
    public Long stop;

    @Column(length=128)
    public String name;
    
    @ManyToMany
    @JoinTable(name="ix_core_procrec_prop")
    public List<Value> properties = new ArrayList<Value>();
    
    
    @Version
    public Timestamp lastUpdate; // here
    
    
    /**
     * record status
     */
    public Status status = Status.PENDING;

    /**
     * detailed status message
     */
    @Lob
    @Basic(fetch=FetchType.EAGER)
    public String message;

    @OneToOne(cascade=CascadeType.ALL)
    public XRef xref;
    
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JsonView(BeanViews.Full.class)
    public ProcessingJob job;

    public ProcessingRecord () {}
}
