package fda.gsrs.substance.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import ix.core.models.Indexable;
import com.fasterxml.jackson.annotation.JsonIgnore;


@Entity
@Table(name="SRSCID_INGRED")
@Indexable(indexed=false)
public class Legacy_Ingred  {

    @Id
    @SequenceGenerator(name="legIngredSeq", sequenceName="SRSCID_SQ_INGRED_ID",allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "legIngredSeq")
    @Column(name="INGRED_ID")
    public Long id;

    @Column(name="BDNUM")
    public String bdnum;

    @Column(name="CAS_NO")
    public String cas_no;

    @Column(name="UNII")
    public String unii;

    @Column(name="INGRED_PUBLIC_DOMAIN")
    public String publicDomain;

    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="SUBSTANCE_ID")
    public Legacy_Substance legacySubst;

    @OneToMany(cascade = CascadeType.ALL, mappedBy="legacyIngred")
    public List<Legacy_IName> legacyInameList = new ArrayList<Legacy_IName>();

    public Legacy_Ingred() {}
}