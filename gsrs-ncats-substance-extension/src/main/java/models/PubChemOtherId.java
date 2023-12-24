package models;

import gsrs.model.AbstractGsrsEntity;
import ix.core.models.IndexableRoot;
import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
