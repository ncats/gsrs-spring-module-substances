package fda.gsrs.substance.processors;

import java.util.List;

import fda.gsrs.substance.model.Legacy_IName;
import fda.gsrs.substance.model.Legacy_Ingred;
import fda.gsrs.substance.model.Legacy_Substance;
import fda.gsrs.substance.model.Legacy_Substance_Rel;
import ix.core.EntityProcessor;
//import ix.ginas.controllers.v1.SubstanceFactory;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.MixtureSubstance;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.NucleicAcidSubstance;
import ix.ginas.models.v1.PolymerSubstance;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.SpecifiedSubstanceGroup1Substance;
import ix.ginas.models.v1.StructurallyDiverseSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LegacyTableProcessor implements EntityProcessor<Substance>
{
	//private static final Model.Finder<String, Legacy_Ingred> finder = new Model.Finder("srscid", String.class, Legacy_Ingred.class);

	private static final String INTERNAL_CODE_SYSTEM = "BDNUM";
	private static final String INTERNAL_CAS_SYSTEM = "CAS";


	public LegacyTableProcessor()
	{
		// System.out.println("Made processor");
	}


	@Override
	public void postPersist(Substance obj)
	{
//    System.out.println("Post Persist Hook on:" + obj);

		try {

			// Grab BDNUM and CAS
			String internalCode = null;
			String casno = null;
			for (Code c : obj.codes)
			{
				if (c.codeSystem.equals(INTERNAL_CODE_SYSTEM))
				{
					internalCode = c.code;
//             Logger.debug("LEGACY CODE SYSTEM : " + internalCode);
				}
				else if (c.codeSystem.equals(INTERNAL_CAS_SYSTEM))
				{
					casno = c.code;
//             Logger.debug("LEGACY CAS NUMBER : " + casno);

				}
			}// end grab BDNUM and CAS

			Legacy_Substance substanceRecord = new Legacy_Substance();

			// if BDNUM is not null
			if (internalCode != null) {

//          Logger.debug("INSIDE LEGACY SUBSTANCE : " + internalCode + " Substance ID "
//                + obj.uuid.toString()
//                + " UNII " + obj.getApprovalIDDisplay());

				substanceRecord.subst_id = obj.uuid.toString();
				substanceRecord.unii = obj.approvalID;
				substanceRecord.priorityBdnum = internalCode;

				// Changed by Ramez on Jan. 3, 2017
				// Now we use a series of value mappings.
				//
				// substanceRecord.status = obj.status;
				//
				if (obj.status != null) {
					if(obj.status.equalsIgnoreCase("FAILED")){
						substanceRecord.status = "INCOMPLETE";
					}else if(obj.status.equalsIgnoreCase("non-approved")){
						substanceRecord.status = "CONCEPT";
					}else if(obj.status.equalsIgnoreCase("pending")){
						substanceRecord.status = "PENDING";
					}else if(obj.status.equalsIgnoreCase("approved")){
						substanceRecord.status = "COMPLETE";
					}
				}else {
					substanceRecord.status = null;
				}
				//
				// end change by Ramez, Jan. 3, 2017

				substanceRecord.substance_source = null;  // TO DO: Fix this. We need to insert the SUBSTANCE SOURCE data

				boolean isRecordPublic = false;
				switch (obj.substanceClass)
				{
					case chemical:
						isRecordPublic = ((ChemicalSubstance) obj).getStructure().getAccess().isEmpty();
						break;
					case concept:
						isRecordPublic = (obj.getAccess().isEmpty()) ? true : false;
						break;
					case mixture:
						isRecordPublic = ((MixtureSubstance) obj).mixture.getAccess().isEmpty();
						break;
					case nucleicAcid:
						isRecordPublic = ((NucleicAcidSubstance) obj).nucleicAcid.getAccess().isEmpty();
						break;
					case polymer:
						isRecordPublic = ((PolymerSubstance) obj).polymer.getAccess().isEmpty();
						break;
					case protein:
						isRecordPublic = ((ProteinSubstance) obj).protein.getAccess().isEmpty();
						break;
					case specifiedSubstanceG1:
						isRecordPublic = ((SpecifiedSubstanceGroup1Substance) obj).specifiedSubstance.getAccess().isEmpty();
						break;
					case structurallyDiverse:
						isRecordPublic = ((StructurallyDiverseSubstance) obj).structurallyDiverse.getAccess().isEmpty();
						break;
					case reference:
					case specifiedSubstanceG2:
					case specifiedSubstanceG3:
					case specifiedSubstanceG4:
					case unspecifiedSubstance:
					default:
						isRecordPublic = true;
						break;
				}//switch

				substanceRecord.publicDomain = (isRecordPublic) ? "Y" : "N";

				Legacy_Ingred ingredRecord = new Legacy_Ingred();
				ingredRecord.bdnum = internalCode;
				
				ingredRecord.unii = obj.approvalID;
				//If there is no explicit UNII, then get the UNII for the parent
				//substance (for sub-concepts)
				if(obj.approvalID==null){
				    SubstanceReference subRef = obj.getParentSubstanceReference();
                    
                    //Note that we may need to expand how this is done
                    //in the future, as the subref approvalID is not updated
                    //after approval of that record. We will need an additional
                    //trigger and/or query to fix this.
                    if (subRef != null) {
                        ingredRecord.unii = subRef.approvalID;
                    }
				}
				
				
				ingredRecord.cas_no = casno;
				ingredRecord.publicDomain = (obj.getAccess().isEmpty()) ? "Y" : "N";
//          ingredRecord.subst_id = obj.uuid.toString();

				ingredRecord.legacySubst=substanceRecord;

//          Logger.debug("AFTER INGRED RECORD!!! ");

				for (Name n : obj.names) {

					boolean isEnglish = false;
					for (ix.core.models.Keyword kw : n.languages) {
						if (kw.getValue().equals("en")) {
							isEnglish = true;
							break;
						}
					}
					if (!isEnglish) continue;

//             System.out.println("Loop start INAME Name : " + n.getName() + " Type : " + n.type);

					String inameType;
					if (n.isDisplayName()) {
						inameType = "PT";
					} else {
						if (n.type != null) {
							if(n.type.equalsIgnoreCase("cn")){ //Common name
								inameType = "SY"; //synonym
							}else if(n.type.equalsIgnoreCase("sys")){ //systematic
								inameType = "SN"; //Old term for systematic
							}else if(n.type.equalsIgnoreCase("bn")){ //brand name
								inameType = "TR"; //trade name
							}else if(n.type.equalsIgnoreCase("cd")){
								inameType = "CD"; //codes are the same
							}else{
								inameType = "SY"; //default to SY, as in old legacy tables
							}
						}else {
							inameType = "SY";
						}
					}

					Legacy_IName inameRecord = new Legacy_IName();
//             System.out.println("INAME BDNUM : " + internalCode);
					inameRecord.bdnum = internalCode;
//             System.out.println("INAME IANME : " + n.getName());
					inameRecord.iname = n.getName().substring(0, Math.min(n.getName().length(), 4000));;
					inameRecord.type = inameType;
					inameRecord.publicDomain = (n.getAccess().isEmpty()) ? "Y" : "N";

					// Added by Ramez on January 3, 2017
					//
					inameRecord.deprecated = (n.deprecated) ? "Y" : "N";
					inameRecord.listing_name = (n.preferred) ? "Y" : "N";
					//
					// end addition by Ramez, Jan. 3, 2017

//             System.out.println("Loop end INAME Name : " + n.getName() + " Type : " + n.type);

					ingredRecord.legacyInameList.add(inameRecord);
				}
//          System.out.println("Loop exit INAME");

				//Psuedocode for relationships

				for (Relationship r : obj.relationships)
				{
					//Parent UNII
					Legacy_Substance_Rel newRel = new Legacy_Substance_Rel();

					newRel.parent_bdnum=internalCode;
					newRel.parent_unii=obj.approvalID;

					newRel.related_unii=r.relatedSubstance.approvalID;
					newRel.related_substance_id=r.relatedSubstance.refuuid;
					newRel.related_bdnum=getBDNUMForSubstanceReference(r.relatedSubstance);

					newRel.relationship_type_id=r.type;
					newRel.public_domain= substanceRecord.publicDomain;

					substanceRecord.legacyRelationshipsList.add(newRel);
				}

//Ebean.getServer(null).getAdminLogging().setDebugGeneratedSql(true);
				ingredRecord.save("srscid");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Legacy Substance Insert Error", ex);
		}
	}//postPersist()

	public String getBDNUM(Substance s)
	{
		if(s==null) return null;

		String internalCode = null;
		for (Code c : s.codes)
		{
			if (c.codeSystem.equals(INTERNAL_CODE_SYSTEM)) {
				internalCode = c.code;
//          Logger.debug("LEGACY CODE SYSTEM : " + internalCode);
			}
		}
		return internalCode;
	}

	public String getBDNUMForSubstanceReference(SubstanceReference sr)
	{
		Substance substance = SubstanceFactory.getSubstance(sr.uuid);
		return getBDNUM(substance);
	}


	@Override
	public void postLoad(Substance obj)
	{
		// Logic here may be needed at certain times for rebuilding indexes
		// This will require some external information, not yet present
	}

	@Override
	public Class<Substance> getEntityClass() {
		return Substance.class;
	}

	@Override
	public void postRemove(Substance obj)
	{
		// Could have logic here to remove things

//    Logger.debug("Legacy Ingred Delete: SUBSTANCE_ID = " + obj.uuid.toString());
		List<Legacy_Ingred> ingredList  = finder.where().ieq("SUBSTANCE_ID", obj.uuid.toString()).findList();

		if(ingredList!=null && !ingredList.isEmpty()){
			for(Legacy_Ingred ingredRecord : ingredList) {
				ingredRecord.delete("srscid");
			}
		}// if
	}// postRemove()


	@Override
	public void postUpdate(Substance gsrsSubstance) {
		try {
			postRemove(gsrsSubstance);
		} catch (Exception e) {
			log.error("Error updating legacy substance [delete]", e);
			e.printStackTrace();
		}

		try {
			postPersist(gsrsSubstance);
		} catch (Exception e) {
			log.error("Error updating legacy substance [persist]", e);
			e.printStackTrace();
		}

	}

	@Override
	public void prePersist(Substance s){}
	@Override
	public void preUpdate(Substance obj) {}
	@Override
	public void preRemove(Substance obj) {}

}//class LegacyTableProcessor
