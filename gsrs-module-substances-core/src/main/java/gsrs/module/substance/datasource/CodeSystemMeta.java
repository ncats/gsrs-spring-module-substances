package gsrs.module.substance.datasource;

import ix.ginas.models.v1.Code;
import org.apache.commons.lang3.StringUtils;

public class CodeSystemMeta {
	String codeSystem;
	String url;

	public CodeSystemMeta(String cs, String url) {
		this.codeSystem = cs;
		this.url = url;
	}
	
	public void addURL(Code cd){
		String urlValue = generateUrlFor(cd);
		if(urlValue !=null){
			cd.url = urlValue;
		}
	}

	public String generateUrlFor(Code code){
		if(url==null || url.trim().isEmpty()){
			return null;
		}
		String result = StringUtils.replace(url,"$CODE$", code.code);
        if( code.codeText!=null && code.codeText.length()>0){
            result = StringUtils.replace(url, "$CODETEXT$", code.codeText);
        }
        return result.trim();
	}

}