package gsrs.module.substance.utils;

import gov.nih.ncats.molwitch.Chemical;

import java.util.*;

import gov.fda.gsrs.ndsri.FeaturizeNitrosamine;
import gov.fda.gsrs.ndsri.FeaturizeNitrosamine.FeatureJob;
import gov.fda.gsrs.ndsri.FeaturizeNitrosamine.FeatureResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeatureUtils {
    public static List<Map<String, String>> calculateFeatures(Chemical chemical) throws Exception{

        try {
            Optional<FeatureResponse> response = FeaturizeNitrosamine.forMostPotentNitrosamine(chemical);
            List<Map<String,String>> maps = new ArrayList<>();
            if( response.isPresent()){
                FeatureResponse r = response.get();
                Map<String, String> ret = new HashMap<>();
                r.getFeatureSet().entrySet()
			         .stream()
			//TODO make configurable, but for now just remove YES/NO features and 0-score features
			//************************************************
			         .filter(e-> !(""+e.getValue()).equals("0"))
			         .filter(e-> !(""+e.getValue()).equals("NO"))
			         .filter(e-> !(""+e.getValue()).equals("YES"))
			//************************************************
			         .forEach(e-> ret.put(e.getKey(), e.getValue()));
                ret.put("Potency Category", Integer.toString( r.getCategoryScore()));
                ret.put("Potency Score", Integer.toString(r.getSumOfScores()));
	            if(r.getCategoryScore()==5){
 		            ret.put("Potency Score", "none");
	            }
                ret.put("type", r.getType());
                maps.add(ret);
            }
            return maps;
        } catch (IllegalArgumentException ex) {
            return new ArrayList<>();
        }
    }
}
