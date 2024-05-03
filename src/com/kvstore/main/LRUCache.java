package com.kvstore.main;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements a Least Recently Used (LRU) cache mechanism that evicts the least recently accessed
 * entries when the capacity is exceeded. The LRU cache is backed by a doubly linked list and a hash map,
 * ensuring O(1) time complexity for adding, removing, and accessing entries.
 */
class LRUCache {
    /**
     * Represents a node in the doubly linked list.
     */
    static class Node {
        String key;
        String value;
        Node prev, next;

        /**
         * Constructs a new node with specified key and value.
         *
         * @param key   the key of the node
         * @param value the value of the node
         */
        Node(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private static final int CAPACITY = 1000;

    private final Map<String, Node> cache;
    private final int capacity;
    private Node head, tail;

    /**
     * Constructs an LRUCache with the default capacity.
     */
    public LRUCache() {
        this.capacity = CAPACITY;
        this.cache = new HashMap<>();
        this.head = new Node(null, null);
        this.tail = new Node(null, null);
        head.next = tail;
        tail.prev = head;
    }

    /**
     * Adds a node right after the head of the doubly linked list.
     *
     * @param node the node to add
     */
    private void addNode(Node node) {
        Node next = head.next;
        node.next = next;
        node.prev = head;
        head.next = node;
        next.prev = node;
    }

    /**
     * Removes a node from the doubly linked list.
     *
     * @param node the node to remove
     */
    private void removeNode(Node node) {
        Node prev = node.prev;
        Node next = node.next;
        prev.next = next;
        next.prev = prev;
    }

    /**
     * Moves a node to the head of the list, indicating it has been recently accessed.
     *
     * @param node the node to move
     */
    private void moveToHead(Node node) {
        removeNode(node);
        addNode(node);
    }

    /**
     * Removes the node from the tail of the list, which is the least recently accessed item.
     *
     * @return the node that was removed
     */
    private Node popTail() {
        Node res = tail.prev;
        removeNode(res);
        return res;
    }

    /**
     * Retrieves the value associated with the specified key in the cache.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the specified key, or null if the key does not exist
     */
    public String get(String key) {
        Node node = cache.get(key);
        if (node == null) return null;
        moveToHead(node);
        return node.value;
    }

    /**
     * Updates the value of the key if it exists, or inserts the key if it does not already exist.
     * If the cache exceeds its capacity, the least recently accessed item is evicted.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     */
    public void put(String key, String value) {
        Node node = cache.get(key);
        if (node == null) {
            Node newNode = new Node(key, value);
            cache.put(key, newNode);
            addNode(newNode);
            if (cache.size() > capacity) {
                Node tail = popTail();
                cache.remove(tail.key);
            }
        } else {
            node.value = value;
            moveToHead(node);
        }
    }

    /**
     * Removes the entry for the specified key only if it is currently mapped.
     *
     * @param key the key whose mapping is to be removed from the cache
     */
    public void delete(String key) {
        Node node = cache.get(key);
        if (node != null) {
            removeNode(node);
            cache.remove(key);
        }
    }
}

