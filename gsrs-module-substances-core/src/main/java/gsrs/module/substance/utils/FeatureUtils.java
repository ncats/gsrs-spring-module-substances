package gsrs.module.substance.utils;

import gov.nih.ncats.molwitch.Chemical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import gov.fda.gsrs.ndsri.FeaturizeNitrosamine;
import gov.fda.gsrs.ndsri.FeaturizeNitrosamine.FeatureJob;
import gov.fda.gsrs.ndsri.FeaturizeNitrosamine.FeatureResponse;

public class FeatureUtils {
    public static List<Map<String, String>> calculateFeatures(Chemical chemical) throws Exception{
        FeatureJob fj = new FeatureJob(chemical);

        List<FeatureResponse> resp = FeaturizeNitrosamine.fingerprintNitrosamine(fj);

        List<Map<String,String>> maps = new ArrayList<>();
        resp.forEach(r->{
            Map<String, String> ret = new HashMap<>();
            r.getFeatureSet().entrySet().forEach(e-> ret.put(e.getKey(), e.getValue()));
            maps.add(ret);
        });
        return maps;
    }
}
