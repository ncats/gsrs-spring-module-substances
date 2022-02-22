package ix.core.processing;

public interface RecordTransformFactory<K,T> {

    RecordTransformer<K,T> createTransformerFor();
}
