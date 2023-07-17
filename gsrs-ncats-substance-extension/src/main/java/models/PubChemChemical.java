package models;

import gsrs.model.AbstractGsrsEntity;
import ix.core.models.IndexableRoot;
import lombok.Data;

import javax.persistence.*;

@IndexableRoot
//@Backup
@Data
@Entity
@Table(name="PUBCHEM_CHEMICAL")
public class PubChemChemical extends AbstractGsrsEntity {

    @Id
    @Column(name = "CID")
    public Long id;

    public String InChI;

    public String InChIKey;
}
