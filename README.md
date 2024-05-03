# Key-Value Store

## Overview
This KeyValue Store is a simple yet robust database system designed for efficient storage and retrieval of key-value pairs. It integrates an LRU (Least Recently Used) cache mechanism to enhance read performance by caching frequently accessed data.

## Features

## Collision Handling & Hashing
- Groups entries into buckets based on their hash value. Each bucket can dynamically expand in response to collisions
- Utilizes separate chaining algorithm to handle collisions effectively

### CRUD Operations
- **Create (Put)**: Add new key-value pairs to the database. If the key already exists, the value is updated.
- **Read (Get)**: Retrieve values based on their keys.
- **Update**: Performed via the `put` method by providing an existing key with a new value.
- **Delete**: Remove key-value pairs from the database.

### Caching
- **LRU Cache**: Implements an LRU cache to optimize data retrieval operations. The cache automatically manages the eviction of the least recently used items when it reaches its capacity limit.

### Data Persistence
- Uses file-based storage to maintain data persistence across sessions, ensuring that data is not lost between application restarts.

### Scalability
- Handles dynamic resizing based on load factors to maintain optimal performance and efficiency even as the dataset grows.
