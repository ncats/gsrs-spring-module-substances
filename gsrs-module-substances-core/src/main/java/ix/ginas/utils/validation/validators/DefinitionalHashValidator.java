package ix.ginas.utils.validation.validators;

import gsrs.module.substance.definitional.DefinitionalElements;
import gsrs.module.substance.definitional.DefinitionalElements.DefinitionalElementDiff.OP;
import gsrs.module.substance.services.DefinitionalElementFactory;
import gsrs.security.GsrsSecurityUtils;
import ix.core.models.Role;
import ix.core.util.LogUtil;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by katzelda on 2/11/19.
 */
@Slf4j
public class DefinitionalHashValidator  extends AbstractValidatorPlugin<Substance> {

	@Autowired
	private DefinitionalElementFactory definitionalElementFactory;

	public DefinitionalElementFactory getDefinitionalElementFactory() {
		return definitionalElementFactory;
	}

	public void setDefinitionalElementFactory(DefinitionalElementFactory definitionalElementFactory) {
		this.definitionalElementFactory = definitionalElementFactory;
	}

	/*
        When changing a structure or defining information the application should require
        you to enter a new reference or reaffirm the reference or references for the structure
        or defining information for an approved substance
         */
    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {
//				System.out.println("in def hash Validator with substance of type " + objnew.substanceClass.name());
        
        //don't validate if there is no old version
        if(objold==null) {
            return;
        }
		LogUtil.trace(()->"in def hash Validator with substance of type " + objnew.substanceClass.name());

        /*if(objold ==null || objnew.getApprovalID() ==null || (objnew.getApprovalID() !=null && objold.getApprovalID() ==null)){
            //new substance or not approved don't validate
            return;
        }*/
				DefinitionalElements newDefinitionalElements = definitionalElementFactory.computeDefinitionalElementsFor(objnew);
				DefinitionalElements oldDefinitionalElements;
				try	{
				        // The old equivalent to this line used to throw a NPE if objold was null,
				        // this was the effective way that the validation rule was disabled for new records.
						oldDefinitionalElements = definitionalElementFactory.computeDefinitionalElementsFor(objold);
				}catch(Exception e){
						log.warn("Unable to access definitional elements for old substance");
						return;
				}

				if(!Arrays.equals(newDefinitionalElements.getDefinitionalHash(),
                oldDefinitionalElements.getDefinitionalHash())){
				//we have changed something "definitional"

						List<DefinitionalElements.DefinitionalElementDiff> diff = newDefinitionalElements.diff(oldDefinitionalElements);
						//quick hack in case the definitional elements are in a different order,
						//Arrays.equals() won't cut it. so need to do the more involved diff...
						if(!diff.isEmpty()) {
								if( changesContainLayer(diff, 1) && objnew.status.equals("approved")) {
										log.trace("approved substance with change to layer 1 ");
										// only for approved substances
										//confirm can be a new warning that can be dismissed

										if(!GsrsSecurityUtils.hasAnyRoles(Role.Admin)) {
											/*
											This section related to GSRS-1347 (March 2020)
											When a user makes a change to an approved sustance (with a UNII) and the user is _not_ an admin
											-- but _is_ a super updated because regular updaters are not allowed to update approved substances
											we display a strong warning.
											Test this by making these types of changes:
											1) To a substance's level 1 hash (for example, by changing the structure of a Chemical)
												-> we expect a non-admin user to get the warning below
												-> we expect an admin user to get a warning about the specific changes to the def hash
											2) To a substance's level 2 hash (for example, by changing the stereochemistry field of a Chemical)
												-> we expect both types of user to get a warning about the specific changes to the def hash
											3) To a field outside of the def hash (for example, adding a name or code)
												-> no warning.
											*/
												String message = 
													"WARNING! You have made a change to the fundamental definition of a validated substance. Are you sure you want to proceed with this change?";
												callback.addMessage(GinasProcessingMessage
													.WARNING_MESSAGE(message));
												return;
										}
								}
								GinasProcessingMessage gpm = createDiffMessage(diff);
								callback.addMessage(gpm);
								log.trace("in DefinitionalHashValidator, appending message " + gpm.getMessage());
						} else {
								log.trace("diffs empty ");
						}
				} else {
					log.trace("Arrays equal");
				}
    }
		
		private boolean changesContainLayer(List<DefinitionalElements.DefinitionalElementDiff> changes, int layer) {
			log.trace("changed: ");
			
			boolean result= changes.stream().anyMatch(c-> (c.getOp().equals(OP.ADD) && c.getNewValue().getLayer()==layer)
							|| (c.getOp().equals(OP.REMOVED) && (c.getOldValue().getLayer() == layer))
							|| (c.getOp().equals(OP.CHANGED) && (c.getNewValue().getLayer() == layer || c.getOldValue().getLayer() == layer))
							);
			log.trace("changesContainLayer to return " + result);
			return result;
		}
		
		private GinasProcessingMessage createDiffMessage(List<DefinitionalElements.DefinitionalElementDiff> diffs) {
			List<String> messageParts = new ArrayList();
			for(DefinitionalElements.DefinitionalElementDiff d : diffs){
				switch(d.getOp()) {
					case CHANGED :
						messageParts.add( String.format("definitional element %s changed from '%s' to '%s'",
							d.getNewValue().getKey(), d.getOldValue().getValue(), d.getNewValue().getValue()));
						break;
					case ADD :
						messageParts.add( String.format("definitional element %s added with value '%s'",
							d.getNewValue().getKey(), d.getNewValue().getValue()));
						break;
					case REMOVED :
						messageParts.add(String.format("definitional element %s with value '%s' was removed",
							d.getOldValue().getKey(), d.getOldValue().getValue()));
						break;
				}
			}
			if(messageParts.size() == 1) {
				return GinasProcessingMessage.WARNING_MESSAGE("A definitional change has been made: %s please reaffirm.  ",
					messageParts.get(0));
			}
			return GinasProcessingMessage.WARNING_MESSAGE("Definitional changes have been made: %s; please reaffirm.  ",
				String.join("; ", messageParts));
		}
}
