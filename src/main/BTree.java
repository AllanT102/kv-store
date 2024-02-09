package main;

/**
 * BTree is a generic B-tree implementation that supports key-value pairs.
 *
 * @param <Key>   the key type (must be Comparable)
 * @param <Value> the value type
 */
public class BTree<Key extends Comparable<Key>, Value> {

    private static final int MIN_CHILDREN = 4;

    /**
     * Inner class representing a B-tree node.
     */
    private static final class BTreeNode {
        private int numberOfChildren;               // number of children
        private BTreeEntry[] children = new BTreeEntry[MIN_CHILDREN];   // the array of children

        /**
         * Creates a node with the specified number of children.
         *
         * @param count the number of children
         */
        private BTreeNode(int count) {
            numberOfChildren = count;
        }
    }

    /**
     * Inner class representing an entry in the B-tree node.
     */
    private static class BTreeEntry {
        private Comparable key;
        private Object value;
        private BTreeNode next;     // helper field to iterate over array entries

        /**
         * Creates a new entry with the specified key, value, and next node.
         *
         * @param key   the key of the entry
         * @param value the value of the entry
         * @param next  the next node in the B-tree
         */
        public BTreeEntry(Comparable key, Object value, BTreeNode next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private BTreeNode root;       // root of the B-tree
    private int treeHeight;      // height of the B-tree
    private int numberOfEntries;           // number of key-value pairs in the B-tree

    /**
     * Creates an empty B-tree.
     */
    public BTree() {
        root = new BTreeNode(0);
    }

    /**
     * Checks if the B-tree is empty.
     *
     * @return true if the B-tree is empty, false otherwise
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns the number of key-value pairs in the B-tree.
     *
     * @return the number of key-value pairs
     */
    public int size() {
        return numberOfEntries;
    }

    /**
     * Returns the height of the B-tree.
     *
     * @return the height of the B-tree
     */
    public int height() {
        return treeHeight;
    }

    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key the key to search for
     * @return the value associated with the key, or null if not found
     * @throws IllegalArgumentException if the key is null
     */
    public Value get(Key key) {
        if (key == null) throw new IllegalArgumentException("argument to get() is null");
        return search(root, key, treeHeight);
    }

    /**
     * Recursive helper method to search for the key in the B-tree.
     *
     * @param currentNode the current node
     * @param key         the key to search for
     * @param height      the height of the current node
     * @return the value associated with the key, or null if not found
     */
    private Value search(BTreeNode currentNode, Key key, int height) {
        BTreeEntry[] children = currentNode.children;

        // external node
        if (height == 0) {
            for (int j = 0; j < currentNode.numberOfChildren; j++) {
                if (isEqual(key, children[j].key)) return (Value) children[j].value;
            }
        }

        // internal node
        else {
            for (int j = 0; j < currentNode.numberOfChildren; j++) {
                if (j + 1 == currentNode.numberOfChildren || isLess(key, children[j + 1].key))
                    return search(children[j].next, key, height - 1);
            }
        }
        return null;
    }

    /**
     * Inserts the specified key-value pair into the B-tree.
     *
     * @param key   the key to insert
     * @param value the value to insert
     * @throws IllegalArgumentException if the key is null
     */
    public void put(Key key, Value value) {
        if (key == null) throw new IllegalArgumentException("argument key to put() is null");
        BTreeNode newNode = insert(root, key, value, treeHeight);
        numberOfEntries++;
        if (newNode == null) return;

        // need to split root
        BTreeNode newRoot = new BTreeNode(2);
        newRoot.children[0] = new BTreeEntry(root.children[0].key, null, root);
        newRoot.children[1] = new BTreeEntry(newNode.children[0].key, null, newNode);
        root = newRoot;
        treeHeight++;
    }

    /**
     * Recursive helper method to insert the key-value pair into the B-tree.
     *
     * @param currentNode the current node
     * @param key         the key to insert
     * @param value       the value to insert
     * @param height      the height of the current node
     * @return the new node after insertion, or null if no split occurred
     */
    private BTreeNode insert(BTreeNode currentNode, Key key, Value value, int height) {
        int j;
        BTreeEntry newEntry = new BTreeEntry(key, value, null);

        // external node
        if (height == 0) {
            for (j = 0; j < currentNode.numberOfChildren; j++) {
                if (isLess(key, currentNode.children[j].key)) break;
            }
        }

        // internal node
        else {
            for (j = 0; j < currentNode.numberOfChildren; j++) {
                if ((j + 1 == currentNode.numberOfChildren) || isLess(key, currentNode.children[j + 1].key)) {
                    BTreeNode newNode = insert(currentNode.children[j++].next, key, value, height - 1);
                    if (newNode == null) return null;
                    newEntry.key = newNode.children[0].key;
                    newEntry.value = null;
                    newEntry.next = newNode;
                    break;
                }
            }
        }

        for (int i = currentNode.numberOfChildren; i > j; i--)
            currentNode.children[i] = currentNode.children[i - 1];
        currentNode.children[j] = newEntry;
        currentNode.numberOfChildren++;
        if (currentNode.numberOfChildren < MIN_CHILDREN) return null;
        else return split(currentNode);
    }

    /**
     * Splits the specified node in half.
     *
     * @param currentNode the node to split
     * @return the new node created after the split
     */
    private BTreeNode split(BTreeNode currentNode) {
        BTreeNode newNode = new BTreeNode(MIN_CHILDREN / 2);
        currentNode.numberOfChildren = MIN_CHILDREN / 2;
        for (int j = 0; j < MIN_CHILDREN / 2; j++)
            newNode.children[j] = currentNode.children[MIN_CHILDREN / 2 + j];
        return newNode;
    }

    /**
     * Returns a string representation of the B-tree.
     *
     * @return a string representation of the B-tree
     */
    public String toString() {
        return toString(root, treeHeight, "") + "\n";
    }

    /**
     * Recursive helper method to create a string representation of the B-tree.
     *
     * @param currentNode the current node
     * @param height      the height of the current node
     * @param indent      the indentation string
     * @return a string representation of the B-tree
     */
    private String toString(BTreeNode currentNode, int height, String indent) {
        StringBuilder result = new StringBuilder();
        BTreeEntry[] children = currentNode.children;

        if (height == 0) {
            for (int j = 0; j < currentNode.numberOfChildren; j++) {
                result.append(indent + children[j].key + " " + children[j].value + "\n");
            }
        } else {
            for (int j = 0; j < currentNode.numberOfChildren; j++) {
                if (j > 0) result.append(indent + "(" + children[j].key + ")\n");
                result.append(toString(children[j].next, height - 1, indent + "     "));
            }
        }
        return result.toString();
    }

    /**
     * Compares two comparable objects and checks if the first is less than the second.
     *
     * @param obj1 the first comparable object
     * @param obj2 the second comparable object
     * @return true if the first is less than the second, false otherwise
     */
    private boolean isLess(Comparable obj1, Comparable obj2) {
        return obj1.compareTo(obj2) < 0;
    }

    /**
     * Compares two comparable objects for equality.
     *
     * @param obj1 the first comparable object
     * @param obj2 the second comparable object
     * @return true if the objects are equal, false otherwise
     */
    private boolean isEqual(Comparable obj1, Comparable obj2) {
        return obj1.compareTo(obj2) == 0;
    }
}
