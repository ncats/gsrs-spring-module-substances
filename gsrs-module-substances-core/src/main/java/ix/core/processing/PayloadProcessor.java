package ix.core.processing;

import ix.core.models.Payload;
import ix.utils.Util;

import java.io.Serializable;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class PayloadProcessor implements Serializable {
    public final UUID payloadId;
    public final String id;
    public final String key;
    public Long jobId;

    private static final Random rand = new Random();

    private static String randomKey(int size) {
        byte[] b = new byte[size];
        rand.nextBytes(b);
        return Util.toHex(b);
    }
    public PayloadProcessor(Payload payload) {
        Objects.requireNonNull(payload);
        this.payloadId = payload.id;
        this.key = randomKey(10);
        this.id = payload.id + ":" + this.key;

    }
}
