package gsrs.module.substance.comparators;

import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Comparator;

@Slf4j
public class ExportingSubstanceComparator implements Serializable, Comparator<Substance> {
    @Override
    public int compare(Substance substance1, Substance substance2) {
        if(substance1 ==null || substance2==null){
            log.trace("one or more parameters is null");
            if( substance1 == null  && substance2!=null) return -1;
            if( substance2 == null  && substance1!=null) return 1;
            return 0;
        }

        log.trace("starting ExportingSubstanceComparator.compare 1 {} - 2 {}", substance1.getUuid() != null ? substance1.getUuid().toString() : "[no id]",
                substance2.getUuid() != null ? substance2.getUuid().toString() : "[no id]");
        if( substance1.getDependsOnSubstanceReferencesAndParents().stream().anyMatch(sr->
            substance2.getUuid()!=null && sr.v().refuuid.equals(substance2.getUuid().toString())
                    || (substance2.getApprovalID()!=null && sr.v().approvalID!=null && sr.v().approvalID.equals(substance2.getApprovalID())))) {
            log.trace("detected that substance1 depends on substance2");
            return 1;
        } else if( substance2.getDependsOnSubstanceReferencesAndParents().stream().anyMatch(sr->
                substance1.getUuid()!=null && sr.v().refuuid.equals(substance1.getUuid().toString())
                    || (substance1.getApprovalID()!=null && sr.v().approvalID!=null && sr.v().approvalID.equals(substance1.getApprovalID())))) {
            log.trace("detected that substance2 depends on substance1");
            return -1;
        } else {
            if( substance1.getName() != null && substance2.getName()!=null){
                return substance1.getName().compareTo(substance2.getName());
            }

            if( substance1.getApprovalID()!=null && substance2.getApprovalID()!=null) {
                return substance1.getApprovalID().compareTo(substance2.getApprovalID());
            }

            if(substance1.getUuid()!=null && substance2.getUuid()!=null) {
                return substance1.getUuid().compareTo(substance2.getUuid());
            }
            log.trace("neither depends on the other");
            try {
                return substance1.getDefinitionalHash().compareTo(substance2.getDefinitionalHash());
            } catch (Exception ignore) {
            }
            if( substance1.equals(substance2)) return 0;//equals seems to be returning an invalid value. Fixing that is a job for another day!
        }
        return -1;//returning 0 implies the 2 objects are equal
    }
}
