package ix.ginas.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.module.substance.repository.CodeRepository;
import gsrs.repository.GroupRepository;
import ix.core.models.Group;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
@Component
public class CodeSequentialGenerator extends SequentialNumericIDGenerator<Substance> {
	@Autowired
	private CodeRepository codeRepository;

	@Autowired
	private GroupRepository groupRepository;

	private final CachedSupplier<AtomicLong> lastNum;
	private String codeSystem;
	private String name;

	public CodeRepository getCodeRepository() {
		return codeRepository;
	}

	public void setCodeRepository(CodeRepository codeRepository) {
		this.codeRepository = codeRepository;
	}

	public String getCodeSystem() {
		return codeSystem;
	}

	public void setCodeSystem(String codeSystem) {
		this.codeSystem = codeSystem;
	}

	protected Comparator<String> getCodeSystemComparator(){
		return Comparator.comparing(code-> Long.parseLong(code.replaceAll(suffix+"$", "")));
	}
	@JsonCreator
	public CodeSequentialGenerator(@JsonProperty("name") String name,
								   @JsonProperty("len") int len,
								   @JsonProperty("suffix") String suffix,
								   @JsonProperty("padding") boolean padding,
								   @JsonProperty("codeSystem") String codeSystem) {
		super(len, suffix, padding);
		this.name = name;
		this.codeSystem = codeSystem;
		this.lastNum = CachedSupplier.runOnce(this::findHighestValueCode);
	}


		//this method must be in @Transactional so the underlying connection for the stream stays open
		@Transactional(readOnly = true)
		protected AtomicLong findHighestValueCode() {
		//need to create Stream in try-with-resource so it gets closed correctly
			try (Stream<Code> codesByCodeSystemAndCodeLike = getCodeRepository().findCodesByCodeSystemAndCodeLike(codeSystem, "%" + suffix)) {
				String lastCode = codesByCodeSystemAndCodeLike
						.map(Code::getCode)
						.max(getCodeSystemComparator())
						.orElse("0" + suffix);
				return new AtomicLong(Long.parseLong(lastCode.replaceAll(suffix + "$", "")));
			}
		}
	@Override
	public String getName() {
		return name;
	}

	@Override
	public long getNextNumber() {
		return lastNum.getSync().incrementAndGet();
	}
	
	public Code getCode(){
		Code c = new Code();
		c.codeSystem=this.codeSystem;
		c.code=this.generateID();
		c.type="PRIMARY";
		return c;
	}
	public Code addCode(Substance s){
		Code c=getCode();
		s.codes.add(c);
		Reference r = new Reference();
		r.docType="SYSTEM";
		r.citation="System Generated Code";
		Group g = groupRepository.findByNameIgnoreCase("protected");
		if(g ==null){
			g= new Group("protected");
		}
		r.addRestrictGroup(g);
		c.addRestrictGroup(g);
		c.addReference(r, s);
		return c;
	}

	@Override
	public boolean isValidId(String id) {
		if(id.endsWith(id)){
			return true;
		}
		return false;
	}
	
	

}
