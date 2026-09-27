package autumn.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/** Sérialiseur JSON minimal pour les valeurs renvoyées par les contrôleurs. */
public final class JsonSerializer {
    private JsonSerializer() { }

    public static String toJson(Object value) {
        StringBuilder json = new StringBuilder();
        appendJson(value, json, new IdentityHashMap<Object, Boolean>());
        return json.toString();
    }

    private static void appendJson(Object value, StringBuilder json, IdentityHashMap<Object, Boolean> seen) {
        if (value == null) { json.append("null"); return; }
        if (value instanceof String || value instanceof Character || value instanceof Enum<?>) {
            quote(value instanceof Enum<?> ? ((Enum<?>) value).name() : value.toString(), json);
            return;
        }
        if (value instanceof Number || value instanceof Boolean) { json.append(value); return; }
        if (seen.put(value, Boolean.TRUE) != null) { throw new IllegalArgumentException("Cycle détecté pendant la sérialisation JSON"); }
        try {
            if (value instanceof Map<?, ?>) {
                json.append('{');
                Iterator<? extends Map.Entry<?, ?>> entries = ((Map<?, ?>) value).entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry<?, ?> entry = entries.next();
                    quote(String.valueOf(entry.getKey()), json); json.append(':');
                    appendJson(entry.getValue(), json, seen);
                    if (entries.hasNext()) json.append(',');
                }
                json.append('}');
            } else if (value instanceof Iterable<?>) {
                json.append('['); Iterator<?> items = ((Iterable<?>) value).iterator();
                while (items.hasNext()) { appendJson(items.next(), json, seen); if (items.hasNext()) json.append(','); }
                json.append(']');
            } else if (value.getClass().isArray()) {
                json.append('[');
                for (int i = 0; i < Array.getLength(value); i++) { if (i > 0) json.append(','); appendJson(Array.get(value, i), json, seen); }
                json.append(']');
            } else {
                json.append('{'); boolean first = true;
                for (Class<?> type = value.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
                    for (Field field : type.getDeclaredFields()) {
                        if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
                        try {
                            field.setAccessible(true);
                            Object fieldValue = field.get(value);
                            if (!first) json.append(',');
                            quote(field.getName(), json); json.append(':'); appendJson(fieldValue, json, seen);
                            first = false;
                        } catch (ReflectiveOperationException | RuntimeException e) {
                            throw new IllegalArgumentException("Impossible de lire le champ " + field.getName(), e);
                        }
                    }
                }
                json.append('}');
            }
        } finally { seen.remove(value); }
    }

    private static void quote(String value, StringBuilder json) {
        json.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': json.append("\\\""); break;
                case '\\': json.append("\\\\"); break;
                case '\b': json.append("\\b"); break;
                case '\f': json.append("\\f"); break;
                case '\n': json.append("\\n"); break;
                case '\r': json.append("\\r"); break;
                case '\t': json.append("\\t"); break;
                default: if (c < 0x20) json.append(String.format("\\u%04x", (int)c)); else json.append(c);
            }
        }
        json.append('"');
    }
}
