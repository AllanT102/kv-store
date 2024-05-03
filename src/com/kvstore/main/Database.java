package com.kvstore.main;

import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static com.kvstore.main.BucketManager.INITIAL_BUCKETS;

/**
 * Represents a key-value store database which uses a file for data persistence.
 * This class provides basic CRUD operations along with dynamic resizing based on
 * load factors for efficient data handling.
 */
public class Database {
    private static final int KEY_SIZE = 100;
    private static final int VALUE_SIZE = 100;
    private static final double HIGHER_LOAD_FACTOR = 0.75;
    private static final double LOWER_LOAD_FACTOR = 0.125;
    private static final int SHRINK = 0;
    private static final int GROW = 1;
    private static final String DEFAULT_DATA_FILE_NAME = "data.data";

    private FileManager fileManager;
    private final BucketManager bucketManager;
    private final LRUCache cache;

    private int size;  // Number of active records

    /**
     * Initializes a new Database instance. If the data file is empty, it initializes a new hash table.
     * Otherwise, it reads the existing bucket count from the file.
     *
     * @throws IOException If there is an error opening the file or reading from it.
     */
    public Database() throws IOException {
        this.fileManager = new FileManager(DEFAULT_DATA_FILE_NAME, "rw");
        this.bucketManager = new BucketManager();
        this.cache = new LRUCache();
        this.size = 0;
        if (fileManager.getLength() == 0) {
            initializeHashTable();
        } else {
            // Assuming the first int in the file is the number of buckets
            bucketManager.setBucketCount(fileManager.readInt());
        }
    }

    /**
     * Initializes the hash table by setting all bucket pointers to zero and writing the bucket count to the file.
     *
     * @throws IOException If there is an error writing to the file.
     */
    private void initializeHashTable() throws IOException {
        fileManager.setLength(Integer.BYTES + bucketManager.getBucketCount() * Long.BYTES);
        fileManager.seek(0);
        fileManager.writeInt(bucketManager.getBucketCount());

        // mark all buckets to be invalid status
        for (int i = 0; i < bucketManager.getBucketCount(); i++) {
            fileManager.writeLong(0);
        }
    }

    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key The key whose associated value is to be returned.
     * @return The value associated with the specified key, or null if no value is found.
     * @throws IOException If an I/O error occurs during file access.
     */
    public String get(String key) throws IOException {
        String value;
        if ((value = cache.get(key)) != null) return value;
        long bucketOffset = bucketManager.getBucketOffset(key);
        fileManager.seek(bucketOffset);
        long entryPos = fileManager.readLong(); // Read the head of the chain for this bucket

        while (entryPos != 0) {
            fileManager.seek(entryPos);
            byte status = fileManager.readByte(); // Read the status of the record
            long nextEntryPos = fileManager.readLong(); // Read the pointer to the next record
            byte[] keyBytes = new byte[KEY_SIZE];
            fileManager.readKey(keyBytes); // Read the key
            String currentKey = new String(keyBytes).trim();

            if (status == 1 && currentKey.equals(key)) {
                byte[] valueBytes = new byte[VALUE_SIZE];
                fileManager.readValue(valueBytes); // Read the value
                cache.put(key, new String(valueBytes).trim());
                return new String(valueBytes).trim(); // Return the found value
            }

            entryPos = nextEntryPos; // Move to the next entry in the chain
        }

        return null; // Key not found
    }

    /**
     * Inserts a new key-value pair or updates an existing pair in the database.
     *
     * @param key The key of the element to save.
     * @param value The value to be associated with the key.
     * @throws IOException If an I/O error occurs during file access.
     */
    public void put(String key, String value) throws IOException {
        cache.put(key, value);
        if ((size + 1) > (int) (bucketManager.getBucketCount() * HIGHER_LOAD_FACTOR)) {
            resize(GROW);
            fileManager = new FileManager(DEFAULT_DATA_FILE_NAME, "rw");
            fileManager.seek(0);
        }

        long bucketOffset = bucketManager.getBucketOffset(key);
        fileManager.seek(bucketOffset);
        long headPos = fileManager.readLong();
        long currentPos = headPos;
        long prevPos = -1;

        while (currentPos != 0) {
            fileManager.seek(currentPos);
            byte status = fileManager.readByte();
            long nextPos = fileManager.readLong();
            byte[] keyBytes = new byte[KEY_SIZE];
            fileManager.readKey(keyBytes);
            if (status == 1 && new String(keyBytes).trim().equals(key)) {
                // Key found, update value
                fileManager.write(fixLength(value, VALUE_SIZE).getBytes());
                return;
            }
            prevPos = currentPos;
            currentPos = nextPos;
        }

        // No entry found, add new entry
        long newEntryPos = fileManager.getLength();
        fileManager.seek(newEntryPos);
        fileManager.writeByte(1); // Active record
        fileManager.writeLong(0); // Next pointer
        fileManager.write(fixLength(key, KEY_SIZE).getBytes());
        fileManager.write(fixLength(value, VALUE_SIZE).getBytes());

        if (prevPos == -1) {
            // Updating head of the bucket
            fileManager.seek(bucketOffset);
            fileManager.writeLong(newEntryPos);
        } else {
            // Updating the previous record's next pointer
            fileManager.seek(prevPos + 1); // +1 to skip the status byte
            fileManager.writeLong(newEntryPos);
        }

        size++;
    }


