package com.kvstore.main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;

/**
 * Manages file operations for a database system, providing a simplified API for reading and writing
 * to a file. This class abstracts away some of the complexities of the RandomAccessFile class and
 * adds utility methods for common operations.
 */
public class FileManager {
    public RandomAccessFile file;

    public FileManager(String filename, String mode) throws FileNotFoundException {
        file = new RandomAccessFile(filename, mode);
    }

    public long getLength() throws IOException {
        return file.length();
    }

    public void setLength(long length) throws IOException {
        file.setLength(length);
    }

    public byte readByte() throws IOException {
        return file.readByte();
    }

    public int readInt() throws IOException {
        return file.readInt();
    }

    public void writeInt(int value) throws IOException {
        file.writeInt(value);
    }

    public long readLong() throws IOException {
        return file.readLong();
    }

    public void writeLong(long value) throws IOException {
        file.writeLong(value);
    }

    public void readFully(byte[] bytes) throws IOException {
        file.readFully(bytes);
    }

    public void readKey(byte[] bytes) throws IOException {
        readFully(bytes);
    }

    public void readValue(byte[] bytes) throws IOException {
        readFully(bytes);
    }

    public void write(byte[] bytes) throws IOException {
        file.write(bytes);
    }

    public void writeByte(int b) throws IOException {
        file.writeByte(b);
    }

    public void seek(long pos) throws IOException {
        file.seek(pos);
    }

    public void close() throws IOException {
        file.close();
    }

    public void moveFile(
            java.nio.file.Path src, java.nio.file.Path target, java.nio.file.CopyOption... options) throws IOException {
        Files.move(src, target, options);
    }

}
