# kv-store


# uses separate chaining algorithm
# buckets -> use separate file to store bucket information
# we need to keep track of the first pointer in the list, the last one to write new values, and the bucket index, if it's empty, etc.
# use bits to keep track of bucket information

# first 4 bytes is used to store the amount of buckets

