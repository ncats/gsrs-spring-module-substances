package gsrs.module.substance;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.*;

@Configuration
@ConfigurationProperties("ix.proxy")
@Data
public class ProxyConfiguration {

    private boolean enabled;

    private String name ="domain name";

    private int port=0;

    public HttpURLConnection openConnection(URL url) throws IOException {
        Proxy proxy;
        if(enabled){
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(name, port));
        }else{
            proxy = Proxy.NO_PROXY;
        }
        return (HttpURLConnection) url.openConnection(proxy);
    }
}
