package gsrs.module.substance.scrubbers;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;
import gov.nih.ncats.common.Tuple;
import gov.nih.ncats.common.stream.StreamUtil;
import gov.nih.ncats.common.util.Unchecked;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.EntityFetcher;
import ix.core.util.EntityUtils;
import ix.ginas.exporters.RecordScrubber;
import ix.ginas.exporters.ScrubberParameterSchema;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Note;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class GSRSPublicScrubber<T> implements RecordScrubber<T> {
    private Set<String> groupsToInclude;
    private ScrubberParameterSchema scrubberSettings;

    public GSRSPublicScrubber( ScrubberParameterSchema scrubberSettings){
        this.scrubberSettings =scrubberSettings;
        groupsToInclude= Arrays.stream(Optional.ofNullable( scrubberSettings.getAccessGroupsToInclude()).orElse("").split("\n"))
                .map(t->t.trim())
                .filter(t->t.length()>0)
                .collect(Collectors.toSet());
    }
    //TODO remove all private access things
    //As transformer
    //1. Remove all things which have $..access!=[]
    //2. Remove modifications and properties if [definingpart].access is not []
    //3. Remove all references which are not PD=true
    //4. Remove all notes
    //5. Look for "IND[ -][0-9][0-9]*" in references, remove them

    //NOT FINISHED!!!!

    //get these things from factory... by way of parsing config object (deserialized from config file)
    // add a constructor that gives a ScrubberParameterSchema object
    //
    @Value("${gsrs.scrubber.ref.pattern1:$..references[?(@.docType=='BDNUM')].uuid}")
    private static String pattern1 ="$..references[?(@.docType=='BDNUM')].uuid";

    @Value("${gsrs.scrubber.ref.pattern2}")
    private static String pattern2="$..references[?(@.docType=='BATCH_IMPORT')].uuid";

    @Value("${gsrs.scrubber.access1}")
    private static String access1="$..notes[?(@.note)]";

    @Value("${gsrs.scrubber.access2}")
    private static String access2="$.relationships[?(@.access.length()>0)][?(@.type=='SUB_CONCEPT->SUBSTANCE')].access[?]";

    @Value("${gsrs.scrubber.access3}")
    private static String access3="$.relationships[?(@.access.length()>0)][?(@.type=='SUBSTANCE->SUB_CONCEPT')].access[?]";

    @Autowired
    private SubstanceRepository substanceRepository;

    public String restrictedJSONSimple(String s) {

        DocumentContext dc = JsonPath.parse(s);
        Predicate accessPredicate = context -> {
            System.out.println("hello from predicate");
            log.trace("hello from lambda");
            Map ref=context.item(Map.class);
            List<String> access=(List<String>)ref.get("access");
            if(access.stream().anyMatch(ac->groupsToInclude.contains(ac))) {
                return false;
            }
            return true;
        };
        System.out.println("before delete");
        //dc.delete("$..[?(@.access.length()>0)]", accessPredicate);
        System.out.println("after delete");
        return dc.jsonString();
    }


        public String restrictedJSON(String s){

        DocumentContext dc = JsonPath.parse(s);


        Set<String> definitelyRemoveReferences=
                StreamUtil.with(((JSONArray)dc.read("$..references[?(@.access.length()>0)].uuid")).stream())
                        .and(((JSONArray)dc.read(pattern1)).stream())
                        .and(((JSONArray)dc.read(pattern2)).stream())
                        .stream()
                        .map(s1->s1.toString())
                        .distinct()
                        .collect(Collectors.toSet());

        Set<String> probablyRemoveReferences=
                StreamUtil.with(((JSONArray)dc.read("$..references[?(@.publicDomain==false)].uuid")).stream())
                        .and(((JSONArray)dc.read("$..references[?(@.access.length()>0)].uuid")).stream())
                        .and(((JSONArray)dc.read(pattern1)).stream())
                        .and(((JSONArray)dc.read(pattern2)).stream())
                        .stream()
                        .map(s1->s1.toString())
                        .distinct()
                        .filter(r->!definitelyRemoveReferences.contains(r))
                        .collect(Collectors.toSet());
        //BATCH_IMPORT


        Map<String, LinkedHashMap> referenceLookup=((List<LinkedHashMap>)dc.read("$.references"))
                .stream()
                .map(o-> Tuple.of(o.get("uuid")+"", o))
                .collect(Tuple.toMap());


        boolean defprivate= Stream.of("$..structure[?(@.access.length()>0)]",
                        "$..protein[?(@.access.length()>0)]",
                        "$..mixture[?(@.access.length()>0)]",
                        "$..structurallyDiverse[?(@.access.length()>0)]",
                        "$..polymer[?(@.access.length()>0)]",
                        "$..nucleicAcid[?(@.access.length()>0)]",
                        "$..specifiedSubstance[?(@.access.length()>0)]")
                .map(jp->(JSONArray)dc.read(jp))
                .filter(ja->ja.size()>0)
                .findAny()
                .isPresent();



        String ptName = dc.read("$._name").toString();

        dc.delete("$..notes[?(@.note)]");

        /*
        dc.delete(access1, (r)->{
            //System.out.println("AM HURRAY!");
            //java.awt.Toolkit.getDefaultToolkit().beep();
            return true;
        });

        //SUB_CONCEPT->SUBSTANCE

        dc.delete(access2, (r)->{
            //System.out.println("AM HURRAY!");
            //java.awt.Toolkit.getDefaultToolkit().beep();
            return true;
        });

        dc.delete(access3, (r)->{
            //System.out.println("AM HURRAY!");
            //java.awt.Toolkit.getDefaultToolkit().beep();
            return true;
        });
         */

			/*
			Stream.of("$..structure[?(@.references.length()==0)]",
				     "$..protein[?(@.references.length()==0)]",
				     "$..mixture[?(@.references.length()==0)]",
				     "$..structurallyDiverse[?(@.references.length()==0)]",
				     "$..polymer[?(@.references.length()==0)]",
				     "$..nucleicAcid[?(@.references.length()==0)]",
				     "$..specifiedSubstance[?(@.references.length()==0)]")
			  .map(jp->(JSONArray)dc.read(jp))
		      .filter(ja->ja.size()>0)
		      .findAny()
		      .isPresent();
			*/

        //Delete all things which are marked protected
        String debugLine = dc.read("$..[?(@.access.length()>0)]").toString();
        log.trace(debugLine);
        System.out.println("before delete");
        System.out.println(dc.jsonString());
        Predicate accessPredicate = context -> {
            System.out.println("hello from predicate");
            log.trace("hello from lambda");
            /*Map ref=context.item(Map.class);
            List<String> access=(List<String>)ref.get("access");
            if(access.stream().anyMatch(ac->groupsToInclude.contains(ac))) {
                return false;
            }
            return true;*/
            return false;
        };
        System.out.println("before read");
        String predicated="";
        //dc.read(s,accessPredicate);
        //System.out.println("after predicate");
        //System.out.println(predicated);
        try {
            dc.delete("$..[?(@.access.length()>0)]", accessPredicate);
        } catch (Exception ex){
            System.err.println("error: " + ex.getMessage());
        }

        return dc.jsonString();
/*

        dc.delete("$..[?(@.access.length()>0)]", js->{
            System.out.println("hello from lambda");
            log.trace("hello from lambda");
            Map ref=js.item(Map.class);
            List<String> access=(List<String>)ref.get("access");
            if(access.stream().anyMatch(ac->groupsToInclude.contains(ac))) {
                return false;
            }
            return true;
        });
        System.out.println("after delete");
        System.out.println(dc.jsonString());

        if(defprivate){
            dc.delete("$..properties");
            dc.delete("$..modifications.structuralModifications");
            dc.delete("$..modifications.agentModifications");
            dc.delete("$..modifications.physicalModifications");
            dc.delete("$..moieties");
        }




        //Delete bad references
        dc.delete("$..references[?(@.docType=='BDNUM')]");
        dc.delete("$..references[?(@.docType=='BATCH_IMPORT')]");

        dc.delete("$.references[?]", (a)->{
            Map ref=a.item(Map.class);

            boolean b=(" " + ref.get("citation")+"").matches(".*[ ]IND[ -]*[_]*[0-9][0-9][0-9]*.*") ||
                    (ref.get("docType")+"").equals("IND") ||
                    (" " + ref.get("citation")+"").trim().equals("IND");
            if(b){
                definitelyRemoveReferences.add(ref.get("uuid").toString());

                log.warn("IND-like citation: " + dc.read("$.uuid") + "\t" +dc.read("$.approvalID")+ "\t" + ref.get("docType")+ "\t" + ref.get("citation"));
                java.awt.Toolkit.getDefaultToolkit().beep();
                Unchecked.uncheck(()->Thread.sleep(10_000));
                try(PrintWriter pw = new PrintWriter(new FileOutputStream(
                        new File("ind-like-references.txt"),
                        true */
/* append = true *//*
))){
                    pw.println("IND-like citation: " + dc.read("$.uuid") + "\t" +dc.read("$.approvalID")+ "\t" + ref.get("docType")+ "\t" + ref.get("citation"));

                }catch(Exception e){
                    e.printStackTrace();
                }
            }else{
                if((" " + ref.get("citation")).contains("NO_UNII")){
                    definitelyRemoveReferences.add(ref.get("uuid").toString());
                    b=true;
                }
            }
            return b;
        });

        //TODO:
        //remove non-public references of the first kind (not PD)
        String definitelyRemoveRSet=definitelyRemoveReferences.stream().collect(Collectors.joining("','"));
        dc.delete("$..references[?(@ in ['" + definitelyRemoveRSet + "'])]");

        dc.delete("$.names[?(@.references.length()==0)][?]", (f)->{


            try(PrintWriter pw = new PrintWriter(new FileOutputStream(
                    new File("unreferencedNames.txt"),
                    true */
/* append = true *//*
))){

                Map m = f.item(Map.class);
                String appid = "";

                try{
                    appid=dc.read("$.approvalID").toString();
                }catch(Exception e3){

                }
                pw.println(m.get("name") + "\t" + dc.read("$.uuid").toString() + "\t" + appid + "\t" + ptName + "\t" + "NO PUBLIC ACCESS" + "\t" + m.get("lastEdited")+ "\t" + m.get("lastEditedBy"));
                System.out.println(m.get("name") + "\t" + dc.read("$.uuid").toString() + "\t" + appid + "\t" + ptName+ "\t" + "NO PUBLIC ACCESS" + "\t" + m.get("lastEdited")+ "\t" + m.get("lastEditedBy"));

                //java.awt.Toolkit.getDefaultToolkit().beep();
                //Unchecked.uncheck(()->Thread.sleep(0_500));

            }catch(Exception e){
                e.printStackTrace();

                //java.awt.Toolkit.getDefaultToolkit().beep();
                //Unchecked.uncheck(()->Thread.sleep(10_000));
            }
            return true;
        });


        //TODO:
        //remove references with public domain not set to true (?)
        String probablyRemoveRset=probablyRemoveReferences.stream().collect(Collectors.joining("','"));

        //dc.delete("$.references[?(@.uuid in ['" + probablyRemoveRset + "'])]");
        //dc.delete("$..references[?(@ in ['" + probablyRemoveRset + "'])]");

        Set<String> allUnderscores = new HashSet<String>();

        Filter mytest = Filter.filter(new Predicate(){

            @Override
            public boolean apply(PredicateContext arg0) {
                Map m = arg0.item(Map.class);
                if(m==null)return false;
                ((Stream<Object>)m.keySet()
                        .stream())
                        .map(k->k.toString())
                        .filter(k->k.startsWith("_"))
                        .forEach(k->{
                            allUnderscores.add(k);
                        });
                return false;
            }

        });
        dc.read("$..[?]",mytest);

        allUnderscores.stream()
                .forEach(u->{
                    dc.delete("$.." + u);
                });

        //TODO: probably add references for these?
        //System.out.println((Object)dc.read("$.names[?(@.references.length()==0)]"));

        Set<String> keepRefs = new LinkedHashSet<String>();

        //Look through names to decide if we should keep some references

        dc.delete("$.names[?(@.references[?(@ in ['" + probablyRemoveRset + "'])])][?]", (f)->{
            long okIfFrom=1493355528008l;
            String okIfBy="DAMMIKA.AMUGODA-KANK";

            try(PrintWriter pw = new PrintWriter(new FileOutputStream(
                    new File("unreferencedNames.txt"),
                    true */
/* append = true *//*
))){

                Map m = f.item(Map.class);


                String name=m.get("name")+"";
                List<String> nameReferences = ((List<String>) m.get("references")).stream()
                        .sorted()
                        .distinct()
                        .collect(Collectors.toList());

                List<String> problemReferences = nameReferences.stream()
                        .filter(r->probablyRemoveReferences.contains(r))
                        .collect(Collectors.toList());


                if(problemReferences.isEmpty()){
                    return false;
                }else if(problemReferences.size() != nameReferences.size()){
                    //don't remove name if it has some other reference,
                    //not in the delete list
                    return false;
                }

                String appid = "";
                String by=m.get("lastEditedBy")+"";

                long from=Long.parseLong(m.get("lastEdited")+"");

                String keepRUUID = problemReferences.get(0);

                LinkedHashMap refObj = referenceLookup.get(keepRUUID);

                String refDocType = refObj.get("docType")+"";
                String refCit = refObj.get("citation")+"";
                String refurl = refObj.get("url")+"";
                String refcreated = refObj.get("created")+"";
                String refcreatedBy = refObj.get("createdBy")+"";


                String log=m.get("name") + "\t" + dc.read("$.uuid").toString() + "\t" + appid + "\t" + ptName + "\tNO PUBLIC DOMAIN"
                        + "\t" + m.get("lastEdited")
                        + "\t" + m.get("lastEditedBy")
                        + "\t" + refDocType
                        + "\t" + refCit
                        + "\t" + refurl
                        + "\t" + refcreated
                        + "\t" + refcreatedBy;

                if(from>okIfFrom && okIfBy.equals(by)){

                    //This is for a special exception

                    //keepRefs.add(keepRUUID);
                    //log+="\tKEEPING";
                }else{
                    //log+="\tREMOVING";
                }
                pw.println(log);
                System.out.println(log);

            }catch(Exception e){
                e.printStackTrace();
                java.awt.Toolkit.getDefaultToolkit().beep();
                Unchecked.uncheck(()->Thread.sleep(10_000));
            }
            return false;
        });

        probablyRemoveReferences.removeAll(keepRefs);

        String reallyRemoveRset=probablyRemoveReferences.stream().collect(Collectors.joining("','"));

        dc.delete("$.references[?(@.uuid in ['" + reallyRemoveRset + "'])]");
        dc.delete("$..references[?(@ in ['" + reallyRemoveRset + "'])]");



        boolean isDefNoReferences = Stream.of("$..structure[?(@.references.length()==0)]",
                        "$..protein[?(@.references.length()==0)]",
                        "$..mixture[?(@.references.length()==0)]",
                        "$..structurallyDiverse[?(@.references.length()==0)]",
                        "$..polymer[?(@.references.length()==0)]",
                        "$..nucleicAcid[?(@.references.length()==0)]",
                        "$..specifiedSubstance[?(@.references.length()==0)]")
                .map(jp->(JSONArray)dc.read(jp))
                .filter(ja->ja.size()>0)
                .findAny()
                .isPresent();

        if(isDefNoReferences){
            try(PrintWriter pw = new PrintWriter(new FileOutputStream(
                    new File("uniiDefinitions.txt"),
                    true */
/* append = true *//*
))){

                //java.awt.Toolkit.getDefaultToolkit().beep();
                //System.out.println(dc.read("$.approvalID").toString());
                pw.println(dc.read("$.approvalID").toString());
                //Unchecked.uncheck(()->Thread.sleep(10_000));
            }catch(Exception e){

            }
        }

        dc.delete("$.names[?(@.references.length()==0)][?]", (f)->{
            return true;
        });

        //TODO: remove lastEdited / lastEditedBy / deprecated=false
        dc.delete("$..lastEdited");
        dc.delete("$..lastEditedBy");
        dc.delete("$..created");
        dc.delete("$..createdBy");
        dc.delete("$..[?(@.deprecated==false)].deprecated");

        //dc.delete("$..[?(@.references.length()==0)].references");
        dc.delete("$..[?(@.access.length()==0)].access");
        dc.delete("$..[?(@.sites.length()==0)].sites");

        dc.delete("$.codes[?(@.codeSystem=='BDNUM')]");
        dc.delete("$.codes[?(@.codeSystem=='FARM SUBSTANCE ID')]");
        dc.delete("$.codes[?(@.codeSystem=='CFSAN PSEUDO CAS')]");
        dc.delete("$.codes[?(@.codeSystem=='DASH INDICATION')]");
        dc.delete("$.codes[?(@.codeSystem=='USP-MC')]");
        dc.delete("$.codes[?(@.codeSystem=='STARI')]");
        dc.delete("$.codes[?(@.codeSystem=='CERES')]");

        dc.delete("$.tags[?]",(f)->{
            if(f.item(String.class).equals("DASH")){
                //java.awt.Toolkit.getDefaultToolkit().beep();
                //System.out.println("DASH!");

                //Unchecked.uncheck(()->Thread.sleep(10_000));
                return true;
            }
            return false;
        });


        dc.delete("$.changeReason");

        //Need to fix SRUs???


        //
        //FARM SUBSTANCE ID

        //remove all _ keys
        //System.out.println((Object)dc.read("$..[?]*", mytest));

        try{
            String apby = dc.read("$.approvedBy");

            if(apby!=null){
                dc.set("$.approvedBy", "FDA_SRS");
                dc.delete("$..approved");
            }
        }catch(Exception e){

        }

        //Ok, remove references?
        String oldClass = dc.read("$.substanceClass");

        //IF the substance is a specific thing, but its definition is private,
        //put it in as a concept for now
        if(defprivate){


            dc.set("$.substanceClass", "concept");
            dc.set("$.definitionLevel", "INCOMPLETE");
            Note n = new Note();
            n.note ="This concept is an incompletely defined " + oldClass + " substance. A more complete definition may be available later";

            dc.add("$..notes", toMap(n));
            //System.out.println(CommonTransformers.JSON_TO_GSRS_SUBSTANCE.apply(dc.jsonString()).getNotes().get(0).getNote());
            //java.awt.Toolkit.getDefaultToolkit().beep();
            //Unchecked.uncheck(()->Thread.sleep(10_000));
        }else if(oldClass.equals("polymer")){
            dc.delete("$..structuralUnits[?(@.label)]");
            dc.set("$.substanceClass", "polymer");
            dc.set("$.definitionLevel", "INCOMPLETE");

            Note n = new Note();
            n.note = "This is an incomplete polymer record. A more detailed definition may be available soon.";
            dc.add("$..notes", toMap(n));
        }

        return dc.jsonString();
    */
    }

    private static Map toMap(Note note){
        Map m =new HashMap();
        m.put("note", note);
        return m;
    }

    @SneakyThrows
    @Override
    public Optional scrub(Object object) {
        log.trace("starting in scrub");
        if( !(object instanceof Substance)){
            log.warn("scrubber called on object other than Substance");
            return Optional.of(object);
        }
        Substance substance = (Substance)object;
        log.trace("cast to substance with UUID {}", substance.uuid.toString());
        String substanceJson;
        try {
            substanceJson = substance.toFullJsonNode().toString();
        } catch (Exception ex){
            log.error("Error retrieving substance; using alternative method");
            EntityUtils.Key skey = EntityUtils.Key.of(Substance.class, substance.uuid);
            Optional<Substance> substanceRefetch = EntityFetcher.of(skey).getIfPossible().map(o->(Substance)o);
            substanceJson = substanceRefetch.get().toFullJsonNode().toString();
        }
        log.trace("got json");
        try {
            String cleanJson= restrictedJSONSimple( substanceJson);
            log.trace("cleaned JSON");
            return Optional.of( SubstanceBuilder.from(cleanJson).build());
        }
        catch (Exception ex) {
            log.warn("error processing record; Will return empty", ex);
        }
        return Optional.empty();
    }
}
