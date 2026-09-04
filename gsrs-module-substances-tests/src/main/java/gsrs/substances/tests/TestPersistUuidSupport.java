package gsrs.substances.tests;

import ix.ginas.models.GinasCommonData;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Test-only safeguard that assigns UUIDs before persistence walks a GSRS object graph.
 *
 * This keeps Hibernate 6 from failing on assigned-id entities in test paths without
 * changing production code.
 */
final class TestPersistUuidSupport {

    private TestPersistUuidSupport() {
    }

    static void ensurePersistableIds(Object root) {
        if (root == null) {
            return;
        }
        Set<Object> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Object> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Object current = stack.pop();
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);

            if (current instanceof Collection<?> collection) {
                for (Object value : collection) {
                    if (value != null) {
                        stack.push(value);
                    }
                }
                continue;
            }
            if (current instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null) {
                        stack.push(entry.getKey());
                    }
                    if (entry.getValue() != null) {
                        stack.push(entry.getValue());
                    }
                }
                continue;
            }
            Class<?> type = current.getClass();
            if (type.isArray()) {
                for (int i = 0; i < Array.getLength(current); i++) {
                    Object value = Array.get(current, i);
                    if (value != null) {
                        stack.push(value);
                    }
                }
                continue;
            }

            if (isLeaf(current)) {
                continue;
            }

            if (current instanceof GinasCommonData data) {
                // getOrGenerateUUID() returns the UUID but may not set it on the object.
                // We need to ensure the returned UUID is actually assigned to the object's field.
                UUID generatedUuid = data.getOrGenerateUUID();
                if (generatedUuid != null) {
                    // Try to assign the returned UUID to the uuid field if it's still null
                    assignUuidIfNull(current, generatedUuid);
                }
            }
            
            if (requiresManualIdentifierAssignment(current.getClass())) {
                ensureEntityHasUuid(current);
            }
            
            normalizeGeneratedStructureIdentity(current);
            if (requiresManualIdentifierAssignment(current.getClass())) {
                ensureAssignedIdentifier(current);
            }

            if (shouldInspectFields(type)) {
                for (Class<?> cursor = type; cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
                    for (Field field : cursor.getDeclaredFields()) {
                        if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                            continue;
                        }
                        try {
                            field.setAccessible(true);
                            Object value = field.get(current);
                            if (value != null) {
                                stack.push(value);
                            }
                        } catch (IllegalAccessException ignored) {
                            // Best effort only; the main goal is to initialize entity UUIDs.
                        }
                    }
                }
            }
        }
    }

    static void refreshReferenceIds(Object root) {
        if (!(root instanceof Substance substance) || substance.references == null) {
            return;
        }
        for (Reference reference : substance.references) {
            if (reference != null) {
                forceUuid(reference, UUID.randomUUID());
            }
        }
    }

    private static boolean isLeaf(Object value) {
        Class<?> type = value.getClass();
        if (type.isPrimitive() || type.isEnum()) {
            return true;
        }
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof UUID
                || value instanceof Date
                || value instanceof TemporalAccessor
                || type == Class.class
                || packageName(type).startsWith("java.")
                || packageName(type).startsWith("javax.")
                || packageName(type).startsWith("jakarta.")
                || packageName(type).startsWith("org.hibernate.");
    }

    private static boolean shouldInspectFields(Class<?> type) {
        String packageName = packageName(type);
        return packageName.startsWith("ix.")
                || packageName.startsWith("gsrs.")
                || packageName.startsWith("gov.nih.ncats.");
    }

    private static void ensureAssignedIdentifier(Object value) {
        for (Class<?> cursor = value.getClass(); cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
            for (Field field : cursor.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                String name = field.getName();
                boolean isIdentifierField = "uuid".equals(name)
                        || "id".equals(name)
                        || field.getAnnotation(Id.class) != null;
                if (!isIdentifierField) {
                    continue;
                }
                if (field.getAnnotation(GeneratedValue.class) != null && isStructureType(value.getClass())) {
                    // Structure ids must remain transient so Hibernate treats them as new.
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object currentValue = field.get(value);
                    if (currentValue != null) {
                        // Field already has a value; assume it's properly assigned
                        continue;
                    }

                    // Hibernate 6 requires assigned-id entities to have IDs before persist(),
                    // even if they have custom @GeneratedValue generators like NullUUIDGenerator.
                    // Assign a UUID or appropriate value for null id/uuid fields based on their type.
                    if (field.getType() == UUID.class) {
                        field.set(value, UUID.randomUUID());
                    } else if (field.getType() == String.class) {
                        field.set(value, UUID.randomUUID().toString());
                    } else if (field.getType() == Long.class || field.getType() == long.class) {
                        field.set(value, UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
                    } else if (field.getType() == Integer.class || field.getType() == int.class) {
                        field.set(value, UUID.randomUUID().hashCode());
                    }
                } catch (IllegalAccessException ignored) {
                    // Best effort for test fixtures only.
                }
            }
        }
    }

    private static boolean requiresManualIdentifierAssignment(Class<?> type) {
        for (Class<?> cursor = type; cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
            String className = cursor.getName();
            if ("ix.ginas.models.v1.Substance".equals(className)
                    || "ix.ginas.models.v1.NameOrg".equals(className)
                    || "ix.ginas.models.v1.Name".equals(className)
                    || "ix.ginas.models.v1.Code".equals(className)
                    || "ix.ginas.models.v1.SiteContainer".equals(className)) {
                return true;
            }
        }
        return false;
    }

    private static void normalizeGeneratedStructureIdentity(Object value) {
        if (!isStructureType(value.getClass())) {
            return;
        }

        Field idField = findIdField(value.getClass());
        if (idField == null || idField.getAnnotation(GeneratedValue.class) == null) {
            return;
        }

        try {
            idField.setAccessible(true);
            Object currentId = idField.get(value);
            if (currentId == null) {
                return;
            }

            // Only clear the ID if the entity appears to be NEW (version is 0 or null).
            // If version > 0, the entity was previously persisted and is now detached.
            // Clearing its ID would confuse Hibernate and cause EntityExistsException.
            Integer version = getVersionFieldValue(value);
            if (version != null && version > 0) {
                // This is a previously persisted entity, leave it alone
                return;
            }

            // Hibernate 6 treats generated-id structures with pre-populated ids as detached.
            // Force them back to a transient state for create/persist test paths, but only
            // if the version indicates this is actually a new entity.
            idField.set(value, null);
            clearVersionField(value);
        } catch (IllegalAccessException ignored) {
            // Best effort only.
        }
    }

    private static Integer getVersionFieldValue(Object value) {
        try {
            for (Class<?> cursor = value.getClass(); cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
                for (Field field : cursor.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                        continue;
                    }
                    if ((field.getAnnotation(Version.class) == null && !"version".equals(field.getName()))
                        || !Number.class.isAssignableFrom(field.getType()) && field.getType() != int.class && field.getType() != long.class) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object versionValue = field.get(value);
                    if (versionValue instanceof Number) {
                        return ((Number) versionValue).intValue();
                    } else if (versionValue != null) {
                        try {
                            return Integer.parseInt(versionValue.toString());
                        } catch (NumberFormatException ignored) {
                            // Not a number
                        }
                    }
                    return null;
                }
            }
        } catch (IllegalAccessException ignored) {
            // Best effort only
        }
        return null;
    }


    private static Field findIdField(Class<?> type) {
        for (Class<?> cursor = type; cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
            for (Field field : cursor.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                if (field.getAnnotation(Id.class) != null) {
                    return field;
                }
            }
        }
        return null;
    }

    private static void clearVersionField(Object value) {
        for (Class<?> cursor = value.getClass(); cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
            for (Field field : cursor.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                if (field.getAnnotation(Version.class) == null && !"version".equals(field.getName())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    if (!field.getType().isPrimitive()) {
                        field.set(value, null);
                    }
                } catch (IllegalAccessException ignored) {
                    // Best effort only.
                }
                return;
            }
        }
    }

    private static boolean isStructureType(Class<?> type) {
        for (Class<?> cursor = type; cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
            if ("ix.core.models.Structure".equals(cursor.getName())
                    || "ix.ginas.models.v1.GinasChemicalStructure".equals(cursor.getName())) {
                return true;
            }
        }
        return false;
    }


    private static void assignUuidIfNull(Object value, UUID generatedUuid) {
        if (value == null || generatedUuid == null) {
            return;
        }
        try {
            Field uuidField = null;
            for (Class<?> cursor = value.getClass(); cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
                try {
                    uuidField = cursor.getDeclaredField("uuid");
                    break;
                } catch (NoSuchFieldException ignored) {
                    // Continue searching in superclass
                }
            }
            if (uuidField != null && uuidField.getType() == UUID.class) {
                uuidField.setAccessible(true);
                if (uuidField.get(value) == null) {
                    uuidField.set(value, generatedUuid);
                }
            }
        } catch (IllegalAccessException ignored) {
            // Best effort only
        }
    }

    private static void forceUuid(Object value, UUID generatedUuid) {
        if (value == null || generatedUuid == null) {
            return;
        }
        try {
            Field uuidField = null;
            for (Class<?> cursor = value.getClass(); cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
                try {
                    uuidField = cursor.getDeclaredField("uuid");
                    break;
                } catch (NoSuchFieldException ignored) {
                    // Continue searching in superclass
                }
            }
            if (uuidField != null && uuidField.getType() == UUID.class) {
                uuidField.setAccessible(true);
                uuidField.set(value, generatedUuid);
            }
        } catch (IllegalAccessException ignored) {
            // Best effort only
        }
    }

    /**
     * Ensure that entities like Substance, NameOrg, and SiteContainer have a UUID assigned.
     * These entities require manually-assigned UUIDs for persistence.
     */
    private static void ensureEntityHasUuid(Object value) {
        if (value == null) {
            return;
        }
        try {
            // Try getOrGenerateUUID() method if it exists
            java.lang.reflect.Method getOrGenerateMethod = null;
            try {
                getOrGenerateMethod = value.getClass().getMethod("getOrGenerateUUID");
                UUID uuid = (UUID) getOrGenerateMethod.invoke(value);
                assignUuidIfNull(value, uuid);
                return;
            } catch (NoSuchMethodException ignored) {
                // Method doesn't exist, fall through
            }
            
            // Fall back to direct ID field assignment
            for (Class<?> cursor = value.getClass(); cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
                try {
                    Field uuidField = cursor.getDeclaredField("uuid");
                    uuidField.setAccessible(true);
                    Object currentValue = uuidField.get(value);
                    if (currentValue == null) {
                        uuidField.set(value, UUID.randomUUID());
                    }
                    return;
                } catch (NoSuchFieldException ignored) {
                    // Continue to next superclass
                }
            }
            
            // Try "id" field if "uuid" doesn't exist
            for (Class<?> cursor = value.getClass(); cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
                try {
                    Field idField = cursor.getDeclaredField("id");
                    idField.setAccessible(true);
                    Object currentValue = idField.get(value);
                    if (currentValue == null) {
                        if (idField.getType() == UUID.class) {
                            idField.set(value, UUID.randomUUID());
                        } else if (idField.getType() == String.class) {
                            idField.set(value, UUID.randomUUID().toString());
                        } else if (idField.getType() == Long.class || idField.getType() == long.class) {
                            idField.set(value, UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
                        } else if (idField.getType() == Integer.class || idField.getType() == int.class) {
                            idField.set(value, UUID.randomUUID().hashCode());
                        }
                    }
                    return;
                } catch (NoSuchFieldException ignored) {
                    // Continue to next superclass
                }
            }
        } catch (Exception ignored) {
            // Best effort only - if we can't set the UUID, let Hibernate handle the error
        }
    }

    private static String packageName(Class<?> type) {
        Package pkg = type.getPackage();
        return pkg == null ? "" : pkg.getName();
    }
}

