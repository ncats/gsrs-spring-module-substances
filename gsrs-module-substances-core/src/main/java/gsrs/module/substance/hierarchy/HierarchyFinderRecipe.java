package gsrs.module.substance.hierarchy;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gsrs.GsrsUtils;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HierarchyFinderRecipe {
    private String renameChildTo;
    private String renameChildLambda;
    private String relationship;
    private Boolean invertible;

    public String getRenameChildTo() {
        return renameChildTo;
    }

    public void setRenameChildTo(String renameChildTo) {
        this.renameChildTo = renameChildTo;
    }

    public String getRenameChildLambda() {
        return renameChildLambda;
    }

    public void setRenameChildLambda(String renameChildLambda) {
        this.renameChildLambda = renameChildLambda;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public Boolean getInvertible() {
        return invertible;
    }

    public void setInvertible(Boolean invertible) {
        this.invertible = invertible;
    }

    public SubstanceHierarchyFinder.HierarchyFinder makeFinder() throws Exception {
        SubstanceHierarchyFinder.HierarchyFinder finder = null;
        if (Boolean.TRUE.equals(invertible)) {
            finder = new SubstanceHierarchyFinder.InvertibleRelationshipHierarchyFinder(relationship);
        } else { // if not set then not invertible ?
            finder = new SubstanceHierarchyFinder.NonInvertibleRelationshipHierarchyFinder(relationship);
        }
        if (renameChildTo != null) {
            finder = finder.renameChildType(renameChildTo);
        } else if (renameChildLambda != null) {
            finder = finder.renameChildType(toLambda(renameChildLambda));
        }
        return finder;
    }

    private static BiFunction<Substance,Substance,String> toLambda(String lambdaString) throws Exception{
        List<ClassLoader> cls = Stream.of(Substance.class.getClassLoader(),
                SubstanceHierarchyFinder.class.getClassLoader()
                ).collect(Collectors.toList());
        if(cls.stream().allMatch(cc->cc==null)) {
            log.error("one of the classloaders is null");
        }
        for(int i=0;i<cls.size();i++) {
            ClassLoader cl1= cls.get(i);
            try {
                return GsrsUtils.toLambdaBiFunction(cl1, lambdaString,
                        new GsrsUtils.LambdaTypeReference<BiFunction<Substance, Substance, String>>() {},
                        Substance.class);
            }catch(Exception e) {
                log.warn("classloader " + i + " failed",e );
                if(i==cls.size()-1) {
                    throw e;
                }
            }
        }
        //Not possible to get here
        throw new IllegalStateException("No suitable classloader to compile lambda");
    }
    
}
