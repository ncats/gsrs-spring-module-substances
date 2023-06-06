package gsrs.module.substance.comparators;

import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ExportingSubstanceComparator implements Serializable, Comparator<Substance> {
    @Override
    public int compare(Substance substance1, Substance substance2) {
        log.trace("starting ExportingSubstanceComparator.compare");
        AtomicInteger result = new AtomicInteger(0);
        if( substance1.getDependsOnSubstanceReferencesAndParents().stream().anyMatch(sr->
            substance2.getUuid()!=null && sr.v().refuuid.equals(substance2.getUuid().toString())
                    || (substance2.getApprovalID()!=null && sr.v().approvalID.equals(substance2.getApprovalID())))) {
            log.trace("detected that substance1 depends on substance2");
            result.set(1);
        } else if( substance2.getDependsOnSubstanceReferencesAndParents().stream().anyMatch(sr->
                substance1.getUuid()!=null && sr.v().refuuid.equals(substance1.getUuid().toString())
                || (substance1.getApprovalID()!=null && sr.v().approvalID.equals(substance1.getApprovalID())))) {
            log.trace("detected that substance2 depends on substance1");
            result.set(-1);
        } else {
            log.trace("neither depends on the other");
        }
        return result.get();
    }
}
