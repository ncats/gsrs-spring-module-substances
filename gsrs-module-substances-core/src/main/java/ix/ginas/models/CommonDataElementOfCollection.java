package ix.ginas.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.core.SingleParent;
import ix.ginas.models.v1.Substance;

import jakarta.persistence.*;

/**
 * This abstract class is meant as a convenience tool to allow ownership for 
 * simple @OneToMany annotations.
 * 
 * @author Tyler Peryea
 *
 */
@MappedSuperclass
@SingleParent
public abstract class CommonDataElementOfCollection extends GinasCommonSubData{

}
