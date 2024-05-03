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
        String jsonValue = "{\"name\": \"value1\"}";
        store.put("key1", jsonValue);
        assertEquals(jsonValue, store.get("key1"), "Retrieved value should match the stored JSON value.");
    }

    @Test
    void testUpdateValue() throws IOException {
        String jsonValue1 = "{\"name\": \"value1\"}";
        String jsonValue2 = "{\"name\": \"value2\"}";
        store.put("key1", jsonValue1);
        store.put("key1", jsonValue2);
        assertEquals(jsonValue2, store.get("key1"), "Retrieved value should be updated JSON value.");
    }

    @Test
    void testDeleteValue() throws IOException {
        String jsonValue1 = "{\"name\": \"value1\"}";
        String jsonValue2 = "{\"name\": \"value2\"}";
        store.put("key1", jsonValue1);
        store.put("key2", jsonValue2);
        store.delete("key1");
        assertNull(store.get("key1"), "Deleted JSON value should not exist.");
    }

    @Test
    void testPersistence() throws IOException {
        String jsonValue = "{\"name\": \"value1\"}";
        store.put("key1", jsonValue);
        store.close(); // Close and reopen to simulate persistence
        store = new Database();
        assertEquals(jsonValue, store.get("key1"), "JSON value should persist after store is reopened.");
    }

    @Test
    void testCollisionHandling() throws IOException {
        String jsonValue1 = "{\"name\": \"value1\"}";
        String jsonValue2 = "{\"name\": \"value2\"}";
        store.put("key1", jsonValue1);
        store.put("key2", jsonValue2);
        assertAll(
                () -> assertEquals(jsonValue1, store.get("key1"), "First key should retrieve its correct JSON value."),
                () -> assertEquals(jsonValue2, store.get("key2"), "Second key should retrieve its correct JSON value.")
        );
    }

    @Test
    void testResizingGrowth() throws IOException {
        int numEntries = (int) (16 * .75) + 1;
        for (int i = 0; i < numEntries; i++) {
            store.put("key" + i, "{\"value\": \"value" + i + "\"}");
        }
        for (int i = 0; i < numEntries; i++) {
            assertEquals("{\"value\": \"value" + i + "\"}", store.get("key" + i), "All JSON values should be retrievable post-resize.");
        }
    }

    @Test
    void testResizingShrink() throws IOException {
        int numEntries = (int) (16 * .75) + 1;
        for (int i = 0; i < numEntries; i++) {
            store.put("key" + i, "{\"value\": \"value" + i + "\"}");
        }
        int lowerThreshold = (int) (numEntries * .875);
        for (int i = 0; i < lowerThreshold; i++) {
            store.delete("key" + i);
        }
        for (int i = lowerThreshold; i < numEntries; i++) {
            assertEquals("{\"value\": \"value" + i + "\"}", store.get("key" + i), "All JSON values should be retrievable post-resize.");
        }
    }

    @Test
    void testLargeAmountOfData() throws IOException {
        int largeNumEntries = 1000;
        for (int i = 0; i < largeNumEntries; i++) {
            store.put("key" + i, "{\"value\": \"value" + i + "\"}");
        }
        for (int i = 0; i < largeNumEntries; i++) {
            assertEquals("{\"value\": \"value" + i + "\"}", store.get("key" + i), "All JSON values should be retrievable.");
        }
    }
}