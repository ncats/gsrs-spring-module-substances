package example.substance.processor;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.junit.TimeTraveller;
import gsrs.module.substance.processors.LegacyAuditInfoProcessor;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestEntityProcessorFactory;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.models.Role;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Note;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
//TODO some tests from Play 2.x have been commented out because they change the created and modified dates which currently won't work in Hibernate
//so these tests just check the approval id and time
public class LegacyAuditInfoParserITTest extends AbstractSubstanceJpaEntityTest {

	@Autowired
	private TestEntityProcessorFactory entityProcessorFactory;

	@RegisterExtension
	TimeTraveller timeTraveller = new TimeTraveller();


    public void assertEqualsIgnoreCase(String expected, String test) {
        assertEquals(expected.toUpperCase(), test.toUpperCase());
    }
    
	
	@BeforeEach
	public void addEntityProcessor(){
		LegacyAuditInfoProcessor legacyAuditInfoProcessor = new LegacyAuditInfoProcessor();
		AutowireHelper.getInstance().autowire(legacyAuditInfoProcessor);
		entityProcessorFactory.setEntityProcessors(legacyAuditInfoProcessor);

	}


	public static class AuditNoteBuilder{

		String createdBy=null;
		String modifiedBy=null;
		String approvedBy=null;
		Date approvedDate = null;
		Date createdDate = null;

		public AuditNoteBuilder(){

		}

		public AuditNoteBuilder withCreatedBy(String createdBy){
			this.createdBy=createdBy;
			return this;
		}
		public AuditNoteBuilder withModifiedBy(String modifiedBy){
			this.modifiedBy=modifiedBy;
			return this;
		}
		public AuditNoteBuilder withApprovedBy(String approvedBy){
			this.approvedBy=approvedBy;
			return this;
		}

		public AuditNoteBuilder withApprovedDate(Date approvedDate){
			this.approvedDate=approvedDate;
			return this;
		}


		public String buildNoteText(){
			StringBuilder sb = new StringBuilder();
			sb.append(LegacyAuditInfoProcessor.START_LEGACY_REF);
			if(this.createdBy!=null){
				sb.append("<CREATED_BY>" + this.createdBy + "</CREATED_BY>");
			}
			if(this.modifiedBy!=null){
				sb.append("<MODIFIED_BY>" + this.modifiedBy + "</MODIFIED_BY>");
			}
			if(this.approvedBy!=null){
				sb.append("<APPROVED_BY>" + this.approvedBy + "</APPROVED_BY>");
			}
			if(this.approvedDate!=null){
				sb.append("<APPROVED_DATE>" + LegacyAuditInfoProcessor.FORMATTER.format(this.approvedDate) + "</APPROVED_DATE>");
			}
			if(this.createdDate!=null){
				sb.append("<CREATED_DATE>" + LegacyAuditInfoProcessor.FORMATTER.format(this.createdDate) + "</CREATED_DATE>");
			}
			return sb.toString();
		}

		public AuditNoteBuilder withCreatedDate(Date d) {
			createdDate=d;
			return this;
		}

	}

	@Test
	@WithMockUser(username = "admin", roles="Admin")

