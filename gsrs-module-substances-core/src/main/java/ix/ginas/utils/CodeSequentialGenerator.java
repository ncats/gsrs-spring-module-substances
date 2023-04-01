package ix.ginas.utils;

import gsrs.module.substance.services.CodeEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import gsrs.module.substance.repository.CodeRepository;
import ix.core.models.Group;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CodeSequentialGenerator extends SequentialNumericIDGenerator<Substance> {

	private static final String GROUP_PROTECTED = "protected";

	@Autowired
	private CodeRepository codeRepository;

	@Autowired
	private CodeEntityService codeEntityService;

	private String codeSystem;
	private String name;
	private Long max;

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

	@JsonCreator
	public CodeSequentialGenerator( @JsonProperty("name") String name,
					@JsonProperty("len") int len,
					@JsonProperty("suffix") String suffix,
					@JsonProperty("padding") boolean padding,
					@JsonProperty("max") Long max,
					@JsonProperty("codeSystem") String codeSystem) {
		super(len, suffix, padding);
		this.name = name;
		this.max = max;
		this.codeSystem = codeSystem;
	}


	@Override
	public String getName() {
		return name;
	}

	@Override
	public long getNextNumber() {
		long nextNumber = 1L;
		try {
			nextNumber = codeRepository.findMaxCodeByCodeSystemAndCodeLikeAndCodeLessThen(codeSystem, "%" + suffix, max).longValue() + 1L;
		} catch (Exception e) {
		}
		if (nextNumber > max) {
			//TODO: What shall we do if the result next number value out of range?
			//throw new Exception("Next number out of range.");
		}
		return nextNumber;
	}
	
	public Code getCode(){
		Code c = new Code();
		c.codeSystem=this.codeSystem;
		c.code=this.generateID();
		c.type="PRIMARY";
		return c;
	}

	public Code addCode(Substance s){
		return codeEntityService.createNewSystemCode(s, this.codeSystem,c-> this.generateID(),GROUP_PROTECTED);
	}

	public Long getMax() {
		return max;
	}

	public void setMax(Long max) {
		this.max = max;
	}

	@Override
	public boolean isValidId(String id) {
		if(id.endsWith(id)){
			return true;
		}
		return false;
	}
}
