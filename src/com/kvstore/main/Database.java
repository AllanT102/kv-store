package com.kvstore.main;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Database {
    private RandomAccessFile store;
    private static final int INITIAL_BUCKETS = 16;
    private static final int RECORD_SIZE = 210; // 1 byte status + 8 bytes next + 100 bytes key + 100 bytes value
    private static final int KEY_SIZE = 100;
    private static final int VALUE_SIZE = 100;
    private static final double LOAD_FACTOR = 0.75;

    private int bucketCount;
    private int size; // Number of active records

    public Database() throws IOException {
        String fileName = "data.data";
        this.store = new RandomAccessFile(fileName, "rw");
        this.bucketCount = INITIAL_BUCKETS;
        this.size = 0;
        if (store.length() == 0) {
            initializeHashTable();
        } else {
            // Assuming the first int in the file is the number of buckets
            this.bucketCount = store.readInt();
        }
    }

    private void initializeHashTable() throws IOException {
        store.setLength(Integer.BYTES + bucketCount * Long.BYTES);
        store.seek(0);
        store.writeInt(bucketCount);

        // mark all buckets to be invalid status
        for (int i = 0; i < bucketCount; i++) {
            store.writeLong(0);
        }
    }

    public String get(String key) throws IOException {
        long bucketIndex = hashKey(key) % bucketCount;
        long bucketOffset = Integer.BYTES + bucketIndex * Long.BYTES; // Offset where the bucket pointer is stored
        store.seek(bucketOffset);
        long entryPos = store.readLong(); // Read the head of the chain for this bucket

        while (entryPos != 0) {
            store.seek(entryPos);
            byte status = store.readByte(); // Read the status of the record
            long nextEntryPos = store.readLong(); // Read the pointer to the next record
            byte[] keyBytes = new byte[KEY_SIZE];
            store.readFully(keyBytes); // Read the key
            String currentKey = new String(keyBytes).trim();

            if (status == 1 && currentKey.equals(key)) {
                byte[] valueBytes = new byte[VALUE_SIZE];
                store.readFully(valueBytes); // Read the value
                return new String(valueBytes).trim(); // Return the found value
            }

            entryPos = nextEntryPos; // Move to the next entry in the chain
        }

        return null; // Key not found
    }

    public void put(String key, String value) throws IOException {
        if ((size + 1) > (int) (bucketCount * LOAD_FACTOR)) {
            resize();
            this.store = new RandomAccessFile("data.data", "rw");
            this.store.seek(0);
        }

        long bucketIndex = hashKey(key) % bucketCount;
        long bucketOffset = Integer.BYTES + bucketIndex * Long.BYTES;
        store.seek(bucketOffset);
        long headPos = store.readLong();
        long currentPos = headPos;
        long prevPos = -1;

        while (currentPos != 0) {
            store.seek(currentPos);
            byte status = store.readByte();
            long nextPos = store.readLong();
            byte[] keyBytes = new byte[KEY_SIZE];
            store.readFully(keyBytes);
            if (status == 1 && new String(keyBytes).trim().equals(key)) {
                // Key found, update value
                store.write(fixLength(value, VALUE_SIZE).getBytes());
                return;
            }
            prevPos = currentPos;
            currentPos = nextPos;
        }

        // No entry found, add new entry
        long newEntryPos = store.length();
        store.seek(newEntryPos);
        store.writeByte(1); // Active record
        store.writeLong(0); // Next pointer
        store.write(fixLength(key, KEY_SIZE).getBytes());
        store.write(fixLength(value, VALUE_SIZE).getBytes());

        if (prevPos == -1) {
            // Updating head of the bucket
            store.seek(bucketOffset);
            store.writeLong(newEntryPos);
        } else {
            // Updating the previous record's next pointer
            store.seek(prevPos + 1); // +1 to skip the status byte
            store.writeLong(newEntryPos);
        }

        size++;
    }

    public void delete(String key) throws IOException {
        long bucketIndex = hashKey(key) % bucketCount;
        long bucketOffset = Integer.BYTES + bucketIndex * Long.BYTES;
        store.seek(bucketOffset);
        long headPos = store.readLong();
        long currentPos = headPos;
        long prevPos = 0;  // Keep track of the previous node's position to update links if needed

        while (currentPos != 0) {
            store.seek(currentPos);
            byte status = store.readByte();
            long nextPos = store.readLong();
            byte[] keyBytes = new byte[KEY_SIZE];
            store.readFully(keyBytes);
            String currentKey = new String(keyBytes).trim();

            if (status == 1 && currentKey.equals(key)) {
                // Mark the record as deleted by setting its status byte to 0
                store.seek(currentPos);  // Go back to the start of the record
                store.writeByte(0);  // Status byte to 0 to mark as deleted

                // If you need to remove the record from the chain
                if (prevPos != 0) {
                    // Update the previous record's next pointer
                    store.seek(prevPos + 1);  // Move to the position of the next pointer in the previous record
                    store.writeLong(nextPos);  // Set the previous record's next pointer to skip the deleted record
                } else {
                    // Update the head pointer in the bucket if the head record is deleted
                    store.seek(bucketOffset);
                    store.writeLong(nextPos);
                }

                return;  // Exit after deleting the key
            }

            // Move to the next record in the chain
            prevPos = currentPos;
            currentPos = nextPos;
        }

        // If the key was not found, you might want to throw an exception or return a status
         throw new IOException("Key not found: " + key);
    }

    private void compact() throws IOException {

    }

    private void resize() throws IOException {
        String tempFileName = "tempStore_" + System.currentTimeMillis() + ".tmp";
        RandomAccessFile tempStore = new RandomAccessFile(tempFileName, "rw");


        // try to refactor here to use initializeBuckets()
        int newBucketCount = this.bucketCount * 2;
        long[] newBuckets = new long[newBucketCount];
        for (int i = 0; i < newBucketCount; i++) {
            newBuckets[i] = 0;  // Initialize bucket pointers to 0
        }

        // initialize new file
        tempStore.setLength(Integer.BYTES + newBucketCount * Long.BYTES);
        tempStore.seek(0);
        tempStore.writeInt(newBucketCount);
        // mark all buckets to be invalid status
        for (int i = 0; i < newBucketCount; i++) {
            tempStore.writeLong(0);
        }

        for (int i = 0; i < bucketCount; i++) {
            long bucketOffset = Integer.BYTES + i * Long.BYTES;
            store.seek(bucketOffset);
            long entryPos = store.readLong();
            while (entryPos != 0) {
                store.seek(entryPos);
                byte status = store.readByte();
                long nextPos = store.readLong();
                byte[] keyBytes = new byte[KEY_SIZE];
                store.readFully(keyBytes);
                byte[] valueBytes = new byte[VALUE_SIZE];
                store.readFully(valueBytes);
                if (status == 1) {
                    // rehash this value into the new file
                    long newBucketIndex = hashKey(new String(keyBytes).trim()) % newBucketCount;
                    long newBucketOffset = Integer.BYTES + newBucketIndex * Long.BYTES;
                    tempStore.seek(newBucketOffset);
                    long newEntryPos = tempStore.readLong();

                    // Write new entry at the end of tempStore
                    long writePos = tempStore.length();
                    tempStore.seek(writePos);
                    tempStore.writeByte(1);
                    tempStore.writeLong(newBuckets[(int) newBucketIndex]);  // Update chain head
                    tempStore.write(keyBytes);
                    tempStore.write(valueBytes);
                    newBuckets[(int) newBucketIndex] = writePos;
                    tempStore.seek(newBucketOffset);
                    tempStore.writeLong(writePos);
                }
                entryPos = nextPos;
            }
        }
        this.store.close();
        Files.move(Paths.get(tempFileName), Paths.get("data.data"), StandardCopyOption.REPLACE_EXISTING);
        this.bucketCount = newBucketCount;
    }

    private long hashKey(String key) {
        return key.hashCode() & 0x7fffffff; // Non-negative hash code
    }

    private String fixLength(String string, int length) {
        return String.format("%-" + length + "s", string);
    }

    public void close() throws IOException {
        if (store != null) {
            store.close();
        }
    }
}
