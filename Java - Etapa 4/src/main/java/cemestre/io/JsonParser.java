package cemestre.io;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JsonParser {
    private static final int MAX_DEPTH = 200;

    private final String text;
    private int pos = 0;

    private JsonParser(String text) {
        this.text = text;
    }

    static Object parse(String text) throws DataLoadException {
        if (text == null || text.trim().isEmpty()) {
            throw new DataLoadException("el archivo esta vacio");
        }
        JsonParser parser = new JsonParser(text);
        parser.skipWhitespace();
        Object value = parser.readValue(0);
        parser.skipWhitespace();
        if (parser.pos < text.length()) {
            throw parser.error("hay contenido despues del final del JSON");
        }
        return value;
    }

    private Object readValue(int depth) throws DataLoadException {
        if (depth > MAX_DEPTH) {
            throw error("JSON demasiado anidado");
        }
        if (pos >= text.length()) {
            throw error("fin inesperado del archivo (JSON truncado?)");
        }
        char c = text.charAt(pos);
        switch (c) {
            case '{':
                return readObject(depth);
            case '[':
                return readArray(depth);
            case '"':
                return readString();
            case 't':
                expectWord("true");
                return Boolean.TRUE;
            case 'f':
                expectWord("false");
                return Boolean.FALSE;
            case 'n':
                expectWord("null");
                return null;
            default:
                if (c == '-' || (c >= '0' && c <= '9')) {
                    return readNumber();
                }
                throw error("caracter inesperado '" + c + "'");
        }
    }

    private Map<String, Object> readObject(int depth) throws DataLoadException {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++; // '{'
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return map;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("se esperaba el nombre de un campo entre comillas");
            }
            String key = readString();
            skipWhitespace();
            if (peek() != ':') {
                throw error("se esperaba ':' despues del campo '" + key + "'");
            }
            pos++;
            skipWhitespace();
            map.put(key, readValue(depth + 1));
            skipWhitespace();
            char c = peek();
            if (c == ',') {
                pos++;
            } else if (c == '}') {
                pos++;
                return map;
            } else {
                throw error("se esperaba ',' o '}'");
            }
        }
    }

    private List<Object> readArray(int depth) throws DataLoadException {
        List<Object> list = new ArrayList<>();
        pos++; // '['
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            skipWhitespace();
            list.add(readValue(depth + 1));
            skipWhitespace();
            char c = peek();
            if (c == ',') {
                pos++;
            } else if (c == ']') {
                pos++;
                return list;
            } else {
                throw error("se esperaba ',' o ']'");
            }
        }
    }

    private String readString() throws DataLoadException {
        pos++; // comilla de apertura
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= text.length()) {
                throw error("cadena de texto sin cerrar (JSON truncado?)");
            }
            char c = text.charAt(pos);
            if (c == '"') {
                pos++;
                return sb.toString();
            }
            if (c == '\\') {
                pos++;
                if (pos >= text.length()) {
                    throw error("secuencia de escape incompleta");
                }
                char e = text.charAt(pos);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (pos + 4 >= text.length()) {
                            throw error("secuencia \\u incompleta");
                        }
                        try {
                            sb.append((char) Integer.parseInt(text.substring(pos + 1, pos + 5), 16));
                        } catch (NumberFormatException ex) {
                            throw error("secuencia \\u invalida");
                        }
                        pos += 4;
                        break;
                    default:
                        throw error("secuencia de escape desconocida: \\" + e);
                }
                pos++;
            } else if (c < 0x20) {
                throw error("caracter de control dentro de un texto");
            } else {
                sb.append(c);
                pos++;
            }
        }
    }

    private Object readNumber() throws DataLoadException {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        readDigits();
        boolean integer = true;
        if (peek() == '.') {
            integer = false;
            pos++;
            readDigits();
        }
        if (peek() == 'e' || peek() == 'E') {
            integer = false;
            pos++;
            if (peek() == '+' || peek() == '-') {
                pos++;
            }
            readDigits();
        }
        String token = text.substring(start, pos);
        if (integer) {
            try {
                return Long.valueOf(token);
            } catch (NumberFormatException e) {
                return Double.valueOf(token); // entero gigantesco: se guarda como decimal
            }
        }
        return Double.valueOf(token);
    }

    private void readDigits() throws DataLoadException {
        int start = pos;
        while (pos < text.length() && text.charAt(pos) >= '0' && text.charAt(pos) <= '9') {
            pos++;
        }
        if (pos == start) {
            throw error("numero mal formado");
        }
    }

    private void expectWord(String word) throws DataLoadException {
        if (!text.startsWith(word, pos)) {
            throw error("se esperaba '" + word + "'");
        }
        pos += word.length();
    }

    private char peek() {
        return pos < text.length() ? text.charAt(pos) : '\0';
    }

    private void skipWhitespace() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }

    private DataLoadException error(String message) {
        int line = 1;
        int column = 1;
        for (int i = 0; i < pos && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new DataLoadException("JSON invalido: " + message + " (linea " + line + ", columna " + column + ")");
    }
}
