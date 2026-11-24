package cemestre.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class JsonObject {
    private final Map<String, Object> values;
    private final String path;

    private JsonObject(Map<String, Object> values, String path) {
        this.values = values;
        this.path = path;
    }

    @SuppressWarnings("unchecked")
    static JsonObject root(Object parsed) throws DataLoadException {
        if (!(parsed instanceof Map)) {
            throw new DataLoadException("la raiz del JSON debe ser un objeto { ... }");
        }
        return new JsonObject((Map<String, Object>) parsed, "");
    }

    public String getPath() {
        return path;
    }
    public boolean has(String key) {
        return values.containsKey(key);
    }
    public boolean isNull(String key) {
        return values.containsKey(key) && values.get(key) == null;
    }

    public String string(String key) throws DataLoadException {
        Object value = require(key);
        if (!(value instanceof String)) {
            throw typeError(key, "un texto", value);
        }
        return (String) value;
    }

    public int integer(String key) throws DataLoadException {
        Object value = require(key);
        if (value instanceof Long) {
            long number = (Long) value;
            if (number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE) {
                return (int) number;
            }
        }
        throw typeError(key, "un numero entero", value);
    }

    public double number(String key) throws DataLoadException {
        Object value = require(key);
        if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        throw typeError(key, "un numero", value);
    }

    public boolean bool(String key) throws DataLoadException {
        Object value = require(key);
        if (!(value instanceof Boolean)) {
            throw typeError(key, "true o false", value);
        }
        return (Boolean) value;
    }

    @SuppressWarnings("unchecked")
    public JsonObject object(String key) throws DataLoadException {
        Object value = require(key);
        if (!(value instanceof Map)) {
            throw typeError(key, "un objeto", value);
        }
        return new JsonObject((Map<String, Object>) value, pathOf(key));
    }

    /** Arreglo cuyos elementos son todos objetos. */
    @SuppressWarnings("unchecked")
    public List<JsonObject> objects(String key) throws DataLoadException {
        List<Object> items = list(key);
        List<JsonObject> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            String itemPath = pathOf(key) + "[" + i + "]";
            if (!(item instanceof Map)) {
                throw new DataLoadException("en " + itemPath + " se esperaba un objeto pero hay " + describe(item));
            }
            result.add(new JsonObject((Map<String, Object>) item, itemPath));
        }
        return result;
    }

    /** Arreglo cuyos elementos son todos textos. */
    public List<String> strings(String key) throws DataLoadException {
        List<Object> items = list(key);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            if (!(item instanceof String)) {
                throw new DataLoadException("en " + pathOf(key) + "[" + i + "] se esperaba un texto pero hay " + describe(item));
            }
            result.add((String) item);
        }
        return result;
    }

    public String pathOf(String key) {
        return path.isEmpty() ? key : path + "." + key;
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(String key) throws DataLoadException {
        Object value = require(key);
        if (!(value instanceof List)) {
            throw typeError(key, "una lista", value);
        }
        return (List<Object>) value;
    }

    private Object require(String key) throws DataLoadException {
        if (!values.containsKey(key)) {
            String where = path.isEmpty() ? "la raiz del archivo" : path;
            throw new DataLoadException("falta el campo '" + key + "' en " + where);
        }
        return values.get(key);
    }

    private DataLoadException typeError(String key, String expected, Object actual) {
        return new DataLoadException("el campo " + pathOf(key) + " debe ser " + expected + " pero es " + describe(actual));
    }

    private static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "el texto '" + value + "'";
        }
        if (value instanceof Map) {
            return "un objeto";
        }
        if (value instanceof List) {
            return "una lista";
        }
        return "el valor " + value;
    }
}
