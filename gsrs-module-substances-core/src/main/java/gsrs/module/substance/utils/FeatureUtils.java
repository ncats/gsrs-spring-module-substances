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
                r.getFeatureSet().entrySet().forEach(e-> ret.put(e.getKey(), e.getValue()));
                ret.put("potencyCategory", Integer.toString( r.getCategoryScore()));
                ret.put("potencyScore", Integer.toString(r.getSumOfScores()));
	            if(r.getCategoryScore()==5){
 		            ret.put("potencyScore", "none");
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
