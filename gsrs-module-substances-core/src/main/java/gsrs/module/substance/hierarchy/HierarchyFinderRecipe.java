package gsrs.module.substance.hierarchy;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import gsrs.GsrsUtils;
import gsrs.springUtils.AutowireHelper;
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
        finder = AutowireHelper.getInstance().autowireAndProxy(finder);
        if (renameChildTo != null) {
            finder = finder.renameChildType(renameChildTo);
        } else if (renameChildLambda != null) {
            finder = finder.renameChildType(toLambda(renameChildLambda));
        }
        return finder;
    }

    private static BiFunction<Substance,Substance,String> toLambda(String lambdaString) throws Exception{
        try {
            return toLambdaFull(lambdaString);
        }catch(Exception e){
            try {
                //fallback to name-based simplified lambda
                return toLambdaSimplified(lambdaString);
            }catch(Exception e2) {
                throw e2;
            }
        }

    }

    /**
     * <p>
     * This is a method which compiles a {@link BiFunction} from 2 {@link Substance} objects into a String
     * from a supplied lambda as a string. This compiling method attempts to import the {@link Substance} packages
     * directly, allowing all methods from inside Substance to be exposed. In some configurations the compiler
     * won't work with this method, in such a case {@link #toLambdaSimplified(String)} should be used instad.
     * </p>
     * @param lambdaString
     * @return
     * @throws Exception
     */
    private static BiFunction<Substance,Substance,String> toLambdaFull(String lambdaString) throws Exception{
        
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
        throw new IllegalStateException("No suitable classloader to compile lambda:" + lambdaString);
    }
    
    /**
     * <p>
     * This is a make-shift method which compiles a {@link BiFunction} from 2 {@link Substance} objects into a String
     * from a supplied lambda as a string. This compiling method assumes that the supplied lambda is actually meant to 
     * process only the {@link Substance#getName()} method. Any other method call from the substance object will result in
     * undefined behavior. This is done in order to avoid a runtime compiler issue with visibility of the Substance package.
     * </p>
     * <p>The following lambdas are equivalent when compiled by this method</p>
     * <pre>
     * (a,b)->a.getName() + ":" + b.getName()
     * (a,b)->a + ":" + b
     *  </pre>
     * @param inp
     * @return
     * @throws Exception
     */
    private static BiFunction<Substance,Substance,String> toLambdaSimplified(String inp) throws Exception{
        // remove getName calls to instead use the root
        // this assumes that only calls to getName() will exist, and that any 
        // reference to this is for a Substance call.
        // (this assumption is pretty safe in this case, but it's not perfect)
        String modLambda = inp.replaceAll("[.]\\s*getName\\s*[(]\\s*[)]","");
        
        
        List<ClassLoader> cls = Stream.of(Substance.class.getClassLoader(),
                SubstanceHierarchyFinder.class.getClassLoader()
                ).collect(Collectors.toList());
        if(cls.stream().allMatch(cc->cc==null)) {
            log.error("one of the classloaders is null");
        }
        for(int i=0;i<cls.size();i++) {
            ClassLoader cl1= cls.get(i);
            try {
                BiFunction<String,String,String> mconcat =  GsrsUtils.toLambdaBiFunction(cl1, modLambda,
                        new GsrsUtils.LambdaTypeReference<BiFunction<String, String, String>>() {});
                
                //chain getName adapter to the compiled String-based lambda
                return (a,b)->mconcat.apply(a.getName(), b.getName());
            }catch(Exception e) {
                log.warn("classloader " + i + " failed",e );
                if(i==cls.size()-1) {
                    throw e;
                }
            }
        }
        //Not possible to get here
        throw new IllegalStateException("No suitable classloader to compile lambda:" + inp);
    }
    
}