    /**
     * Deletes the entry associated with the specified key, if it exists.
     *
     * @param key The key whose entry is to be deleted.
     * @throws IOException If an I/O error occurs during file access or the key does not exist.
     */
    public void delete(String key) throws IOException {
        cache.delete(key);
        if ((size - 1) < (int) (bucketManager.getBucketCount() * LOWER_LOAD_FACTOR)) {
            resize(SHRINK);
            fileManager = new FileManager(DEFAULT_DATA_FILE_NAME, "rw");
            fileManager.seek(0);
        }

        long bucketOffset = bucketManager.getBucketOffset(key);
        fileManager.seek(bucketOffset);
        long headPos = fileManager.readLong();
        long currentPos = headPos;
        long prevPos = 0;  // Keep track of the previous node's position to update links if needed

        while (currentPos != 0) {
            fileManager.seek(currentPos);
            byte status = fileManager.readByte();
            long nextPos = fileManager.readLong();
            byte[] keyBytes = new byte[KEY_SIZE];
            fileManager.readKey(keyBytes);
            String currentKey = new String(keyBytes).trim();

            if (status == 1 && currentKey.equals(key)) {
                // Mark the record as deleted by setting its status byte to 0
                fileManager.seek(currentPos);  // Go back to the start of the record
                fileManager.writeByte(0);  // Status byte to 0 to mark as deleted

                // If you need to remove the record from the chain
                if (prevPos != 0) {
                    // Update the previous record's next pointer
                    fileManager.seek(prevPos + 1);  // Move to the position of the next pointer in the previous record
                    fileManager.writeLong(nextPos);  // Set the previous record's next pointer to skip the deleted record
                } else {
                    // Update the head pointer in the bucket if the head record is deleted
                    fileManager.seek(bucketOffset);
                    fileManager.writeLong(nextPos);
                }
                size--;
                return;  // Exit after deleting the key
            }

            // Move to the next record in the chain
            prevPos = currentPos;
            currentPos = nextPos;
        }

        // If the key was not found, you might want to throw an exception or return a status
         throw new IOException("Key not found: " + key);
    }


    /**
     * Resizes the hash table based on the specified mode. The size can be doubled or halved.
     *
     * @param mode The operation mode, either SHRINK (0) or GROW (1), where SHRINK halves the bucket count,
     *             and GROW doubles it.
     * @throws IOException If an I/O error occurs during resizing operations.
     */
    private void resize(int mode) throws IOException {
        String tempFileName = "tempStore_" + System.currentTimeMillis() + ".tmp";
        FileManager tempFileManager = new FileManager(tempFileName, "rw");
        BucketManager tempBucketManager = new BucketManager();

        // try to refactor here to use initializeBuckets();
        int newBucketCount = mode == GROW ? bucketManager.getBucketCount() * 2
                : Math.max(INITIAL_BUCKETS, bucketManager.getBucketCount() / 2);
        tempBucketManager.setBucketCount(newBucketCount);
        long[] newBuckets = new long[newBucketCount];
        for (int i = 0; i < newBucketCount; i++) {
            newBuckets[i] = 0;  // Initialize bucket pointers to 0
        }

        // initialize new file
        tempFileManager.setLength(Integer.BYTES + newBucketCount * Long.BYTES);
        tempFileManager.seek(0);
        tempFileManager.writeInt(newBucketCount);
        // mark all buckets to be invalid status
        for (int i = 0; i < newBucketCount; i++) {
            tempFileManager.writeLong(0);
        }

        for (int i = 0; i < bucketManager.getBucketCount(); i++) {
            long bucketOffset = Integer.BYTES + i * Long.BYTES;
            fileManager.seek(bucketOffset);
            long entryPos = fileManager.readLong();
            while (entryPos != 0) {
                fileManager.seek(entryPos);
                byte status = fileManager.readByte();
                long nextPos = fileManager.readLong();
                byte[] keyBytes = new byte[KEY_SIZE];
                fileManager.readKey(keyBytes);
                byte[] valueBytes = new byte[VALUE_SIZE];
                fileManager.readValue(valueBytes);
                if (status == 1) {
                    // rehash this value into the new file
                    long newBucketIndex = tempBucketManager.getBucketIndex(new String(keyBytes).trim());
                    long newBucketOffset = tempBucketManager.getBucketOffset(new String(keyBytes).trim());
                    tempFileManager.seek(newBucketOffset);
                    long newEntryPos = tempFileManager.readLong();

                    // Write new entry at the end of tempStore
                    long writePos = tempFileManager.getLength();
                    tempFileManager.seek(writePos);
                    tempFileManager.writeByte(1);
                    tempFileManager.writeLong(newBuckets[(int) newBucketIndex]);  // Update chain head
                    tempFileManager.write(keyBytes);
                    tempFileManager.write(valueBytes);
                    newBuckets[(int) newBucketIndex] = writePos;
                    tempFileManager.seek(newBucketOffset);
                    tempFileManager.writeLong(writePos);
                }
                entryPos = nextPos;
            }
        }
        fileManager.close();
        fileManager.moveFile(Paths.get(tempFileName), Paths.get("data.data"), StandardCopyOption.REPLACE_EXISTING);
        bucketManager.setBucketCount(newBucketCount);
    }

    /**
     * Adjusts the length of the given string to a specified length by padding it with spaces
     * on the right if it is shorter than the desired length. If the string is longer than the
     * specified length, it will be truncated to fit.
     *
     * @param string The string to be adjusted.
     * @param length The desired length of the string. Must be non-negative.
     * @return A string adjusted to the specified length.
     */
    private String fixLength(String string, int length) {
        return String.format("%-" + length + "s", string);
    }

    /**
     * Closes the file manager and releases any system resources associated with the file.
     *
     * @throws IOException If an I/O error occurs.
     */
    public void close() throws IOException {
        if (fileManager != null) {
            fileManager.close();
        }
    }
}
