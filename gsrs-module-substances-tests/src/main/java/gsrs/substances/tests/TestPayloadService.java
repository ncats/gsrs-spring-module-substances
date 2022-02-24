package gsrs.substances.tests;

import gov.nih.ncats.common.io.IOUtil;
import gsrs.service.PayloadService;
import ix.core.models.Payload;

import java.io.*;
import java.util.*;

public class TestPayloadService implements PayloadService {
    private Map<String, File> nameMap = new HashMap<>();
    private Map<UUID, File> uuidMap = new HashMap<>();
    private File tmpDir;

    public TestPayloadService(File tmpDir) {
        this.tmpDir = tmpDir;
    }

    public synchronized void deleteAll(){
        for(File f : nameMap.values()){
            f.delete();
        }
        nameMap.clear();
        uuidMap.clear();
    }

    @Override
    public synchronized Payload createPayload(String name, String mime, InputStream content, PayloadPersistType persistType) throws IOException {

        try{
            Objects.requireNonNull(name);
            if(nameMap.containsKey(name)){
                throw new IOException("already have a payload with given name");
            }
            File f = File.createTempFile("payload-"+name, null, tmpDir);
            try(OutputStream out = new BufferedOutputStream(new FileOutputStream(f))) {
                IOUtil.copy(content, out);
            }
            nameMap.put(name, f);
            Payload p = new Payload();
            p.name = name;
            p.id = UUID.randomUUID();

            uuidMap.put(p.id, f);
            return p;
        }finally{
            IOUtil.closeQuietly(content);
        }
    }

    @Override
    public synchronized Optional<InputStream> getPayloadAsInputStream(Payload payload) throws IOException {
        File f = nameMap.get(payload.name);
        if(f ==null) {
            return Optional.empty();
        }
        return Optional.of(new FileInputStream(f));
    }
    @Override
    public synchronized Optional<InputStream> getPayloadAsInputStream(UUID payloadID) throws IOException {
        File f = uuidMap.get(payloadID);
        if(f ==null) {
            return Optional.empty();
        }
        return Optional.of(new FileInputStream(f));
    }
    @Override
    public Optional<File> getPayloadAsFile(Payload payload) throws IOException {
        return Optional.ofNullable(nameMap.get(payload.name));

    }
}
