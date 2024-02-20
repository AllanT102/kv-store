package main;

public class BTree {
    public static final int MIN_DEGREE = 4;

    private BTreeNode root;

    public BTree() {
        this.root = null;
    }

    public void traverse() {
        if (this.root != null) {
            this.root.traverse();
        }
        System.out.println();
    }

    public BTreeNode search(int k) {
        if (this.root == null) return null;
        else return this.root.search(k);
    }

    public void insert(int k) {
        if (root == null) {
            root = new BTreeNode(0, true);
            root.keys[0] = k;
            root.n += 1;
            return;
        }

        if (root.n == 2 * MIN_DEGREE - 1) {
            BTreeNode newNode = new BTreeNode(0, false);
            newNode.children[0] = root;
            newNode.splitChild(0, root);

            int i = 0;
            if (newNode.keys[0] < k) i++;
            newNode.children[i].insertNonFull(k);
            root = newNode;
        } else {
            root.insertNonFull(k);
        }
    }
}
