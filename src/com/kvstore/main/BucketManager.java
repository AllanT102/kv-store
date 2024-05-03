package com.kvstore.main;

/**
 * Manages the bucket indices for a key-value store, facilitating the distribution and retrieval
 * of values based on keys. This class provides methods to get and set the number of buckets,
 * calculate bucket indices, and compute offsets within a file or data structure based on the key's hash.
 */
public class BucketManager {
    public static final int INITIAL_BUCKETS = 16;
    private int bucketCount;

    public BucketManager() {
        this.bucketCount = INITIAL_BUCKETS;
    }

    public BucketManager(int bucketCount) {
        this.bucketCount = bucketCount;
    }

    public void setBucketCount(int bucketCount) {
        this.bucketCount = bucketCount;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public long getBucketOffset(String key) {
        return Integer.BYTES + getBucketIndex(key) * Long.BYTES;
    }

    public long getBucketIndex(String key) {
        return hashKey(key) % this.bucketCount;
    }

    private long hashKey(String key) {
        return key.hashCode() & 0x7fffffff; // Non-negative hash code
    }
}
