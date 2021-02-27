package ix.ginas.utils;

import ix.core.UserFetcher;
import ix.core.models.Principal;
import ix.core.util.TimeUtil;
import ix.ginas.controllers.v1.SpecifiedSubstanceGroupCodeFactory;
import ix.srs.models.SpecifiedSubstanceGroup;
import ix.srs.models.SpecifiedSubstanceGroupCode;
import ix.srs.models.SpecifiedSubstanceGroupReference;
import ix.utils.Tuple;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class SSGCodeSequentialGenerator extends SequentialNumericIDGenerator<SpecifiedSubstanceGroup> {
	private AtomicLong lastNum= new AtomicLong(1);
	private boolean fetched=false;
	private String codeSystem;
	
	public String getCodeSystem() {
		return codeSystem;
	}

	public void setCodeSystem(String codeSystem) {
		this.codeSystem = codeSystem;
	}

	public SSGCodeSequentialGenerator(int len, String suffix, boolean padding, String codeSystem) {
		super(len, suffix, padding);
		this.codeSystem=codeSystem;
	}

	@Override
	public long getNextNumber() {
		if(!fetched){
			Optional<Tuple<Long,SpecifiedSubstanceGroupCode>> code= SpecifiedSubstanceGroupCodeFactory.getHighestValueCode(codeSystem, suffix);
			if(code.isPresent()){
				lastNum.set(code.get().k());
			}
			fetched=true;
		}
		return lastNum.incrementAndGet();
	}

	public void addCode(SpecifiedSubstanceGroup s) {

		// Get Current Date and Time
		java.util.Date currentDate = TimeUtil.getCurrentDate();

		// get Username
		Long userId = 0L;
		Principal p1 = UserFetcher.getActingUser();
		if (p1 != null) {
			userId = p1.id;
		}

		SpecifiedSubstanceGroupReference r = addReference(s);

		SpecifiedSubstanceGroupCode c = new SpecifiedSubstanceGroupCode();
		c.codeSystem=this.codeSystem;
		c.code=this.generateID();
		c.type="PRIMARY";
		c.references = r.uuid.toString();
		c.created = currentDate;
		c.createdBy = userId;
		c.lastEdited = currentDate;
		c.lastEditedBy = userId;

		s.codes.add(c);
	}

	public SpecifiedSubstanceGroupReference addReference(SpecifiedSubstanceGroup s){
		// Get Current Date and Time
		java.util.Date currentDate = TimeUtil.getCurrentDate();

		// get Username
		Long userId = 0L;
		Principal p1 = UserFetcher.getActingUser();
		if (p1 != null) {
			userId = p1.id;
		}

		SpecifiedSubstanceGroupReference r = new SpecifiedSubstanceGroupReference();
		r.uuid = UUID.randomUUID();
		r.docType="SYSTEM";
		r.citation="System Generated Code";
		r.created = currentDate;
		r.createdBy = userId;
		r.lastEdited = currentDate;
		r.lastEditedBy = userId;
		s.references.add(r);

		return r;
	}

	@Override
	public boolean isValidId(String id) {
		if(id.endsWith(id)){
			return true;
		}
		return false;
	}

	@Override
	public String getName() {
		return "BDNUM";
	}
}
