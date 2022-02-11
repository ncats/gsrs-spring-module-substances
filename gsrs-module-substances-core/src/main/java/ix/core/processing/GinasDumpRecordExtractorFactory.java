package ix.core.processing;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import ix.core.stats.Estimate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GinasDumpRecordExtractorFactory implements RecordExtractorFactory<JsonNode> {

    @Override
    public String getExtractorName() {
        return SubstanceBulkLoadService.GinasDumpExtractor.class.getName();
    }

    @Override
    public RecordExtractor<JsonNode> createNewExtractorFor(InputStream in) {
        return new SubstanceBulkLoadService.GinasDumpExtractor(in);
    }

    @Override
    public Estimate estimateRecordCount(InputStream in) {
        //match logic of GSRS 2 estimate... if it errors out return the coount so far
        int count=0;
        try(BufferedReader buf = new BufferedReader(new InputStreamReader(in))){
            //this should be faster than GSRS 2 to get estimated count because we are only counting
            //lines not parsing the JSON for each one!
            String line;
            while( (line= buf.readLine()) !=null){
                line = line.trim();
                if(line.isEmpty() || line.startsWith("#")){
                    continue;
                }
                count++;
            }



        }catch(IOException e){

        }
        return new Estimate(count, Estimate.TYPE.EXACT);
    }
}
