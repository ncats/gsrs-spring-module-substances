package ix.ginas.utils;

import gov.nih.ncats.common.sneak.Sneak;
import gsrs.module.substance.services.CodeEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import gsrs.module.substance.repository.CodeRepository;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
/*
  See UniqueCodeGenerator.

  Creates a code/id value that has a number part and suffix part.
  When a substance is created:

     a) get the next code id number part by looking in the database to find the highest number part previously used with the suffix.
     b) insert a code row with configured code system, type primary, and the code value generated.

  The code id number part must be less than or equal to max.
  The length parameter should  >= # of possible digits + the length of the suffix.
  for example if max = 9999999 and suffix equal AB then length should be 9.
  If you don't specify a length the default will be Long.MAX_VALUE

  Configure like so:
	gsrs.entityProcessors.list.UniqueCodeGenerator = {
		"entityClassName" = "ix.ginas.models.v1.Substance",
		"processor" = "gsrs.module.substance.processors.UniqueCodeGenerator",
		"with"=  {
			"name": "Some Name",
			"codesystem"="SOMECODESYSTEM",
			"suffix"="ZZ",
			"length"=5,
			"padding"=true,
			"max"=999,
			"groups"=["protected"]
		}
	}
 */

@Slf4j
@Component
public class CodeSequentialGenerator extends SequentialNumericIDGenerator<Substance> {

	public static final Long DEFAULT_MAX = Long.MAX_VALUE;

	@Autowired
	private CodeRepository codeRepository;

	@Autowired
	private CodeEntityService codeEntityService;

	private String codeSystem;
	private String name;
	private Long max;
	private String[] groups;

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
					@JsonProperty("codeSystem") String codeSystem,
					@JsonProperty("groups") Map<Integer, String> groups) {

		super(len, suffix, padding);
		if(max==null) { max=DEFAULT_MAX; }

		this.max = max;
		if(suffix==null) { this.suffix= "";}
		this.groups = (groups != null) ? groups.values().stream().toArray(String[]::new) : null;
		if(!this.getClass().equals(LegacyCodeSequentialGenerator.class)) {
            // Legacy could be an extension of this class, and if so these checks aren't appropriate.
			if (len < 1) {
				this.setLen(String.valueOf(this.max).length() + this.suffix.length());
			}
			if (!(this.getLen() >= String.valueOf(this.max).length() + this.suffix.length())) {
				Sneak.sneakyThrow(new Exception("The len value should be greater than or equal to the number of max's digits + the number of characters in the suffix. "
				+ String.format("These values are %s %s %s", this.getLen(), String.valueOf(this.max).length(), this.suffix.length())));
			}
		}
		this.name = name;

		// I don't get an NPE with a null code system.
		// Should we have an exception if codeSystem is null?
		this.codeSystem = codeSystem;
	}

	public boolean checkNextNumberWithinRange(Long nextNumber, Long maxNumber)  {
		Objects.requireNonNull(nextNumber, "Value for nextNumber can not be null");
		Objects.requireNonNull(maxNumber, "Value for maxNumber can not be null");
		return (nextNumber>-1 && nextNumber<=maxNumber);
	}

	public boolean checkCodeIdLength(String codeId) {
		// codeId is made from nextNumber + suffix
		// codeId length must be less than or equal to (max.length + suffix.length)
		Objects.requireNonNull(codeId, "Value for codeId can not be null");
		Objects.requireNonNull(this.max, "Value for max number can not be null");
		int maxNumberLength = String.valueOf(this.max).length();
		return codeId.length() <= this.getLen() && codeId.length() <= maxNumberLength + this.suffix.length();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public long getNextNumber() {
		long nextNumber = 1L;
		try {
			nextNumber = codeRepository.findMaxCodeByCodeSystemAndCodeLikeAndCodeLessThan(codeSystem, "%" + suffix, max)
			.longValue()
			+ 1L;
		} catch (Exception e) {
		}
		if (!checkNextNumberWithinRange(nextNumber, this.max)) {
			// TODO: Is there a better option?
			return Sneak.sneakyThrow(new Exception("The value for nextNumber is out of range."));
		}
		return nextNumber;
	}
	
	public Code getCode() {
		Code c = new Code();
		try {
			c.codeSystem=this.codeSystem;
			c.code = this.generateID();
			c.type="PRIMARY";
			if(!checkCodeIdLength(c.code)) {
				throw new Exception("Code id generated from database failed string length check.");
			}
			return c;
		} catch (Throwable t) {
			return Sneak.sneakyThrow(new Exception("Exception getting code in CodeSequentialGenerator"));
		}
	}

	public Code addCode(Substance s) {
		try {
			return codeEntityService.createNewSystemCode(s, this.codeSystem, c -> this.generateID(), this.groups);
		} catch (Throwable t) {
			return Sneak.sneakyThrow(new Exception("Throwing exception in addCode in CodeSequentialGenerator. " + ((t.getCause()!=null)?t.getCause():"")));
		}
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
