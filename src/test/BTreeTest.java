package test;

import static org.junit.Assert.*;

import main.BTree;
import org.junit.Before;
import org.junit.Test;

public class BTreeTest {

    private BTree<Integer, String> bTree;

    @Before
    public void setUp() {
        bTree = new BTree<>();
    }

    @Test
    public void testputAndget() {
        // put key-value pairs
        bTree.put(3, "Three");
        bTree.put(7, "Seven");
        bTree.put(1, "One");
        bTree.put(5, "Five");
        bTree.put(4, "Four");
        bTree.put(8, "Eight");
        bTree.put(2, "Two");
        bTree.put(6, "Six");

        // get for keys
        assertEquals("Three", bTree.get(3));
        assertEquals("Five", bTree.get(5));
        assertEquals("Eight", bTree.get(8));
        assertNull(bTree.get(9));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testputNullKey() {
        bTree.put(null, "NullValue");
    }

    @Test
    public void testgetEmptyTree() {
        assertNull(bTree.get(42));
    }

    @Test
    public void testgetNonexistentKey() {
        bTree.put(1, "One");
        assertNull(bTree.get(42));
    }

    @Test
    public void testputAndgetLargeDataset() {
        // put a large number of key-value pairs
        for (int i = 1; i <= 1000; i++) {
            bTree.put(i, "Value" + i);
        }

        // get for keys in the large dataset
        for (int i = 1; i <= 1000; i++) {
            assertEquals("Value" + i, bTree.get(i));
        }

        // get for a non-existent key
        assertNull(bTree.get(2000));
    }
}
