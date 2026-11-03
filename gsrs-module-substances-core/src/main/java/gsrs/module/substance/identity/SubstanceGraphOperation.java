package gsrs.module.substance.identity;

/**
 * Persistence operations whose identity semantics differ for a substance graph.
 */
public enum SubstanceGraphOperation {
    CREATE,
    UPDATE,
    CLONE,
    BULK_IMPORT
}
