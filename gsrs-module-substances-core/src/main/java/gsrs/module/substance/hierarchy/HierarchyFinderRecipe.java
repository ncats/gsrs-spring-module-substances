package gsrs.module.substance.hierarchy;

import gsrs.GsrsUtils;
import ix.ginas.models.v1.Substance;

import java.util.function.BiFunction;

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
        return GsrsUtils.toLambdaBiFunction(SubstanceHierarchyFinder.class.getClassLoader(), lambdaString,
                new GsrsUtils.LambdaTypeReference<BiFunction<Substance, Substance, String>>() {},
                Substance.class);
    }
}
