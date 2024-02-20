package main;

import static main.BTree.MIN_DEGREE;

public class BTreeNode<K, V> {
    public int nCurrentEntry;
    public int n; // current number of keys
    public int minDegree; // depends on disk block size
    public boolean leaf;
    public int[] keys;
    public BTreeNode[] children;

    public BTreeNode(K key, boolean isLeaf) {
        this.minDegree = MIN_DEGREE;
        this.keys = new int[2 * minDegree - 1];
        this.children = new BTreeNode[2 * minDegree];
        this.leaf = isLeaf;
        this.n = 0;
    }

    public void traverse() {
        int i = 0;
        for (i = 0; i < this.n; i++) {
            if (!this.leaf) {
                children[i].traverse();
            }
            System.out.println(keys[i] + " ");
        }
        if (!leaf) children[i].traverse();
    }

    public BTreeNode search(int k) {
        int i = 0;
        while (i < n && k > keys[i]) i++;
        if (i < n && keys[i] == k) return this;
        if (leaf) return null;
        return children[i].search(k);
    }

    public void insertNonFull(int k) {
        int i = n - 1;
        if (leaf == true) {
            while (i >= 0 && keys[i] > k) {
                keys[i+1] = keys[i];
                i--;
            }
            keys[i+1] = k;
            n = n + 1;
        } else {
            while (i >= 0 && keys[i] > k) i--;

            if (children[i+1].n == 2 * MIN_DEGREE - 1) {
                splitChild(i + 1, children[i+1]);
                if (keys[i+1] < k) {
                    i++;
                }
            }
            children[i+1].insertNonFull(k);
        }
    }

    public void splitChild(int i, BTreeNode node) {
        BTreeNode newNode = new BTreeNode(0, node.leaf);
        newNode.n = MIN_DEGREE - 1;

        for (int j = 0; j < MIN_DEGREE - 1; j++) newNode.keys[j] = node.keys[j + MIN_DEGREE];
        if (!node.leaf) {
            for (int j = 0; j < MIN_DEGREE; j++) {
                newNode.children[j] = node.children[j + MIN_DEGREE];
            }
        }
        node.n = MIN_DEGREE - 1;

        for (int j = n; j >= i + 1; j--) children[j + 1] = children[j];

        children[i + 1] = newNode;

        for (int j = n - 1; j >= i; j--) keys[j + 1] = keys[j];

        keys[i] = node.keys[MIN_DEGREE - 1];
        n = n + 1;
    }


}
