package fda.gsrs.substance.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import ix.core.models.Indexable;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="SRSCID_SUBSTANCE")
@Indexable(indexed=false)
public class Legacy_Substance {

    @Id
    @Column(name="SUBSTANCE_ID")
    public String subst_id;

    @Column(name="PRIORITY_BDNUM")
    public String priorityBdnum;

    @Column(name="SUBSTANCE_SOURCE")
    public String substance_source;

    @Column(name="UNII")
    public String unii;

    @Column(name="status")
    public String status;

    @Column(name="PUBLIC_DOMAIN")
    public String publicDomain;

    //@OneToOne(cascade = CascadeType.ALL, mappedBy="legacySubst")
    //public Legacy_Ingred legacyIngred;

    @OneToMany(cascade = CascadeType.ALL, mappedBy="legacyParentSubstance")
    public List<Legacy_Substance_Rel> legacyRelationshipsList = new ArrayList<>();

    public Legacy_Substance() {}
}