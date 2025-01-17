
public class HashMap<K, V> {
    private static final int INITIAL_CAPACITY = 19; // Initial capacity of the HashMap.
    private static final float LOAD_FACTOR = 0.75f; // Load factor threshold for resizing.

    private Entry<K, V>[] table; // Array of Entry objects.
    private int size; // Number of key-value pairs in the map.

    // Entry class representing a key-value pair.
    private static class Entry<K, V> {
        final K key;
        V value;
        Entry<K, V> next; // Reference to the next Entry in case of a collision.

        Entry(K key, V value, Entry<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    public HashMap() { // Constructor for Hashmap class.
        table = new Entry[INITIAL_CAPACITY];
    }

    private int hash(K key) { // Hash function to compute index for a key.
        return (key == null) ? 0 : Math.abs(key.hashCode() % table.length);
    }

    public void put(K key, V value) { // Method to add or update a key-value pair.
        int index = hash(key);
        Entry<K, V> existing = table[index];

        // Check if the key already exists and update its value.
        for (Entry<K, V> e = existing; e != null; e = e.next) {
            if (keyEquals(e.key, key)) {
                e.value = value;
                return;
            }
        }

        // Insert the new key-value pair.
        Entry<K, V> newEntry = new Entry<>(key, value, existing);
        table[index] = newEntry;
        size++;

        // Resize the table if the load factor is exceeded.
        if (size >= LOAD_FACTOR * table.length) {
            resize();
        }
    }

    public V get(K key) { // Method to retrieve a value by its key.
        int index = hash(key);
        Entry<K, V> e = table[index];

        for (; e != null; e = e.next) {
            if (keyEquals(e.key, key)) {
                return e.value;
            }
        }
        return null; // Key could not be found.
    }

    public void putIfAbsent(K key, V value) {
        int index = hash(key);
        Entry<K, V> e = table[index];

        // Check if the key already exists
        for (; e != null; e = e.next) {
            if (keyEquals(e.key, key)) {
                return; // Return existing value if key exists
            }
        }

        // Key is absent, so we insert it
        Entry<K, V> newEntry = new Entry<>(key, value, table[index]);
        table[index] = newEntry;
        size++;

        // Resize the table if the load factor is exceeded
        if (size >= LOAD_FACTOR * table.length) {
            resize();
        }

    }

    public V getOrDefault(K key) {
        int index = hash(key);
        Entry<K, V> e = table[index];

        for (; e != null; e = e.next) {
            if (keyEquals(e.key, key)) {
                return e.value;
            }
        }
        return null;// Return default value if key is not found
    }

    private void resize() { // Method to resize the table when the load factor is exceeded.
        Entry<K, V>[] oldTable = table;
        int newCapacity = oldTable.length * 2;
        table = new Entry[newCapacity];

        // Rehash all existing entries.
        for (Entry<K, V> e : oldTable) {
            while (e != null) {
                Entry<K, V> next = e.next;
                int index = hash(e.key);
                e.next = table[index];
                table[index] = e;
                e = next;
            }
        }
    }

    private boolean keyEquals(K key1, K key2) { // Method to compare the keys.
        if (key1 == null) {
            return key2 == null;
        }
        return key1.equals(key2);
    }
}