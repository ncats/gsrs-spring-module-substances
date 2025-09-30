package ix.ginas.utils.validation.validators;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.nih.ncats.common.Tuple;
import gsrs.services.PrivilegeService;
import ix.core.models.Role;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by peryeata on 8/28/18.
 */
public class RelationshipModificationValidator extends AbstractValidatorPlugin<Substance> {
	private static Collection<Role> ALLOWED_ROLES = Arrays.asList(Role.Admin, Role.SuperUpdate);

	@Autowired
	private PrivilegeService privilegeService;

	@Override
	public void validate(Substance s, Substance objold, ValidatorCallback callback) {


		if(objold == null){
			return;
		}

		if(!privilegeService.canDo("Modify Relationships")){
			Map<UUID, Relationship> oldRelationships = Optional.ofNullable(objold.relationships)
					.map(r->r.stream())
					.orElse(Stream.empty())
					.collect(Collectors.toMap(r->r.uuid, r->r));

			//keep only old relationships
			Optional.ofNullable(s.relationships)
					.map(r->r.stream())
					.orElse(Stream.empty())
					.map(r-> Tuple.of(r, oldRelationships.get(r.uuid)))
					.filter(t->t.v()!=null)
					.filter(t->isChanged(t.v(),t.k())) //has changed
					.forEach(t->{
						callback.addMessage(GinasProcessingMessage
								.ERROR_MESSAGE("Relationship \"%s\" can not be updated by non-admin users.",
										t.v().toSimpleString()));
					});


		}

	}

	public boolean isChanged(Relationship r1, Relationship r2){
		if(!areEquivalentSubRefs(r1.relatedSubstance,r2.relatedSubstance)){
			return true;
		}
		if(!areEquivalentSubRefs(r1.mediatorSubstance,r2.mediatorSubstance)){
			return true;
		}
		try {
			Relationship r2temp = EntityWrapper.of(r2).getClone();
			r2temp.relatedSubstance=r1.relatedSubstance;
			r2temp.mediatorSubstance=r1.mediatorSubstance;

			//Return false if they are the same (looking for change)
			return !r1.getDefinitionalHash().equals(r2temp.getDefinitionalHash());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return true;
		}

	}

	public boolean areEquivalentSubRefs(SubstanceReference sr1, SubstanceReference sr2){
		if(sr1==null && sr2 == null)return true;
		if(sr1==null && sr2 != null)return false;
		if(sr1!=null && sr2 == null)return false;
		return sr1.isEquivalentTo(sr2);
	}

}