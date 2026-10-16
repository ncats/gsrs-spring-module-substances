package gsrs.module.substance;

import ix.core.models.ParentReference;
import ix.ginas.models.v1.Moiety;
import jakarta.persistence.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.SingularAttribute;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Prepares new, owned import records without replacing their incoming identities.
 * Only persist cascades are followed: referenced users, groups and parents are not imports.
 */
final class BulkImportPersistableGraph {
    private BulkImportPersistableGraph() {
    }

    static void prepare(Object root, EntityManager entityManager) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<String, Object> identities = new HashMap<>();
        prepare(root, entityManager, visited, identities);
    }

    private static void prepare(Object entity, EntityManager entityManager,
                                Set<Object> visited, Map<String, Object> identities) {
        if (entity == null || !visited.add(entity)) {
            return;
        }
        EntityType<?> type = entityManager.getMetamodel().entity(entity.getClass());
        try {
            // Moiety's storage key is derived from its structure; its public UUID is separate.
            if (entity instanceof Moiety moiety) {
                moiety.enforce();
                String key = Moiety.class.getName() + ":uuid:" + moiety.uuid;
                if (identities.putIfAbsent(key, moiety) != null
                        || entityManager.createQuery("select count(m) from Moiety m where m.uuid = :uuid", Long.class)
                        .setParameter("uuid", moiety.uuid).getSingleResult() != 0) {
                    throw new EntityExistsException("Bulk import UUID already exists: Moiety " + moiety.uuid);
                }
            }
            for (SingularAttribute<?, ?> attribute : type.getSingularAttributes()) {
                Field field = field(attribute);
                if (attribute.isId()) {
                    Object id = field.get(entity);
                    if (field.isAnnotationPresent(GeneratedValue.class)
                            && Number.class.isAssignableFrom(field.getType())) {
                        // Numeric storage keys are local sequences, not portable UUID identities.
                        field.set(entity, null);
                        continue;
                    }
                    if (id == null && field.getType() == UUID.class) {
                        id = UUID.randomUUID();
                        field.set(entity, id);
                    }
                    if (id != null) {
                        IdentifiableType<?> identityType = type;
                        while (identityType.getSupertype() instanceof EntityType<?> parent) {
                            identityType = parent;
                        }
                        String key = identityType.getJavaType().getName() + ":" + id;
                        if (identities.putIfAbsent(key, entity) != null
                                || entityManager.find(type.getJavaType(), id) != null) {
                            throw new EntityExistsException("Bulk import UUID already exists: "
                                    + type.getName() + " " + id);
                        }
                    }
                } else if (attribute.isVersion()) {
                    // Imported versions describe the source database, not this new row.
                    field.set(entity, field.getType().isPrimitive() ? 0 : null);
                }
            }
            for (Attribute<?, ?> attribute : type.getAttributes()) {
                if (!attribute.isAssociation()) {
                    continue;
                }
                Field field = field(attribute);
                if (field.isAnnotationPresent(ParentReference.class) || !cascadesPersist(field)) {
                    continue;
                }
                Object value = field.get(entity);
                if (value instanceof Collection<?> children) {
                    for (Object child : children) {
                        prepare(child, entityManager, visited, identities);
                    }
                } else {
                    prepare(value, entityManager, visited, identities);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot prepare imported " + type.getName(), e);
        }
    }

    private static Field field(Attribute<?, ?> attribute) {
        if (!(attribute.getJavaMember() instanceof Field field)) {
            throw new IllegalStateException("Expected field access for " + attribute.getName());
        }
        field.setAccessible(true);
        return field;
    }

    private static boolean cascadesPersist(Field field) {
        CascadeType[] cascade;
        if (field.isAnnotationPresent(OneToMany.class)) {
            cascade = field.getAnnotation(OneToMany.class).cascade();
        } else if (field.isAnnotationPresent(OneToOne.class)) {
            cascade = field.getAnnotation(OneToOne.class).cascade();
        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            cascade = field.getAnnotation(ManyToOne.class).cascade();
        } else if (field.isAnnotationPresent(ManyToMany.class)) {
            cascade = field.getAnnotation(ManyToMany.class).cascade();
        } else {
            return false;
        }
        return Arrays.stream(cascade).anyMatch(c -> c == CascadeType.PERSIST || c == CascadeType.ALL);
    }
}
