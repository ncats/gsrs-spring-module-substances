package gsrs.module.substance.importers.actions;

public interface ImportMappingAction<T, U> {
    public T act(T building, U source) throws Exception;
}