package gsrs.module.substance.definitional;

import gsrs.module.substance.services.DefinitionalElementFactory;
import gsrs.module.substance.services.DefinitionalElementImplementation;
import ix.core.models.Keyword;
import ix.ginas.models.v1.Parameter;
import ix.ginas.models.v1.Property;
import ix.ginas.models.v1.StructurallyDiverseSubstance;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
@Slf4j
public class StructurallyDiverseDefinitionalElementImpl implements DefinitionalElementImplementation {

    @Autowired
    private DefinitionalElementFactory definitionalElementFactory;

    public DefinitionalElementFactory getDefinitionalElementFactory() {
        return definitionalElementFactory;
    }

    public void setDefinitionalElementFactory(DefinitionalElementFactory definitionalElementFactory) {
        this.definitionalElementFactory = definitionalElementFactory;
    }

    @Override
    public boolean supports(Object s) {
        return s instanceof StructurallyDiverseSubstance;
    }

    @Override
    public void computeDefinitionalElements(Object s, Consumer<DefinitionalElement> consumer) {
            StructurallyDiverseSubstance substance = (StructurallyDiverseSubstance) s;

		/*
		Factors in this type of substance:
		1) Parent layer 1
    2) Taxonomy (if applicable) layer 1 (Except for author which is layer 2)
    3) Part layer 1
    4) Part location layer 2
    5) Fraction (both name and materialtype) layer 1
    6) Source material class layer 1
    7) Source material type layer 1
		8) Modifications (3 types) layer 2
		9) Properties (values and parameter values) layer 2
		10) adding developmentalStage to layer 2 on 28 December 2020
		*/
            log.debug("in StructurallyDiverse additionalElementsFor");
           
            if( substance.structurallyDiverse.parentSubstance != null) {
                DefinitionalElement parentElement = DefinitionalElement.of("structurallyDiverse.parentSubstance.refuuid",
                        substance.structurallyDiverse.parentSubstance.refuuid, 1);
                consumer.accept(parentElement);
                log.trace("adding parent to the def hash: " + substance.structurallyDiverse.parentSubstance.refuuid);
            }

            if( substance.structurallyDiverse.organismFamily != null && substance.structurallyDiverse.organismFamily.length() > 0) {
                DefinitionalElement familyElement = DefinitionalElement.of("structurallyDiverse.organismFamily",
                        substance.structurallyDiverse.organismFamily.toUpperCase(), 1);
                consumer.accept(familyElement);
                log.trace("adding family to the def hash: " + substance.structurallyDiverse.organismFamily.toUpperCase());
            }

            if( substance.structurallyDiverse.organismGenus != null && substance.structurallyDiverse.organismGenus.length() > 0) {
                DefinitionalElement genusElement = DefinitionalElement.of("structurallyDiverse.organismGenus",
                        substance.structurallyDiverse.organismGenus.toUpperCase(), 1);
                consumer.accept(genusElement);
                log.trace("adding genus to the def hash: " + substance.structurallyDiverse.organismGenus.toUpperCase());
            }
            if( substance.structurallyDiverse.organismSpecies != null && substance.structurallyDiverse.organismSpecies.length() > 0) {
                DefinitionalElement speciesElement = DefinitionalElement.of("structurallyDiverse.organismSpecies",
                        substance.structurallyDiverse.organismSpecies.toUpperCase(), 1);
                consumer.accept(speciesElement);
                log.trace("adding species to the def hash: " + substance.structurallyDiverse.organismSpecies);
            }
            if( substance.structurallyDiverse.organismAuthor != null &&  substance.structurallyDiverse.organismAuthor.length() > 0) {
                DefinitionalElement authorElement = DefinitionalElement.of("structurallyDiverse.organismAuthor",
                        substance.structurallyDiverse.organismAuthor.toUpperCase(), 2);
                consumer.accept(authorElement);
                log.trace("adding author to the def hash: " + authorElement);
            }

            if( substance.structurallyDiverse.part != null && substance.structurallyDiverse.part.size() >0) {
                for(Keyword p : substance.structurallyDiverse.part){
                    //log.debug(String.format("part href: %s; id: %s; label: %s; term: %s",	p.href, p.id, p.label, p.term));
                    DefinitionalElement partElement = DefinitionalElement.of("structurallyDiverse.part",
                            (p.term !=null && p.term.length() >0) ? p.term.toUpperCase() : p.term, 1);
                    consumer.accept(partElement);
                    log.trace("adding part to the def hash: " + partElement);
                }
            }

            if( substance.structurallyDiverse.partLocation != null && substance.structurallyDiverse.partLocation.length() > 0) {
                DefinitionalElement partLocationElement = DefinitionalElement.of("structurallyDiverse.partLocation",
                        substance.structurallyDiverse.partLocation.toUpperCase(), 2);
                consumer.accept(partLocationElement);
            }
            if( substance.structurallyDiverse.sourceMaterialClass != null && substance.structurallyDiverse.sourceMaterialClass.length() > 0)
            {
                DefinitionalElement sourceMaterialClassElement = DefinitionalElement.of("structurallyDiverse.sourceMaterialClass",
                        substance.structurallyDiverse.sourceMaterialClass.toUpperCase(), 1);
                consumer.accept(sourceMaterialClassElement);
                log.trace("adding sourceMaterialClass to the def hash: " + substance.structurallyDiverse.sourceMaterialClass.toUpperCase());
            }
            if( substance.structurallyDiverse.sourceMaterialType != null && substance.structurallyDiverse.sourceMaterialType.length() > 0)
            {
                DefinitionalElement sourceMaterialTypeElement = DefinitionalElement.of("structurallyDiverse.",
                        substance.structurallyDiverse.sourceMaterialType.toUpperCase(), 1);
                consumer.accept(sourceMaterialTypeElement);
                log.trace("adding sourceMaterialType to the def hash: " + substance.structurallyDiverse.sourceMaterialType);
            }
            if( substance.structurallyDiverse.fractionName != null && substance.structurallyDiverse.fractionName.length() >0) {
                DefinitionalElement fractionNameElement = DefinitionalElement.of("structurallyDiverse.fractionName",
                        substance.structurallyDiverse.fractionName.toUpperCase().trim(), 1);
                consumer.accept(fractionNameElement);
                log.trace("adding fractionName to the def hash: " + substance.structurallyDiverse.fractionName.toUpperCase().trim());
            }

            if( substance.structurallyDiverse.fractionMaterialType != null && substance.structurallyDiverse.fractionMaterialType.length() >0) {
                DefinitionalElement fractionTypeElement = DefinitionalElement.of("structurallyDiverse.fractionMaterialType",
                        substance.structurallyDiverse.fractionMaterialType.toUpperCase().trim(), 1);
                consumer.accept(fractionTypeElement);
                log.trace("adding fractionMaterialType to the def hash: " + substance.structurallyDiverse.fractionMaterialType.toUpperCase().trim());
            }

            if( substance.structurallyDiverse.infraSpecificType != null && substance.structurallyDiverse.infraSpecificType.length() >0 ) {
                DefinitionalElement infraSpecTypeElement = DefinitionalElement.of("structurallyDiverse.infraSpecificType",
                        substance.structurallyDiverse.infraSpecificType.toUpperCase(), 2);
                consumer.accept(infraSpecTypeElement);
                log.trace("adding infraSpecificType to the def hash: " + substance.structurallyDiverse.infraSpecificType.toUpperCase());
            }

            if( substance.structurallyDiverse.infraSpecificName != null && substance.structurallyDiverse.infraSpecificName.length() >0 ) {
                DefinitionalElement infraSpecNameElement = DefinitionalElement.of("structurallyDiverse.infraSpecificName",
                        substance.structurallyDiverse.infraSpecificName.toUpperCase(), 2);
                consumer.accept(infraSpecNameElement);
                log.trace("adding infraSpecificName to the def hash: " + substance.structurallyDiverse.infraSpecificName.toUpperCase());
            }

            if (substance.structurallyDiverse.developmentalStage != null && substance.structurallyDiverse.developmentalStage.length() > 0) {
                DefinitionalElement devStageElement = DefinitionalElement.of("structurallyDiverse.developmentalStage",
                        substance.structurallyDiverse.developmentalStage.toUpperCase(), 2);
                log.trace("adding developmentalStage to the def hash: " + substance.structurallyDiverse.developmentalStage.toUpperCase());
                consumer.accept(devStageElement);
            }
            if( substance.modifications != null ){
                definitionalElementFactory.addDefinitionalElementsFor(substance.modifications, consumer);

            }

            if( substance.properties != null ) {
                for(Property property : substance.properties) {
                    if(property.isDefining() && property.getValue() != null) {
                        String defElementName = String.format("properties.%s.value",
                                property.getName());
                        DefinitionalElement propertyValueDefElement =
                                DefinitionalElement.of(defElementName, property.getValue().toString(), 2);
                        consumer.accept(propertyValueDefElement);
                        log.trace("added def element for property " + defElementName);
                        for(Parameter parameter : property.getParameters()) {
                            defElementName = String.format("properties.%s.parameters.%s.value",
                                    property.getName(), parameter.getName());
                            if( parameter.getValue() != null) {
                                DefinitionalElement propertyParamValueDefElement =
                                        DefinitionalElement.of(defElementName,
                                                parameter.getValue().toString(), 2);
                                consumer.accept(propertyParamValueDefElement);
                                log.trace("added def element for property parameter " + defElementName);
                            }
                        }
                    }
                }
            }
    }
}
