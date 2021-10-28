package ix.ncats.resolvers;

import gov.nih.ncats.molwitch.Chemical;
import gsrs.cache.GsrsCache;
import gsrs.module.substance.ProxyConfiguration;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.util.EntityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public abstract class AbstractStructureResolver implements Resolver<Structure> {
    static final int MAX_TRIES = 5;
    
    protected String name;
    protected int maxtries = MAX_TRIES;
    protected int readTimeout = 5000; // read timeout 5s
    protected int connTimeout = 2000; // connect timeout 2s
//    public static final boolean PROXY_ENABLED = ConfigHelper.getBoolean("ix.proxy.enabled",false);
//    private static String PROXY_NAME;
//    private static int PORT_NUMBER;

    @Autowired
    protected ProxyConfiguration proxyConfiguration;

    @Autowired
    protected StructureProcessor structureProcessor;

    @Autowired
    protected GsrsCache gsrsCache;


    
    
    //Static method to instantiate the parameter values for calling/connecting to the proxy server
//    static {
//        if (PROXY_ENABLED) {
//            PROXY_NAME = ConfigHelper.getOrDefault("ix.proxy.name","domain name");
//            PORT_NUMBER = ConfigHelper.getInt("ix.proxy.port",0);
//        }
//    }

    protected AbstractStructureResolver (String name) {
        if (name == null)
            throw new IllegalArgumentException ("Invalid resolver name: "+name);
        this.name = name;        
    }
    
    
    // TODO: This method really shouldn't be necessary, but due to the way
    // spring handles autowired dependency injection for abstact classes and base
    // classes, it appears that it is necessary. Spring will only autowire the FIRST
    // subclass extending the abstract class. All other ones will have the dependencies
    // remain null. The entire structure could, however, be rewritten to avoid these problems, 
    // but its not trivial.
    //
    // Note that this current paradigm of using init may cause issues for test.
    //
    // Screwy, init?
    private void init() {
        if(proxyConfiguration==null) {
            proxyConfiguration =StaticContextAccessor.getBean(ProxyConfiguration.class);
            structureProcessor =StaticContextAccessor.getBean(StructureProcessor.class);
            gsrsCache =StaticContextAccessor.getBean(GsrsCache.class);
        }
    }
    

    public String getName () { return name; }
    public Class<Structure> getType () { return Structure.class; }
    
    public void setMaxTries (int tries) { maxtries = tries; }
    public int getMaxTries () { return maxtries; }
    public void setReadTimeout (int timeout) { readTimeout = timeout; }
    public int getReadTimeout () { return readTimeout; }
    public void setConnectTimeout (int timeout) { connTimeout = timeout; }
    public int getConnectTimeout () { return connTimeout; }

    protected Structure resolve (InputStream is) throws IOException {
        return resolve(is, "sdf");
    }
    protected Structure resolve (InputStream is, String format) throws IOException {
        
        StringBuilder builder = new StringBuilder();
        //katzelda - April 2018
        //sometimes we have an invalid mol or sdfile
        //jchem and cdk will silently just make an empty object
        //so check to make sure we actually got something right
        //so we read the record and scan it to make sure it's complete
        //if it's not don't even bother.

        boolean isValid= Objects.equals("smiles", format);

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(is))){
            String line;
            while( (line = reader.readLine()) !=null){
                builder.append(line).append("\n");
                if(!isValid && line.startsWith("M END") || line.contains("$$$$")){
                    isValid = true;
                }
            }
        }

        if(!isValid){
            return null;
        }
            Structure struc = structureProcessor.instrument(builder.toString(), null, true);
            //katzelda - April 2018
            //sometimes we have an invalid mol file
            //jchem and cdk will silently just make an empty object
            //so check to make sure we actually got something right
            Chemical chemical = struc.toChemical();

            if(chemical.getAtomCount() ==0 || chemical.getBondCount() ==0){
//                System.out.println("atom or bond count was 0");
                return null;
            }
            //katzelda2021: we aren't going to save this in GSRS 3 but need to put it in temp cache
            UUID uuid = UUID.randomUUID();
            struc.id = uuid;
            gsrsCache.setRaw(uuid.toString(), EntityUtils.EntityWrapper.of(struc).toFullJson());
//            struc.save();
            return struc;

    }

    protected abstract UrlAndFormat[] resolvers (String name)
        throws MalformedURLException;

    public Structure resolve (String name) {
        init();
        try {
            UrlAndFormat[] urls = resolvers (name);
            for (UrlAndFormat url : urls) {

                for (int tries = 0; tries < maxtries; ++tries) {
                    try {

                        // If the Proxy flag is enabled in the Config file, it connects to proxy or it wouldn't
//                        
//                        ProxyConfiguration proxyConfiguration2 =StaticContextAccessor.getBean(ProxyConfiguration.class);
//                        
//                        System.out.println("Url is:" +url.url);
//                        System.out.println("Proxy found was:" + proxyConfiguration2);
//                        System.out.println("Autowired was:" + proxyConfiguration);
//                        
//                        

                        HttpURLConnection con = proxyConfiguration.openConnection(url.url);
                        con.connect();

                        con.setReadTimeout(readTimeout);
                        con.setConnectTimeout(connTimeout);

                        int status = con.getResponseCode();
                        log.trace("Resolving " + url + "..." + status);
                        if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                            break;
                        }
                        Structure s = resolve(con.getInputStream(), url.format);
                        if (s != null) {
                            return s;
                        }
                        //if we get this far something worked
                        break;

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        log.warn("Fail to resolve \"" + name + "\"; "
                                + tries + "/" + maxtries + " attempts");
                        Thread.sleep(500); //
                    }
                }
            }
        }
        catch (Exception ex) {
            log.error("Fail to resolve \""+name+"\"", ex);
        }
        return null;
    }

    protected static class UrlAndFormat{
        public final URL url;
        public final String format;

        public UrlAndFormat(URL url, String format) {
            this.url = url;
            this.format = format;
        }
    }
}