	public void tryForcingApprovedByInLegacyNoteAfterFailing() throws Exception {
		String theName = "Simple Named Concept";
		String forcedApprovedBy = "SOME_BLOKE";

		createUser(forcedApprovedBy, Role.Approver);

		UUID uuid = UUID.randomUUID();
		JsonNode jsn = new SubstanceBuilder()
				.addName(theName)
				.setUUID(uuid)
				.addNote(new Note(new AuditNoteBuilder()
						.withApprovedBy(forcedApprovedBy)
						.buildNoteText()))
				.buildJson();

		assertCreated(jsn);

		Optional<Substance> substance = substanceEntityService.get(uuid);
		assertEqualsIgnoreCase(forcedApprovedBy, substance.get().approvedBy.username);
	}
	
	
	
//	@Test
//	public void tryForcingModifiedByInLegacyNote() {
//		String theName = "Simple Named Concept";
//		String forcedApprovedBy = "tylertest";
//
//		createUser(forcedApprovedBy, Role.Approver);
//		JsonNode jsn = new SubstanceBuilder()
//				.addName(theName)
//				.generateNewUUID()
//				.addNote(new Note(new AuditNoteBuilder()
//						.withModifiedBy(forcedApprovedBy)
//						.buildNoteText()))
//				.buildJson();
//		ensurePass(api.submitSubstance(jsn));
//		assertEquals(forcedApprovedBy, api.fetchSubstanceJsonByUuid(jsn.at("/uuid").asText()).at("/lastEditedBy").asText());
//
//	}
	@Test
	@WithMockUser(username = "admin", roles="Admin")
	public void tryForcingApprovalDateAndApproverInLegacyNote() {

		String theName = "Simple Named Concept";
		String forcedApprovedBy = "tylertest";

		timeTraveller.jumpBack(1, ChronoUnit.YEARS);
		Date approvalDate = timeTraveller.getCurrentDate();

		createUser(forcedApprovedBy);

		UUID uuid = UUID.randomUUID();
		timeTraveller.jumpAhead(2, ChronoUnit.YEARS);
		assertNotEquals(approvalDate.getTime(), timeTraveller.getCurrentDate().getTime());
		new SubstanceBuilder()
				.addName(theName)
				.setUUID(uuid)
				.addNote(new Note(new AuditNoteBuilder()
						.withApprovedBy(forcedApprovedBy)
						.withApprovedDate(approvalDate)
						.buildNoteText()))
				.buildJsonAnd(this::assertCreated);

		Substance substance = substanceEntityService.get(uuid).get();
		assertEqualsIgnoreCase(forcedApprovedBy, substance.approvedBy.username);
		//the legacy date processor only keeps time to the second so we can't just do an equals check on the date object as the millis will be different
		assertEquals(TimeUtil.asLocalDate(approvalDate), TimeUtil.asLocalDate(substance.approved));
		assertEquals(timeTraveller.getCurrentDate(), substance.created);

	}


	
//	@Test
//	public void tryForcingCreatedDateWithEmptyCreator() {
//
//		String theName = "Simple Named Concept";
//		String forcedApprovedBy = "tylertest";
//		Date d = new Date();
//		d.setYear(1989);
//
//
//		JsonNode jsn = new SubstanceBuilder()
//				.addName(theName)
//				.generateNewUUID()
//				.addNote(new Note(new AuditNoteBuilder()
//						.withCreatedBy("")
//						.withCreatedDate(d)
//						.buildNoteText()))
//				.buildJson();
//		ensurePass(api.submitSubstance(jsn));
//
//		Date createdDate = new Date(api.fetchSubstanceJsonByUuid(jsn.at("/uuid").asText()).at("/created").asLong());
//
//		//Less than a second difference between set approved Date
//		//and actual approved date
//		assertTrue(Math.abs(d.getTime()-createdDate.getTime())<1000);
//
//		assertEquals(session.getUserName(),api.fetchSubstanceJsonByUuid(jsn.at("/uuid").asText()).at("/createdBy").asText());
//	}
//
//	@Test
//    public void ensureSubmittingRelatedRecordPreservesOldAuditInfo() throws InterruptedException {
//
//        String theName = "Simple Named Concept";
//        Date d = new Date();
//        d.setYear(1989);
//
//
//        JsonNode jsn = new SubstanceBuilder()
//                .addName(theName)
//                .generateNewUUID()
//                .addNote(new Note(new AuditNoteBuilder()
//                        .withModifiedBy("Some Guy")
//                        .withCreatedBy("")
//                        .withCreatedDate(d)
//                        .buildNoteText()))
//                .buildJson();
//
//        ensurePass(api.submitSubstance(jsn));
//
//        Relationship r2 = new Relationship();
//        r2.relatedSubstance=new SubstanceReference();
//        r2.relatedSubstance.refuuid=jsn.at("/uuid").asText();
//        r2.relatedSubstance.refPname=jsn.at("/names/0/name").asText();
//        r2.type="SOME TYPE->TEST";
//
//
//        JsonNode jsn2 = new SubstanceBuilder()
//                .addName(theName + " 2")
//                .generateNewUUID()
//                .addRelationship(r2)
//                .addNote(new Note(new AuditNoteBuilder()
//                        .withModifiedBy("Some Guy")
//                        .withCreatedBy("")
//                        .withCreatedDate(d)
//                        .buildNoteText()))
//                .buildJson();
//
//        ensurePass(api.submitSubstance(jsn2));
//
//        assertEquals("Some Guy",api.fetchSubstanceJsonByUuid(jsn.at("/uuid").asText()).at("/lastEditedBy").asText());
//        assertEquals("2",api.fetchSubstanceJsonByUuid(jsn.at("/uuid").asText()).at("/version").asText());
//
//    }
}
