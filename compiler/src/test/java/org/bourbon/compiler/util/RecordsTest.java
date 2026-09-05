package org.bourbon.compiler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class RecordsTest {

    record Empty() {}
    record Single(String value) {}
    record Pair(int a, String b) {}
    record Nested(String name, Pair pair) {}
    record WithNull(String first, Object second) {}

    @Test
    void formatEmptyRecord() {
        assertEquals("Empty()", Records.format(new Empty()));
    }

    @Test
    void formatSingleComponentRecord() {
        assertEquals("Single(hello)", Records.format(new Single("hello")));
    }

    @Test
    void formatMultipleComponentRecord() {
        assertEquals("Pair(42, bourbon)", Records.format(new Pair(42, "bourbon")));
    }

    @Test
    void formatNestedRecordsRecursively() {
        assertEquals("Nested(outer, Pair(10, inner))", Records.format(new Nested("outer", new Pair(10, "inner"))));
    }

    @Test
    void formatRecordWithNulls() {
        assertEquals("WithNull(test, null)", Records.format(new WithNull("test", null)));
    }

    @Test
    void formatNonRecordObjects() {
        assertEquals("hello", Records.format("hello"));
        assertEquals("123", Records.format(123));
        assertEquals("null", Records.format(null));
        assertEquals("[a, b]", Records.format(List.of("a", "b")));
    }
}
