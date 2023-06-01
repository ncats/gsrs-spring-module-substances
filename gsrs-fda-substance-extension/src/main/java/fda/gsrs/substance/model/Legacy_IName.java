package fda.gsrs.substance.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import ix.core.models.Indexable;
import com.fasterxml.jackson.annotation.JsonIgnore;

//import play.db.ebean.Model;


@Entity
@Table(name="SRSCID_INAME")
@Indexable(indexed=false)
public class Legacy_IName {

    @Id
    @SequenceGenerator(name="legInameSeq", sequenceName="SRSCID_SQ_INAME_ID",allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "legInameSeq")
    @Column(name="INAME_ID")
    public Long id;

    @Column(name="BDNUM")
    public String bdnum;

    @Column(name="INAME", length = 4000)
    public String iname;

    @Column(name="TYPE")
    public String type;

    @Column(name="PUBLIC_DOMAIN")
    public String publicDomain;

    // Added by Ramez on January 3, 2017
    //
    @Column(name="DEPRECATED")
    public String deprecated;

    // Added by Ramez on January 3, 2017
    //
    @Column(name="LISTING_NAME")
    public String listing_name;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="INGRED_ID")
    private Legacy_Ingred legacyIngred;

    public Legacy_IName() {}
}