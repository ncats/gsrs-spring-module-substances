package models;

import gsrs.model.AbstractGsrsEntity;
import ix.core.models.IndexableRoot;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;

@IndexableRoot
//@Backup
@Data
@EqualsAndHashCode(callSuper=false)
@Entity
@Table(name="PUBCHEM_CHEMICAL")
public class PubChemChemical extends AbstractGsrsEntity {

    @Id
    @Column(name = "CID")
    public Long id;

    public String InChI;

    public String InChIKey;
}
