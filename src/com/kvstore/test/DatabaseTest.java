package com.kvstore.test;

import static org.junit.jupiter.api.Assertions.*;

import com.kvstore.main.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class DatabaseTest {
    private Database store;

    @BeforeEach
    void setUp() throws IOException {
        store = new Database();
    }

    @AfterEach
    void tearDown() throws IOException {
        store.close();
        new File("data.data").delete();
    }

    @Test
    void testPutAndGet() throws IOException {
        store.put("key1", "value1");
        assertEquals("value1", store.get("key1"), "Retrieved value should match the stored value.");
    }

    @Test
    void testUpdateValue() throws IOException {
        store.put("key1", "value1");
        store.put("key1", "value2");
        assertEquals("value2", store.get("key1"), "Retrieved value should be updated value.");
    }

    @Test
    void testDeleteValue() throws IOException {
        store.put("key1", "value1");
        store.put("key2", "value2");
        store.delete("key1");
        assertNull(store.get("key1"), "Retrieved value should not exist.");
    }

    @Test
    void testPersistence() throws IOException {
        store.put("key1", "value1");
        store.close(); // Close and reopen to simulate persistence
        store = new Database();
        assertEquals("value1", store.get("key1"), "Value should persist after store is reopened.");
    }

    @Test
    void testCollisionHandling() throws IOException {
        // Assuming hash function might cause these two keys to collide
        store.put("key1", "value1");
        store.put("key2", "value2");
        assertAll(
                () -> assertEquals("value1", store.get("key1")),
                () -> assertEquals("value2", store.get("key2"))
        );
    }

    @Test
    void testResizing() throws IOException {
        // We'll insert more items than would trigger a resize based on initial capacity and load factor
        int numEntries = (int) (16 * .75) + 1;
        for (int i = 0; i < numEntries; i++) {
            store.put("key" + i, "value" + i);
        }
        for (int i = 0; i < numEntries; i++) {
            assertEquals("value" + i, store.get("key" + i), "All values should be retrievable post-resize.");
        }
    }

    @Test
    void testLargeAmountOfData() throws IOException {
        // Test the store with a large amount of data to check stability and performance
        int largeNumEntries = 1000; // Consider increasing this number based on system capability
        for (int i = 0; i < largeNumEntries; i++) {
            store.put("key" + i, "value" + i);
        }
        for (int i = 0; i < largeNumEntries; i++) {
            assertEquals("value" + i, store.get("key" + i), "All values should be retrievable.");
        }
    }
}
