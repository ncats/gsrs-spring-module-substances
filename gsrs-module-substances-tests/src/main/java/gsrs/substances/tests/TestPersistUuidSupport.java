package gsrs.substances.tests;

import ix.ginas.models.GinasCommonData;

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
            if (visited.contains(current) || isLeaf(current)) {
                continue;
            }
            visited.add(current);

            if (current instanceof GinasCommonData data) {
                data.getOrGenerateUUID();
            }

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


    private static String packageName(Class<?> type) {
        Package pkg = type.getPackage();
        return pkg == null ? "" : pkg.getName();
    }
}

