package cemestre.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonParserTest {

    @Test
    void parsesObjectsArraysAndLiterals() throws Exception {
        Object value = JsonParser.parse("{\"a\": [1, 2.5, -3, true, false, null], \"b\": {\"c\": \"x\"}}");
        Map<?, ?> map = (Map<?, ?>) value;
        List<?> list = (List<?>) map.get("a");
        assertEquals(1L, list.get(0));
        assertEquals(2.5, list.get(1));
        assertEquals(-3L, list.get(2));
        assertEquals(Boolean.TRUE, list.get(3));
        assertEquals(Boolean.FALSE, list.get(4));
        assertNull(list.get(5));
        assertEquals("x", ((Map<?, ?>) map.get("b")).get("c"));
    }

    @Test
    void keepsLeadingZerosInTextValues() throws Exception {
        Map<?, ?> map = (Map<?, ?>) JsonParser.parse("{\"startTime\": \"0730\"}");
        assertEquals("0730", map.get("startTime"));
    }

    @Test
    void parsesEscapesAndUnicode() throws Exception {
        Map<?, ?> map = (Map<?, ?>) JsonParser.parse("{\"t\": \"a\\\"b\\\\c\\n\\u00e9\"}");
        assertEquals("a\"b\\c\n\u00e9", map.get("t"));
    }

    @Test
    void parsesEmptyContainers() throws Exception {
        Map<?, ?> map = (Map<?, ?>) JsonParser.parse("{\"o\": {}, \"l\": []}");
        assertTrue(((Map<?, ?>) map.get("o")).isEmpty());
        assertTrue(((List<?>) map.get("l")).isEmpty());
    }

    @Test
    void rejectsEmptyText() {
        assertThrows(DataLoadException.class, () -> JsonParser.parse("   "));
    }

    @Test
    void rejectsTruncatedJson() {
        DataLoadException e = assertThrows(DataLoadException.class, () -> JsonParser.parse("{\"a\": ["));
        assertTrue(e.getMessage().contains("linea"));
    }

    @Test
    void rejectsTrailingComma() {
        assertThrows(DataLoadException.class, () -> JsonParser.parse("[1, 2,]"));
        assertThrows(DataLoadException.class, () -> JsonParser.parse("{\"a\": 1,}"));
    }

    @Test
    void rejectsContentAfterTheValue() {
        assertThrows(DataLoadException.class, () -> JsonParser.parse("{} {}"));
    }

    @Test
    void rejectsUnterminatedString() {
        assertThrows(DataLoadException.class, () -> JsonParser.parse("{\"a\": \"abc"));
    }

    @Test
    void rejectsMalformedNumber() {
        assertThrows(DataLoadException.class, () -> JsonParser.parse("[1.]"));
        assertThrows(DataLoadException.class, () -> JsonParser.parse("[-]"));
    }

    @Test
    void reportsLineAndColumn() {
        DataLoadException e = assertThrows(DataLoadException.class, () -> JsonParser.parse("{\n  \"a\": @\n}"));
        assertTrue(e.getMessage().contains("linea 2"), e.getMessage());
    }
}
