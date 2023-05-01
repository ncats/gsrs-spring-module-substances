package models;

import gsrs.model.AbstractGsrsEntity;
import ix.core.models.IndexableRoot;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@IndexableRoot
//@Backup
@Data
@Entity
@Table(name="PUBCHEM_ID")
public class PubChemOtherId extends AbstractGsrsEntity {

    @Id
    @Column(name = "CID")
    public Long id;

    public String IDSystem = "CAS";

    public String IDValue;
}
