package fda.gsrs.substance.model;

import ix.core.models.Indexable;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="SRSCID_SUBSTANCE_RELATIONSHIP")
@Indexable(indexed=false)
public class Legacy_Substance_Rel {

    @Id
    @SequenceGenerator(name="legSubRelSeq", sequenceName="SRSCID_SQ_SUBST_REL_ID",allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "legSubRelSeq")
    @Column(name="SUBSTANCE_RELATIONSHIP_ID")
    public Long substanceRelId;

    @Column(name="PARENT_BDNUM")
    public String parent_bdnum;

    @Column(name="RELATED_BDNUM")
    public String related_bdnum;

    @Column(name="PARENT_UNII")
    public String parent_unii;

    @Column(name="RELATED_UNII")
    public String related_unii;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="PARENT_SUBSTANCE_ID")
    private Legacy_Substance legacyParentSubstance;

    @Column(name="RELATED_SUBSTANCE_ID")
    public String related_substance_id;

    @Column(name="RELATIONSHIP_TYPE_ID")
    public String relationship_type_id;

    @Column(name="PUBLIC_DOMAIN")
    public String public_domain;

    public Legacy_Substance_Rel() {}
}