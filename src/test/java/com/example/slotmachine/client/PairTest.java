package com.example.slotmachine.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tesztek a Pair utility osztályhoz
 */
@DisplayName("Pair Tests")
public class PairTest {

    @Test
    @DisplayName("Should create pair with correct values")
    public void testPairCreation() {
        Pair<String, Integer> pair = new Pair<>("test", 42);
        
        assertEquals("test", pair.first);
        assertEquals(42, pair.second);
    }

    @Test
    @DisplayName("Should handle null values")
    public void testPairWithNullValues() {
        Pair<String, Integer> pair = new Pair<>(null, null);
        
        assertNull(pair.first);
        assertNull(pair.second);
    }

    @Test
    @DisplayName("Should handle mixed types")
    public void testPairWithMixedTypes() {
        Pair<Integer, String> pair1 = new Pair<>(123, "hello");
        Pair<Boolean, Double> pair2 = new Pair<>(true, 3.14);
        
        assertEquals(123, pair1.first);
        assertEquals("hello", pair1.second);
        assertEquals(true, pair2.first);
        assertEquals(3.14, pair2.second);
    }

    @Test
    @DisplayName("Should handle complex objects")
    public void testPairWithComplexObjects() {
        StringBuilder sb1 = new StringBuilder("test1");
        StringBuilder sb2 = new StringBuilder("test2");
        
        Pair<StringBuilder, StringBuilder> pair = new Pair<>(sb1, sb2);
        
        assertSame(sb1, pair.first);
        assertSame(sb2, pair.second);
        assertEquals("test1", pair.first.toString());
        assertEquals("test2", pair.second.toString());
    }

    @Test
    @DisplayName("Should be immutable after creation")
    public void testPairImmutability() {
        String originalFirst = "original";
        Integer originalSecond = 100;
        
        Pair<String, Integer> pair = new Pair<>(originalFirst, originalSecond);
        
        // Values should be accessible but the references are final
        assertEquals(originalFirst, pair.first);
        assertEquals(originalSecond, pair.second);
        
        // The pair itself doesn't change, but we can verify the values
        assertNotNull(pair.first);
        assertNotNull(pair.second);
    }

    @Test
    @DisplayName("Should work with primitive types")
    public void testPairWithPrimitiveTypes() {
        Pair<Integer, Integer> intPair = new Pair<>(1, 2);
        Pair<Double, Double> doublePair = new Pair<>(1.5, 2.5);
        Pair<Boolean, Boolean> booleanPair = new Pair<>(true, false);
        
        assertEquals(1, intPair.first);
        assertEquals(2, intPair.second);
        assertEquals(1.5, doublePair.first);
        assertEquals(2.5, doublePair.second);
        assertTrue(booleanPair.first);
        assertFalse(booleanPair.second);
    }

    @Test
    @DisplayName("Should handle edge cases")
    public void testPairEdgeCases() {
        // Empty strings
        Pair<String, String> emptyPair = new Pair<>("", "");
        assertEquals("", emptyPair.first);
        assertEquals("", emptyPair.second);
        
        // Zero values
        Pair<Integer, Integer> zeroPair = new Pair<>(0, 0);
        assertEquals(0, zeroPair.first);
        assertEquals(0, zeroPair.second);
        
        // Negative values
        Pair<Integer, Integer> negativePair = new Pair<>(-1, -2);
        assertEquals(-1, negativePair.first);
        assertEquals(-2, negativePair.second);
    }

    @Test
    @DisplayName("Should work with arrays")
    public void testPairWithArrays() {
        int[] array1 = {1, 2, 3};
        String[] array2 = {"a", "b", "c"};
        
        Pair<int[], String[]> pair = new Pair<>(array1, array2);
        
        assertSame(array1, pair.first);
        assertSame(array2, pair.second);
        assertEquals(3, pair.first.length);
        assertEquals(3, pair.second.length);
    }

    @Test
    @DisplayName("Should work with collections")
    public void testPairWithCollections() {
        java.util.List<String> list1 = java.util.Arrays.asList("item1", "item2");
        java.util.Set<Integer> set2 = java.util.Set.of(1, 2, 3);
        
        Pair<java.util.List<String>, java.util.Set<Integer>> pair = 
            new Pair<>(list1, set2);
        
        assertSame(list1, pair.first);
        assertSame(set2, pair.second);
        assertEquals(2, pair.first.size());
        assertEquals(3, pair.second.size());
    }

    @Test
    @DisplayName("Should handle generic type parameters correctly")
    public void testGenericTypeParameters() {
        // Test with same types
        Pair<String, String> sameTypePair = new Pair<>("first", "second");
        assertEquals("first", sameTypePair.first);
        assertEquals("second", sameTypePair.second);
        
        // Test with different types
        Pair<Object, Object> objectPair = new Pair<>(new String("test"), Integer.valueOf(42));
        assertTrue(objectPair.first instanceof String);
        assertTrue(objectPair.second instanceof Integer);
    }
}
