package ix.ginas.utils;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import gov.nih.ncats.common.util.SingleThreadCounter;
import gsrs.module.substance.services.CodeEntityService;
import gsrs.services.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.module.substance.repository.CodeRepository;
import gsrs.repository.GroupRepository;
import ix.core.models.Group;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LegacyCodeSequentialGenerator extends CodeSequentialGenerator {

    private static final String GROUP_PROTECTED = "protected";

    @Autowired
	private CodeRepository codeRepository;

	@Autowired
	private CodeEntityService codeEntityService;
	
	@Autowired
	private PlatformTransactionManager  transactionManager;

	

	
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
	public LegacyCodeSequentialGenerator(@JsonProperty("name") String name,
								   @JsonProperty("len") int len,
								   @JsonProperty("suffix") String suffix,
								   @JsonProperty("padding") boolean padding,
								   @JsonProperty("codeSystem") String codeSystem) {
		super(name, len, suffix, padding, null, codeSystem);
		this.name = name;
		this.codeSystem = codeSystem;
		this.lastNum = CachedSupplier.runOnce(this::findHighestValueCode);
	}


		
		protected AtomicLong findHighestValueCode() {
		    //this method must be in transaction so the underlying connection for the stream stays open
		    //for the stream.
		    //
		    log.debug("Starting to find highest Value Codesystem");
		    
		    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
		    txTemplate.setReadOnly(true);
		    txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		    return txTemplate.execute(status -> {
				SingleThreadCounter counter = new SingleThreadCounter();
		        try (Stream<String> codesByCodeSystemAndCodeLike = getCodeRepository().findCodeByCodeSystemAndCodeLike(codeSystem, "%" + suffix)) {
	                String lastCode = codesByCodeSystemAndCodeLike
//	                        .map(Code::getCode)
				//TODO fix this. It's inefficient and also probably a source of lots of issues
				.peek(c->{
					int currentCount = counter.increment().getAsInt();
					if((currentCount  % 1000) ==0){
						log.debug("find max code count at " + currentCount);
					}
				})
	                        .max(getCodeSystemComparator())
	                        .orElse("0" + suffix);

		        log.debug("found highest value codesystem");
	                return new AtomicLong(Long.parseLong(lastCode.replaceAll(suffix + "$", "")));
	            }
		    });
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
		return codeEntityService.createNewSystemCode(s, this.codeSystem,c-> this.generateID(),GROUP_PROTECTED);
	}

	@Override
	public boolean isValidId(String id) {
		if(id.endsWith(id)){
			return true;
		}
		return false;
	}
	
	

}
