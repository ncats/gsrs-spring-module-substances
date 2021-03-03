package gsrs.module.substance.definitional;

import gsrs.module.substance.services.DefinitionalElementImplementation;
import ix.ginas.models.v1.AgentModification;
import ix.ginas.models.v1.Modifications;
import ix.ginas.models.v1.PhysicalModification;
import ix.ginas.models.v1.StructuralModification;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;
@Slf4j
public class ModificationsDefintionalElementImpl implements DefinitionalElementImplementation {
    @Override
    public boolean supports(Object o) {
        return o instanceof Modifications;
    }

    @Override
    public void computeDefinitionalElements(Object obj, Consumer<DefinitionalElement> consumer) {
        Modifications modifications = (Modifications) obj;
        if( modifications.agentModifications != null){
            for(AgentModification a : modifications.agentModifications){
                //DefinitionalElement agentModElement = DefinitionalElement.of("modifications.agentModificationProcess", a.agentModificationProcess, 2);
                //consumer.accept(agentModElement);
                if( a.agentSubstance != null ){
                    DefinitionalElement agentModElement = DefinitionalElement.of("modifications.agentSubstance", a.agentSubstance.refuuid, 2);
                    consumer.accept(agentModElement);
                    log.debug("processing agent modification with agent substance " + a.agentSubstance.refuuid);
                }
                if( a.amount != null) {
                    DefinitionalElement amountElement = DefinitionalElement.of("modifications.amount", a.amount.toString(), 2);
                    consumer.accept(amountElement);
                    log.debug("processing agent modification with amount " + a.amount);
                }
            }
        }
        if( modifications.physicalModifications != null) {
            for (PhysicalModification p : modifications.physicalModifications){
                if( p.modificationGroup !=null) {
                    log.debug("processing physical modification " + p.modificationGroup);
                    DefinitionalElement physicalModElement = DefinitionalElement.of("modifications.physicalModificationGroup", p.modificationGroup, 2);
                    consumer.accept(physicalModElement);
                }
                if( p.physicalModificationRole !=null) {
                    log.debug("processing p.physicalModificationRole " + p.physicalModificationRole);
                    DefinitionalElement physicalModElementProcess = DefinitionalElement.of("modifications.physicalModificationRole", p.physicalModificationRole, 2);
                    consumer.accept(physicalModElementProcess);
                }
            }
        }

        if( modifications.structuralModifications != null) {
            for (StructuralModification sm : modifications.structuralModifications){
                if( sm.modificationGroup != null ) {
                    log.debug("processing structural modification with group " + sm.modificationGroup);
                    DefinitionalElement structModElement = DefinitionalElement.of("modifications.structuralModifications.group", sm.modificationGroup, 2);
                    consumer.accept(structModElement);
                }
                if( sm.residueModified != null){
                    log.debug("processing sm.residueModified " + sm.residueModified);
                    DefinitionalElement structModResidueElement = DefinitionalElement.of("modifications.structuralModifications.residueModified", sm.residueModified, 2);
                    consumer.accept(structModResidueElement);
                }
            }
        }
    }
}
